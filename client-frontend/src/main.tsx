import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';
import { AuthMemoryProvider } from './auth/AuthMemory';
import { AppErrorBoundary } from './components/error/ErrorPage';

createRoot(document.getElementById('root')!).render(
  <StrictMode><AppErrorBoundary><AuthMemoryProvider><App /></AuthMemoryProvider></AppErrorBoundary></StrictMode>,
);
