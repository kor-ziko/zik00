const LOGIN_RETURN_TO_KEY = 'zik.auth.return-to';

export function currentRelativeUrl() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

export function safeReturnTo(value: string | null | undefined) {
  if (!value || !value.startsWith('/') || value.startsWith('//')) return null;
  if (value.startsWith('/login') || value.startsWith('/oauth')) return null;
  return value;
}

export function loginHref(returnTo: string) {
  const safeDestination = safeReturnTo(returnTo) ?? '/';
  return `/login?${new URLSearchParams({ returnTo: safeDestination }).toString()}`;
}

export function rememberLoginDestination(returnTo: string | null | undefined) {
  const safeDestination = safeReturnTo(returnTo);
  if (safeDestination === null) return;
  try {
    window.sessionStorage.setItem(LOGIN_RETURN_TO_KEY, safeDestination);
  } catch {
    // The server-provided login destination remains available as a fallback.
  }
}

export function consumeLoginDestination() {
  try {
    const destination = safeReturnTo(window.sessionStorage.getItem(LOGIN_RETURN_TO_KEY));
    window.sessionStorage.removeItem(LOGIN_RETURN_TO_KEY);
    return destination;
  } catch {
    return null;
  }
}
