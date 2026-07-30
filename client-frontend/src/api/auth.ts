import { clearAccessSession, hasActiveAccessSession, setAccessSession } from '../auth/AuthMemory';

export type AuthSession = {
  authenticated: boolean;
  registrationComplete: boolean;
  nickname: string;
};

export type AddressResult = {
  zipCode: string;
  province: string;
  detailAddress: string;
};

export type RegistrationDetailPayload = {
  nameKanji: string;
  nameKatakana: string;
  birthDate: string;
  gender: string;
  nickname: string;
  zipCode: string;
  province: string;
  baseAddress: string;
  detailAddress: string;
  telephone: string;
  mobilePhone: string;
};

type CsrfResponse = { headerName: string; token: string };
type ApiErrorResponse = { messages?: string[] };
type AccessSessionResponse = { expiresAt: string };
type OAuthCompleteResponse = {
  expiresAt: string | null;
  destination: string;
};
type TermsSessionResponse = { accepted: boolean; alarmConsent: boolean };
type TermsAgreementPayload = {
  accepted: boolean;
  alarmConsent: boolean;
};

let refreshInFlight: Promise<boolean> | null = null;
let expiredSessionInFlight: Promise<void> | null = null;
let logoutInProgress = false;
let authGeneration = 0;
let csrfCache: CsrfResponse | null = null;
let csrfInFlight: Promise<CsrfResponse> | null = null;
let sessionInFlight: Promise<AuthSession> | null = null;
let detailSessionInFlight: Promise<void> | null = null;
let termsSessionInFlight: Promise<TermsSessionResponse> | null = null;
const postalCodeCache = new Map<string, { expiresAt: number; results: AddressResult[] }>();
const postalCodeRequests = new Map<string, Promise<AddressResult[]>>();
const POSTAL_CODE_CACHE_TTL_MS = 30 * 60 * 1_000;
const MAX_POSTAL_CODE_CACHE_SIZE = 32;
const BROWSER_SESSION_KEY = 'zik.auth.browser-session';

function hasBrowserSession(): boolean {
  try {
    return window.sessionStorage.getItem(BROWSER_SESSION_KEY) === 'active';
  } catch {
    return false;
  }
}

function markBrowserSession(): void {
  try {
    window.sessionStorage.setItem(BROWSER_SESSION_KEY, 'active');
  } catch {
    // Access Token still remains memory-only when browser storage is unavailable.
  }
}

function clearBrowserSession(): void {
  try {
    window.sessionStorage.removeItem(BROWSER_SESSION_KEY);
  } catch {
    // Nothing else is required when browser storage is unavailable.
  }
}

export class ApiError extends Error {
  constructor(public readonly messages: string[], public readonly status: number) {
    super(messages[0] ?? '요청을 처리하지 못했습니다.');
  }
}

async function readError(response: Response) {
  const fallback = response.status === 401
    ? '로그인 세션이 만료되었습니다.'
    : '요청을 처리하지 못했습니다.';
  try {
    const body = await response.json() as ApiErrorResponse;
    return new ApiError(body.messages?.length ? body.messages : [fallback], response.status);
  } catch {
    return new ApiError([fallback], response.status);
  }
}

export async function getCsrfToken(): Promise<CsrfResponse> {
  if (csrfCache !== null) return csrfCache;
  if (csrfInFlight === null) {
    csrfInFlight = fetch('/api/auth/csrf', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw await readError(response);
        csrfCache = await response.json() as CsrfResponse;
        return csrfCache;
      })
      .finally(() => {
        csrfInFlight = null;
      });
  }
  return csrfInFlight;
}

async function performAccessTokenRefresh(): Promise<boolean> {
  if (logoutInProgress || !hasBrowserSession()) return false;
  const requestedGeneration = authGeneration;
  try {
    const csrf = await getCsrfToken();
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { [csrf.headerName]: csrf.token },
    });
    if (!response.ok || logoutInProgress || requestedGeneration !== authGeneration) {
      clearAccessSession();
      return false;
    }
    const body = await response.json() as AccessSessionResponse;
    setAccessSession(body.expiresAt);
    return true;
  } catch {
    return false;
  }
}

async function performSerializedAccessTokenRefresh(): Promise<boolean> {
  if (typeof navigator !== 'undefined' && navigator.locks) {
    return navigator.locks.request(
      'zik00-refresh-token',
      { mode: 'exclusive' },
      performAccessTokenRefresh,
    );
  }
  return performAccessTokenRefresh();
}

function refreshAccessToken(): Promise<boolean> {
  if (refreshInFlight === null) {
    refreshInFlight = performSerializedAccessTokenRefresh()
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

function logoutExpiredSession(): Promise<void> {
  if (expiredSessionInFlight === null) {
    expiredSessionInFlight = (async () => {
      try {
        await logout();
      } catch {
        // Local credentials are cleared by logout() even when the server is unavailable.
      } finally {
        window.location.replace('/login?expired');
      }
    })();
  }
  return expiredSessionInFlight;
}

export async function fetchAuthenticated(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  if (!hasBrowserSession()) {
    clearAccessSession();
    return new Response(null, { status: 401 });
  }

  const initialAccessSession = hasActiveAccessSession();
  const hadExistingSession = hasBrowserSession() || initialAccessSession;
  let refreshAttempted = false;

  if (!initialAccessSession && !logoutInProgress) {
    refreshAttempted = true;
    const refreshed = await refreshAccessToken();
    if (!refreshed && hadExistingSession) {
      await logoutExpiredSession();
      return new Response(null, { status: 401 });
    }
  }

  const authenticatedOptions = () => ({ ...init, credentials: 'include' as const });
  let response = await fetch(input, authenticatedOptions());
  if (response.status === 401 && !logoutInProgress && !refreshAttempted) {
    if (await refreshAccessToken()) {
      response = await fetch(input, authenticatedOptions());
    } else if (hadExistingSession) {
      await logoutExpiredSession();
    }
  }
  return response;
}

export async function completeOAuthLogin(code: string): Promise<OAuthCompleteResponse> {
  const csrf = await getCsrfToken();
  const response = await fetch('/api/auth/oauth/complete', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify({ code }),
  });
  if (!response.ok) throw await readError(response);
  const result = await response.json() as OAuthCompleteResponse;
  if (result.expiresAt) setAccessSession(result.expiresAt);
  else clearAccessSession();
  markBrowserSession();
  return result;
}

export async function getAuthSession(): Promise<AuthSession> {
  if (sessionInFlight === null) {
    sessionInFlight = fetchAuthenticated('/api/auth/session')
      .then(async (response) => {
        if (!response.ok) throw await readError(response);
        return response.json() as Promise<AuthSession>;
      })
      .finally(() => {
        sessionInFlight = null;
      });
  }
  return sessionInFlight;
}

export async function searchJapaneseAddress(postalCode: string): Promise<AddressResult[]> {
  const cached = postalCodeCache.get(postalCode);
  if (cached && cached.expiresAt > Date.now()) return cached.results;
  if (cached) postalCodeCache.delete(postalCode);

  const existingRequest = postalCodeRequests.get(postalCode);
  if (existingRequest) return existingRequest;

  const query = new URLSearchParams({ postalCode });
  const request = fetch(`/api/japan-postal-codes?${query}`, { credentials: 'include' })
    .then(async (response) => {
      if (!response.ok) throw await readError(response);
      const results = await response.json() as AddressResult[];
      if (postalCodeCache.size >= MAX_POSTAL_CODE_CACHE_SIZE) {
        const oldestKey = postalCodeCache.keys().next().value as string | undefined;
        if (oldestKey) postalCodeCache.delete(oldestKey);
      }
      postalCodeCache.set(postalCode, {
        expiresAt: Date.now() + POSTAL_CODE_CACHE_TTL_MS,
        results,
      });
      return results;
    })
    .finally(() => {
      postalCodeRequests.delete(postalCode);
    });
  postalCodeRequests.set(postalCode, request);
  return request;
}

export async function getRegistrationDetailSession(): Promise<void> {
  if (detailSessionInFlight === null) {
    detailSessionInFlight = fetch('/api/auth/detail', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw await readError(response);
      })
      .finally(() => {
        detailSessionInFlight = null;
      });
  }
  return detailSessionInFlight;
}

export async function getRegistrationTerms(): Promise<TermsSessionResponse> {
  if (termsSessionInFlight === null) {
    termsSessionInFlight = fetch('/api/auth/terms', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw await readError(response);
        return response.json() as Promise<TermsSessionResponse>;
      })
      .finally(() => {
        termsSessionInFlight = null;
      });
  }
  return termsSessionInFlight;
}

export async function acceptRegistrationTerms(payload: TermsAgreementPayload): Promise<void> {
  const csrf = await getCsrfToken();
  const response = await fetch('/api/auth/terms', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw await readError(response);
}

export async function submitRegistrationDetail(payload: RegistrationDetailPayload): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetch('/api/auth/detail', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(payload),
  });
  if (!response.ok) throw await readError(response);
  const result = await response.json() as AccessSessionResponse;
  setAccessSession(result.expiresAt);
  markBrowserSession();
}

export async function logout(): Promise<void> {
  logoutInProgress = true;
  authGeneration += 1;
  clearBrowserSession();
  clearAccessSession();
  try {
    const response = await fetch('/logout', {
      method: 'POST',
      credentials: 'include',
    });
    if (!response.ok) throw await readError(response);
  } finally {
    clearAccessSession();
    csrfCache = null;
    sessionInFlight = null;
    logoutInProgress = false;
  }
}
