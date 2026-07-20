import { createContext, type ReactNode, useContext, useEffect, useState } from 'react';

type TokenListener = (token: string | null) => void;

let memoryAccessToken: string | null = null;
const listeners = new Set<TokenListener>();

export function getMemoryAccessToken() {
  return memoryAccessToken;
}

export function setMemoryAccessToken(token: string | null) {
  memoryAccessToken = token;
  listeners.forEach((listener) => listener(token));
}

const AuthMemoryContext = createContext<{ accessToken: string | null }>({ accessToken: null });

export function AuthMemoryProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(memoryAccessToken);

  useEffect(() => {
    listeners.add(setAccessToken);
    return () => {
      listeners.delete(setAccessToken);
    };
  }, []);

  return <AuthMemoryContext.Provider value={{ accessToken }}>{children}</AuthMemoryContext.Provider>;
}

export function useAuthMemory() {
  return useContext(AuthMemoryContext);
}
