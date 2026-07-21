import { getMemoryAccessToken, setMemoryAccessToken } from '../auth/AuthMemory';

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
type AccessTokenResponse = { accessToken: string; expiresAt: string };
type OAuthCompleteResponse = {
  accessToken: string | null;
  expiresAt: string | null;
  destination: string;
};
type TermsSessionResponse = { accepted: boolean; alarmConsent: boolean };
type TermsAgreementPayload = {
  accepted: boolean;
  alarmConsent: boolean;
};

let refreshInFlight: Promise<boolean> | null = null;
let logoutInProgress = false;
let authGeneration = 0;
let csrfInFlight: Promise<CsrfResponse> | null = null;
let sessionInFlight: Promise<AuthSession> | null = null;
let detailSessionInFlight: Promise<void> | null = null;
let termsSessionInFlight: Promise<TermsSessionResponse> | null = null;
const postalCodeCache = new Map<string, { expiresAt: number; results: AddressResult[] }>();
const postalCodeRequests = new Map<string, Promise<AddressResult[]>>();
const POSTAL_CODE_CACHE_TTL_MS = 30 * 60 * 1_000;
const MAX_POSTAL_CODE_CACHE_SIZE = 32;

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
  if (csrfInFlight === null) {
    csrfInFlight = fetch('/api/auth/csrf', { credentials: 'include' })
      .then(async (response) => {
        if (!response.ok) throw await readError(response);
        return response.json() as Promise<CsrfResponse>;
      })
      .finally(() => {
        csrfInFlight = null;
      });
  }
  return csrfInFlight;
}

async function performAccessTokenRefresh(): Promise<boolean> {
  if (logoutInProgress) return false;
  const requestedGeneration = authGeneration;
  try {
    const csrf = await getCsrfToken();
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { [csrf.headerName]: csrf.token },
    });
    if (!response.ok || logoutInProgress || requestedGeneration !== authGeneration) {
      setMemoryAccessToken(null);
      return false;
    }
    const body = await response.json() as AccessTokenResponse;
    setMemoryAccessToken(body.accessToken);
    return true;
  } catch {
    return false;
  }
}

function refreshAccessToken(): Promise<boolean> {
  if (refreshInFlight === null) {
    refreshInFlight = performAccessTokenRefresh()
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

export async function fetchAuthenticated(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const authenticatedOptions = () => {
    const headers = new Headers(init?.headers);
    const accessToken = getMemoryAccessToken();
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
    return { ...init, headers, credentials: 'include' as const };
  };
  let response = await fetch(input, authenticatedOptions());
  if (response.status === 401 && !logoutInProgress && await refreshAccessToken()) {
    response = await fetch(input, authenticatedOptions());
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
  setMemoryAccessToken(result.accessToken ?? null);
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
  const result = await response.json() as AccessTokenResponse;
  setMemoryAccessToken(result.accessToken);
}

export async function logout(): Promise<void> {
  const csrf = await getCsrfToken();
  const headers = new Headers({ [csrf.headerName]: csrf.token });
  const accessToken = getMemoryAccessToken();
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
  logoutInProgress = true;
  authGeneration += 1;
  setMemoryAccessToken(null);
  try {
    const response = await fetch('/logout', {
      method: 'POST',
      credentials: 'include',
      headers,
    });
    if (!response.ok) throw await readError(response);
  } finally {
    setMemoryAccessToken(null);
    sessionInFlight = null;
    logoutInProgress = false;
  }
}
