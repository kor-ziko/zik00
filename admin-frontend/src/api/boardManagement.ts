const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
const AUTH_EXPIRED_EVENT = 'admin-auth-expired';

export type NoticePayload = {
  category: string;
  title: string;
  content: string;
  pinned: boolean;
  published: boolean;
  publishedAt: string | null;
};

export type AdminNotice = NoticePayload & {
  id: number;
  updatedAt: string | null;
};

export type NoticeCategory = { id: number; name: string; displayOrder: number };

export type ReviewComment = {
  id: number;
  adminId: number;
  adminName: string;
  content: string;
  createdAt: string;
};

export type AdminReview = {
  id: number;
  authorName: string;
  title: string;
  content: string;
  rating: number;
  productName: string;
  imageUrl: string | null;
  featured: boolean;
  published: boolean;
  createdAt: string;
  updatedAt: string | null;
  comments: ReviewComment[];
};

export type InquirySummary = {
  inquiryId: number;
  memberId: number;
  memberName: string;
  title: string;
  answered: boolean;
  createdAt: string;
  commentCount: number;
  imageCount: number;
};

export type InquiryImage = { imageUuid: string; imageUrl: string };
export type InquiryComment = {
  commentId: number;
  writerType: 'USER' | 'ADMIN';
  writerName: string;
  content: string;
  createdAt: string;
  images: InquiryImage[];
};
export type InquiryDetail = {
  inquiryId: number;
  memberId: number;
  memberName: string;
  memberNickname: string | null;
  memberEmail: string | null;
  title: string;
  content: string;
  answered: boolean;
  createdAt: string;
  images: InquiryImage[];
  comments: InquiryComment[];
};

export const boardApi = {
  noticeCategories: () => request<NoticeCategory[]>('/api/admin/board-management/notice-categories'),
  createNoticeCategory: (name: string) => request<NoticeCategory>('/api/admin/board-management/notice-categories', json('POST', { name })),
  createNotice: (payload: NoticePayload) => request<AdminNotice>('/api/admin/board-management/notices', json('POST', payload)),
  notices: () => request<AdminNotice[]>('/api/admin/board-management/notices'),
  updateNotice: (id: number, payload: NoticePayload) => request<AdminNotice>(`/api/admin/board-management/notices/${id}`, json('PUT', payload)),
  deleteNotice: (id: number) => request<void>(`/api/admin/board-management/notices/${id}`, { method: 'DELETE' }),
  reviews: () => request<AdminReview[]>('/api/admin/board-management/reviews'),
  updateReview: (id: number, payload: Omit<AdminReview, 'id' | 'createdAt' | 'updatedAt' | 'comments'>) =>
    request<AdminReview>(`/api/admin/board-management/reviews/${id}`, json('PUT', payload)),
  deleteReview: (id: number) => request<void>(`/api/admin/board-management/reviews/${id}`, { method: 'DELETE' }),
  addReviewComment: (id: number, content: string) =>
    request<AdminReview>(`/api/admin/board-management/reviews/${id}/comments`, json('POST', { content })),
  uploadReviewImage: (image: File) => {
    const body = new FormData();
    body.append('image', image);
    return request<{ imageUrl: string }>('/api/admin/board-management/review-images', { method: 'POST', body });
  },
  inquiries: () => request<InquirySummary[]>('/api/admin/inquiries'),
  inquiry: (id: number) => request<InquiryDetail>(`/api/admin/inquiries/${id}`),
  replyInquiry: (id: number, content: string, images: File[]) => {
    const body = new FormData();
    body.append('content', content);
    images.forEach((image) => body.append('images', image));
    return request<InquiryDetail>(`/api/admin/inquiries/${id}/replies`, { method: 'POST', body });
  },
};

function json(method: string, body: unknown): RequestInit {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.method && !['GET', 'HEAD'].includes(init.method.toUpperCase())) {
    const csrf = await fetch(`${API_BASE_URL}/api/admin/auth/csrf`, { credentials: 'include' });
    if (!csrf.ok) throw new Error('보안 토큰을 발급받지 못했습니다.');
    const token = await csrf.json() as { headerName: string; token: string };
    headers.set(token.headerName, token.token);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers, credentials: 'include' });
  if (response.status === 401) window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
  if (!response.ok) {
    const payload = await response.json().catch(() => null) as { message?: string; detail?: string } | null;
    throw new Error(payload?.message ?? payload?.detail ?? '요청을 처리하지 못했습니다.');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
