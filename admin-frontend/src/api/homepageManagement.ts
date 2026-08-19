const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

export type HomepageContent = {
  id: number;
  contentType: string;
  title: string;
  subtitle: string | null;
  content: string | null;
  imageUrl: string | null;
  linkUrl: string | null;
  linkLabel: string | null;
  applicationType: string | null;
  displayOrder: number;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type HomepageContentPayload = Omit<HomepageContent, 'id' | 'contentType' | 'createdAt' | 'updatedAt'>;

export type SitePage = {
  label: string;
  path: string;
  group: string;
};

export function getSitePages() {
  return request<SitePage[]>('/api/admin/web-management/site-pages');
}

export function homepageContentApi(endpoint: string) {
  const base = `/api/admin/web-management/${endpoint}`;
  return {
    findAll: () => request<HomepageContent[]>(base),
    create: (payload: HomepageContentPayload) => request<HomepageContent>(base, json('POST', payload)),
    update: (id: number, payload: HomepageContentPayload) => request<HomepageContent>(`${base}/${id}`, json('PUT', payload)),
    delete: (id: number) => request<void>(`${base}/${id}`, { method: 'DELETE' }),
  };
}

export function uploadHomepageImage(image: File) {
  const formData = new FormData();
  formData.append('image', image);
  return request<{ imageUrl: string }>('/api/admin/web-management/images', { method: 'POST', body: formData });
}

function json(method: string, body: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.method && !['GET', 'HEAD'].includes(init.method.toUpperCase())) {
    const csrf = await fetch(`${API_BASE_URL}/api/admin/auth/csrf`, { credentials: 'include' });
    if (csrf.ok) {
      const token = await csrf.json() as { headerName: string; token: string };
      headers.set(token.headerName, token.token);
    }
  }
  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers, credentials: 'include' });
  if (response.status === 401) window.dispatchEvent(new Event('admin-auth-expired'));
  if (!response.ok) {
    const body = await response.json().catch(() => null) as { message?: string; detail?: string } | null;
    throw new Error(body?.message ?? body?.detail ?? '요청을 처리하지 못했습니다.');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
