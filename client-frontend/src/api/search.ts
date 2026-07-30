export type SearchFacet = {
  value: string;
  count: number;
};

export type SearchProduct = {
  productId: string;
  name: string;
  category: string;
  brand: string;
  price: number;
  originalPrice: number | null;
  currency: 'KRW' | 'JPY';
  sourceUrl: string | null;
  imageUrl: string;
  rating: number;
  reviewCount: number;
  freeShipping: boolean;
  source: string;
  badge: string | null;
};

export type SearchResult = {
  query: string;
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
  items: SearchProduct[];
  categories: SearchFacet[];
  brands: SearchFacet[];
};

export type SearchRequest = {
  query: string;
  category?: string;
  brands?: string[];
  minPrice?: number;
  maxPrice?: number;
  sort?: string;
  page?: number;
};

export async function searchProducts(request: SearchRequest, signal?: AbortSignal) {
  const params = new URLSearchParams({ q: request.query, size: '20' });
  if (request.category) params.set('category', request.category);
  request.brands?.forEach((brand) => params.append('brand', brand));
  if (request.minPrice !== undefined) params.set('minPrice', String(request.minPrice));
  if (request.maxPrice !== undefined) params.set('maxPrice', String(request.maxPrice));
  if (request.sort) params.set('sort', request.sort);
  if (request.page !== undefined) params.set('page', String(request.page));

  const response = await fetch(`/api/search?${params.toString()}`, {
    headers: { Accept: 'application/json' },
    signal,
  });
  if (!response.ok) {
    throw new Error('검색 결과를 불러오지 못했습니다.');
  }
  return response.json() as Promise<SearchResult>;
}
