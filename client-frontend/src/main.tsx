import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';
import { AuthMemoryProvider } from './auth/AuthMemory';
import { AppErrorBoundary } from './components/error/ErrorPage';
import { LocaleProvider } from './locale';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <LocaleProvider><AuthMemoryProvider><App /></AuthMemoryProvider></LocaleProvider>
    </AppErrorBoundary>
  </StrictMode>,
);
