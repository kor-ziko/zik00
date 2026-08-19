export type RatingCount = { rating: number; count: number };

export type ReviewItem = {
  id: number;
  authorName: string;
  title: string;
  content: string;
  rating: number;
  productName: string;
  imageUrl: string | null;
  featured: boolean;
  createdAt: string;
};

export type ReviewListResponse = {
  items: ReviewItem[];
  averageRating: number;
  totalCount: number;
  ratingCounts: RatingCount[];
  page: number;
  size: number;
  totalPages: number;
};

export async function getReviews(
  rating: number | null,
  sort: string,
  page: number,
  signal?: AbortSignal,
): Promise<ReviewListResponse> {
  const params = new URLSearchParams({ sort, page: String(page), size: '9' });
  if (rating) params.set('rating', String(rating));
  const response = await fetch(`/api/reviews?${params}`, { signal, credentials: 'include' });
  if (!response.ok) throw new Error('리뷰를 불러오지 못했습니다.');
  return response.json() as Promise<ReviewListResponse>;
}
