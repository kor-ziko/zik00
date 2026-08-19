export function openClientPage(path: string) {
  const base = import.meta.env.VITE_CLIENT_URL
    ?? (window.location.port === '5173'
      ? `${window.location.protocol}//${window.location.hostname}:5174`
      : window.location.origin);
  window.open(new URL(path, base).toString(), '_blank', 'noopener,noreferrer');
}
