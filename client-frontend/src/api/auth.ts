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

export type AdditionalInfoPayload = {
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
  alarmConsent: boolean;
};

type CsrfResponse = { headerName: string; token: string };
type ApiErrorResponse = { messages?: string[] };

let refreshInFlight: Promise<boolean> | null = null;

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
  const response = await fetch('/api/auth/csrf', { credentials: 'include' });
  if (!response.ok) throw await readError(response);
  return response.json() as Promise<CsrfResponse>;
}

async function performAccessTokenRefresh(): Promise<boolean> {
  try {
    const csrf = await getCsrfToken();
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include',
      headers: { [csrf.headerName]: csrf.token },
    });
    return response.ok;
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
  const options = { ...init, credentials: 'include' as const };
  let response = await fetch(input, options);
  if (response.status === 401 && await refreshAccessToken()) {
    response = await fetch(input, options);
  }
  return response;
}

export async function getAuthSession(): Promise<AuthSession> {
  const response = await fetchAuthenticated('/api/auth/session');
  if (!response.ok) throw await readError(response);
  return response.json() as Promise<AuthSession>;
}

export async function searchJapaneseAddress(postalCode: string): Promise<AddressResult[]> {
  const query = new URLSearchParams({ postalCode });
  const response = await fetch(`/api/japan-postal-codes?${query}`, { credentials: 'include' });
  if (!response.ok) throw await readError(response);
  return response.json() as Promise<AddressResult[]>;
}

export async function submitAdditionalInfo(payload: AdditionalInfoPayload): Promise<void> {
  const csrf = await getCsrfToken();

  const response = await fetchAuthenticated('/api/auth/additional-info', {
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

export async function logout(): Promise<void> {
  const csrf = await getCsrfToken();
  const response = await fetch('/logout', {
    method: 'POST',
    credentials: 'include',
    headers: { [csrf.headerName]: csrf.token },
  });
  if (!response.ok) throw await readError(response);
}
