import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';
import { AuthMemoryProvider } from './auth/AuthMemory';

createRoot(document.getElementById('root')!).render(
  <StrictMode><AuthMemoryProvider><App /></AuthMemoryProvider></StrictMode>,
);
