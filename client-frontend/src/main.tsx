import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';
import { AuthMemoryProvider } from './auth/AuthMemory';
import { AppErrorBoundary } from './components/error/ErrorPage';
import { LocaleProvider } from './locale';
import { HomepageContentProvider } from './homepage/HomepageContentContext';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <AppErrorBoundary>
      <LocaleProvider><AuthMemoryProvider><HomepageContentProvider><App /></HomepageContentProvider></AuthMemoryProvider></LocaleProvider>
    </AppErrorBoundary>
  </StrictMode>,
);
