import { fetchAuthenticated, getCsrfToken } from './auth';

export type ProductSnapshot = {
  productId: string;
  productName: string;
  brand?: string;
  imageUrl?: string;
  price: number;
  currency: 'KRW' | 'JPY';
  sourceUrl?: string;
};

export type WishlistItem = ProductSnapshot & {
  id: number;
  createdAt: string;
};

export type CartItem = Omit<ProductSnapshot, 'price'> & {
  id: number;
  unitPrice: number;
  selectedOptions: Record<string, string>;
  quantity: number;
  createdAt: string;
};

export type CartResponse = { items: CartItem[]; itemCount: number };
export type ShoppingCounts = { wishlist: number; cart: number };

export class ShoppingAuthRequiredError extends Error {}

async function readError(response: Response, fallback: string): Promise<never> {
  if (response.status === 401) throw new ShoppingAuthRequiredError('로그인이 필요합니다.');
  const body = await response.json().catch(() => ({})) as {
    detail?: string;
    error?: string;
    messages?: string[];
    message?: string;
    title?: string;
    errors?: Array<{ defaultMessage?: string; message?: string }>;
  };
  const validationError = body.errors?.find((item) => item.defaultMessage || item.message);
  throw new Error(
    body.messages?.find(Boolean)
    || validationError?.defaultMessage
    || validationError?.message
    || body.detail
    || body.message
    || body.error
    || body.title
    || fallback,
  );
}

async function getJson<T>(path: string): Promise<T> {
  let response: Response;
  try {
    response = await fetchAuthenticated(path);
  } catch {
    throw new Error('서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.');
  }
  if (!response.ok) return readError(response, '정보를 불러오지 못했습니다.');
  return response.json() as Promise<T>;
}

async function mutateJson<T>(path: string, method: 'POST' | 'PATCH' | 'DELETE', body?: unknown): Promise<T> {
  const csrf = await getCsrfToken();
  let response: Response;
  try {
    response = await fetchAuthenticated(path, {
      method,
      headers: {
        [csrf.headerName]: csrf.token,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error('서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.');
  }
  if (!response.ok) return readError(response, '요청을 처리하지 못했습니다.');
  window.dispatchEvent(new Event('shopping-changed'));
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}

export const getWishlist = () => getJson<WishlistItem[]>('/api/wishlist');
export const getWishlistStatus = (productId: string) => getJson<{ wished: boolean }>(`/api/wishlist/${encodeURIComponent(productId)}/status`);
export const addWishlist = (product: ProductSnapshot) => mutateJson<WishlistItem>('/api/wishlist', 'POST', product);
export const removeWishlist = (productId: string) => mutateJson<void>(`/api/wishlist/${encodeURIComponent(productId)}`, 'DELETE');

export const getCart = () => getJson<CartResponse>('/api/cart');
export const addCartItem = ({ price, ...item }: ProductSnapshot & { selectedOptions: Record<string, string>; quantity: number }) => mutateJson<CartItem>('/api/cart', 'POST', { ...item, unitPrice: price });
export const updateCartQuantity = (itemId: number, quantity: number) => mutateJson<CartItem>(`/api/cart/${itemId}`, 'PATCH', { quantity });
export const removeCartItem = (itemId: number) => mutateJson<void>(`/api/cart/${itemId}`, 'DELETE');

export async function getShoppingCounts(): Promise<ShoppingCounts> {
  const [wishlist, cart] = await Promise.all([
    getJson<number>('/api/wishlist/count'),
    getJson<number>('/api/cart/count'),
  ]);
  return { wishlist, cart };
}
