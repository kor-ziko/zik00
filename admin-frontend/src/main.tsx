import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import './index.css';
import App from './App';

const isLocalAdminAlias = window.location.hostname === 'localhost'
  && window.location.port === '5173';

if (isLocalAdminAlias) {
  const destination = new URL(window.location.href);
  destination.hostname = '127.0.0.1';
  window.location.replace(destination.toString());
} else {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </StrictMode>,
  );
}
