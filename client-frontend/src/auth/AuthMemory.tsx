import { createContext, type ReactNode, useContext, useEffect, useState } from 'react';

type AuthMemoryState = {
  accessSessionActive: boolean;
  expiresAt: number | null;
};

type TokenListener = (state: AuthMemoryState) => void;

const LEGACY_ACCESS_TOKEN_KEY = 'zik.auth.access-token';
const ACCESS_TOKEN_EXPIRES_AT_KEY = 'zik.auth.access-token-expires-at';
const listeners = new Set<TokenListener>();
const MAX_REFRESH_EARLY_MS = 5_000;
const MIN_REFRESH_EARLY_MS = 1_000;

function loadSessionState(): AuthMemoryState {
  try {
    window.sessionStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
    const expiresAtValue = window.sessionStorage.getItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    const expiresAt = expiresAtValue === null ? null : Number(expiresAtValue);
    if (expiresAt !== null && Number.isFinite(expiresAt) && expiresAt > Date.now()) {
      return { accessSessionActive: true, expiresAt };
    }
    window.sessionStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
  } catch {
    // Fall back to memory-only authentication when browser storage is unavailable.
  }
  return { accessSessionActive: false, expiresAt: null };
}

let memoryState: AuthMemoryState = loadSessionState();

function persistSessionState(state: AuthMemoryState) {
  try {
    window.sessionStorage.removeItem(LEGACY_ACCESS_TOKEN_KEY);
    if (state.accessSessionActive && state.expiresAt !== null) {
      window.sessionStorage.setItem(ACCESS_TOKEN_EXPIRES_AT_KEY, String(state.expiresAt));
    } else {
      window.sessionStorage.removeItem(ACCESS_TOKEN_EXPIRES_AT_KEY);
    }
  } catch {
    // The in-memory state remains authoritative when browser storage is unavailable.
  }
}

export function hasActiveAccessSession() {
  if (memoryState.expiresAt !== null && memoryState.expiresAt <= Date.now()) {
    clearAccessSession();
  }
  return memoryState.accessSessionActive;
}

export function setAccessSession(expiresAt: string) {
  const parsedExpiresAt = Date.parse(expiresAt);
  const nextState: AuthMemoryState = {
    accessSessionActive: Number.isFinite(parsedExpiresAt),
    expiresAt: Number.isFinite(parsedExpiresAt)
      ? parsedExpiresAt
      : null,
  };
  if (memoryState.accessSessionActive === nextState.accessSessionActive
      && memoryState.expiresAt === nextState.expiresAt) {
    return;
  }
  memoryState = nextState;
  persistSessionState(memoryState);
  listeners.forEach((listener) => listener(memoryState));
}

export function clearAccessSession() {
  const nextState: AuthMemoryState = {
    accessSessionActive: false,
    expiresAt: null,
  };
  if (!memoryState.accessSessionActive && memoryState.expiresAt === null) return;
  memoryState = nextState;
  persistSessionState(memoryState);
  listeners.forEach((listener) => listener(memoryState));
}

const AuthMemoryContext = createContext<AuthMemoryState>(memoryState);

export function AuthMemoryProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthMemoryState>(memoryState);

  useEffect(() => {
    listeners.add(setState);
    return () => {
      listeners.delete(setState);
    };
  }, []);

  useEffect(() => {
    if (state.expiresAt === null) return;
    const remainingMs = state.expiresAt - Date.now();
    if (remainingMs <= 0) {
      clearAccessSession();
      return;
    }
    const refreshEarlyMs = Math.min(
      MAX_REFRESH_EARLY_MS,
      Math.max(MIN_REFRESH_EARLY_MS, remainingMs * 0.2),
    );
    const timeoutId = window.setTimeout(
      clearAccessSession,
      Math.max(0, remainingMs - refreshEarlyMs),
    );
    return () => window.clearTimeout(timeoutId);
  }, [state.expiresAt]);

  return <AuthMemoryContext.Provider value={state}>{children}</AuthMemoryContext.Provider>;
}

export function useAuthMemory() {
  return useContext(AuthMemoryContext);
}
