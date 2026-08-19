import type { Product } from '../data';
import { getCsrfToken } from './auth';

export type LandedPriceEstimate = {
  sourceProductPrice: number;
  sourceCurrency: 'KRW' | 'JPY';
  operatingExchangeRate: number;
  convertedProductPrice: number;
  convertedLocalDistributionFee: number;
  agencyFee: number;
  payableNow: number;
  customsExchangeRate: number;
  customsRateFrom: string | null;
  customsRateTo: string | null;
  customsValue: number;
  dutyRate: number | null;
  estimatedDuty: number | null;
  estimatedConsumptionTax: number | null;
  estimatedImportCharges: number | null;
  estimatedTotalCost: number | null;
  customsStatus: 'ESTIMATED' | 'GENERAL_TARIFF_ESTIMATED' | 'EXEMPT_ESTIMATE' | 'HS_CODE_REQUIRED' | 'RATE_UNAVAILABLE';
  staleCustomsData: boolean;
  internationalShippingStatus: string;
  estimatedWeightMinGrams: number;
  estimatedWeightMaxGrams: number;
  estimatedInternationalShippingMin: number;
  estimatedInternationalShippingMax: number;
  estimatedInternationalShippingFee: number;
  estimatedTotalCostMin: number | null;
  estimatedTotalCostMax: number | null;
  hsCodeCandidate: string;
  customsClassificationMethod: 'RULE' | 'AI_ASSISTED';
  shippingEstimationBasis: string;
  notices: string[];
};

export type OperatingExchangeRate = {
  sourceCurrency: 'KRW';
  targetCurrency: 'JPY';
  rate: number;
  customsRateFrom: string | null;
  customsRateTo: string | null;
  stale: boolean;
};

let operatingRateCache: OperatingExchangeRate | null = null;
let operatingRateRequest: Promise<OperatingExchangeRate> | null = null;

export function getOperatingExchangeRate() {
  if (operatingRateCache) return Promise.resolve(operatingRateCache);
  if (!operatingRateRequest) {
    operatingRateRequest = fetch('/api/products/pricing/rate', { headers: { Accept: 'application/json' } })
      .then((response) => {
        if (!response.ok) throw new Error('운영환율을 불러오지 못했습니다.');
        return response.json() as Promise<OperatingExchangeRate>;
      })
      .then((result) => {
        operatingRateCache = result;
        return result;
      })
      .finally(() => { operatingRateRequest = null; });
  }
  return operatingRateRequest;
}

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

export async function resolveProductUrl(url: string) {
  const params = new URLSearchParams({ url });
  const response = await fetch(`/api/product-links/resolve?${params.toString()}`, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(response.status === 400
      ? '올바른 상품 URL을 입력해 주세요.'
      : '이 URL에서는 상품 정보를 가져오지 못했습니다.');
  }
  return response.json() as Promise<{ productId: string }>;
}

export async function getLandedPriceEstimate(input: {
  productName: string;
  category: string;
  unitPrice: number;
  currency: 'KRW' | 'JPY';
  quantity: number;
  localDistributionFee: number;
}, signal?: AbortSignal) {
  const csrf = await getCsrfToken();
  const response = await fetch('/api/products/pricing/estimate', {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(input),
    signal,
  });
  if (!response.ok) throw new Error('예상 관부가세를 계산하지 못했습니다.');
  return response.json() as Promise<LandedPriceEstimate>;
}
