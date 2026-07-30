import type { Product } from '../data';

export async function getProductDetail(productId: string, signal?: AbortSignal) {
  const response = await fetch(`/api/products/${encodeURIComponent(productId)}`, {
    headers: { Accept: 'application/json' },
    signal,
  });
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error('상품 정보를 불러오지 못했습니다.');
  }
  return response.json() as Promise<Product>;
}
