export function managedHref(value: string | null | undefined, fallback = '#') {
  const link = value?.trim();
  if (!link) return fallback;
  if (link.startsWith('/') || link.startsWith('#') || /^[a-z][a-z\d+.-]*:/i.test(link)) return link;
  return `https://${link}`;
}

export function isExternalHref(value: string) {
  return /^https?:\/\//i.test(value);
}
