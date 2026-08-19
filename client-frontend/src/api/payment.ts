import { fetchAuthenticated, getCsrfToken } from './auth';
import { ShoppingAuthRequiredError } from './shopping';

export type PaymentItem = {
  cartItemId: number;
  productId: string;
  productName: string;
  brand?: string;
  imageUrl?: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
  selectedOptions: Record<string, string>;
};

export type PaymentMethod = {
  code: 'credit3d2' | 'paypay' | 'paypal';
  label: string;
  description: string;
};

export const defaultPaymentMethods: PaymentMethod[] = [
  { code: 'credit3d2', label: '신용·체크카드', description: 'Visa, Mastercard, JCB 등' },
  { code: 'paypay', label: 'PayPay', description: 'PayPay 앱 또는 계정으로 결제' },
  { code: 'paypal', label: 'PayPal', description: 'PayPal 계정으로 결제' },
];

export type PaymentPrepareResponse = {
  paymentId: string;
  orderName: string;
  totalAmount: number;
  currency: 'KRW' | 'JPY';
  paymentEnabled: boolean;
  paymentProvider: 'SBPS';
  paymentMethods: PaymentMethod[];
  productAmount: number;
  domesticShippingFee: number;
  agencyFee: number;
  estimatedShippingFee: number;
  estimatedShippingMin: number;
  estimatedShippingMax: number;
  estimatedDuty: number;
  estimatedConsumptionTax: number;
  estimatedImportCharges: number;
  customsFinalizationRequired: boolean;
  deliveryAddress: {
    id: number;
    addressName: string;
    receiverName: string;
    receiverPhone: string;
    zipCode: string;
    province: string;
    detailAddress: string;
  };
  items: PaymentItem[];
};

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const csrf = await getCsrfToken();
  let response: Response;
  try {
    response = await fetchAuthenticated(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
      body: JSON.stringify(body),
    });
  } catch {
    throw new Error('결제 서버에 연결할 수 없습니다. 백엔드와 Redis 실행 상태를 확인해 주세요.');
  }
  if (response.status === 401) throw new ShoppingAuthRequiredError('로그인이 필요합니다.');
  if (!response.ok) {
    const contentType = response.headers.get('content-type') || '';
    if (!contentType.includes('application/json') && !contentType.includes('application/problem+json')) {
      const message = (await response.text()).trim();
      throw new Error(message || '결제 요청을 처리하지 못했습니다.');
    }
    const error = await response.json().catch(() => ({})) as {
      detail?: string;
      message?: string;
      messages?: string[];
      title?: string;
      errors?: Array<{ defaultMessage?: string; message?: string }>;
    };
    throw new Error(
      error.messages?.find(Boolean)
      || error.errors?.find((item) => item.defaultMessage || item.message)?.defaultMessage
      || error.errors?.find((item) => item.defaultMessage || item.message)?.message
      || error.detail
      || error.message
      || error.title
      || '결제 요청을 처리하지 못했습니다.',
    );
  }
  return response.json() as Promise<T>;
}

type PaymentPrepareWireResponse = Omit<PaymentPrepareResponse, 'paymentMethods'> & {
  paymentMethods?: PaymentMethod[];
};

export async function preparePayment(cartItemIds: number[], deliveryAddressId: number) {
  const response = await postJson<PaymentPrepareWireResponse>(
    '/api/payment/prepare', { cartItemIds, deliveryAddressId },
  );
  return {
    ...response,
    paymentMethods: response.paymentMethods?.length ? response.paymentMethods : defaultPaymentMethods,
  } satisfies PaymentPrepareResponse;
}

export type PaymentStartResponse = {
  requestUrl: string;
  fields: Record<string, string>;
};

export const startPayment = (paymentId: string, paymentMethod: string) =>
  postJson<PaymentStartResponse>('/api/payment/start', { paymentId, paymentMethod });
