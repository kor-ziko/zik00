export type NoticeSummary = {
  id: number;
  category: string;
  title: string;
  pinned: boolean;
  publishedAt: string;
};

export type NoticeDetail = NoticeSummary & {
  content: string;
  updatedAt: string | null;
};

export type NoticeListResponse = {
  items: NoticeSummary[];
  categories: string[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
};

export async function getNotices(category: string, page: number, signal?: AbortSignal): Promise<NoticeListResponse> {
  const params = new URLSearchParams({ page: String(page), size: '10' });
  if (category && category !== '전체') params.set('category', category);
  const response = await fetch(`/api/notices?${params}`, { signal, credentials: 'include' });
  if (!response.ok) throw new Error('공지사항을 불러오지 못했습니다.');
  return response.json() as Promise<NoticeListResponse>;
}

export async function getNotice(id: number, signal?: AbortSignal): Promise<NoticeDetail> {
  const response = await fetch(`/api/notices/${id}`, { signal, credentials: 'include' });
  if (!response.ok) throw new Error('공지사항을 찾을 수 없습니다.');
  return response.json() as Promise<NoticeDetail>;
}
