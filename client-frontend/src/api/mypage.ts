import { fetchAuthenticated, getCsrfToken } from './auth';

export type MypageSummary = {
  completedOrderCount: number;
  deliveryTrackingCount: number;
  inquiryCount: number;
  couponCount: number;
  depositBalance: number;
  rewardPoint: number;
  memberNickname: string;
};

export type MypageProfile = {
  name: string;
  nickname: string;
  telephone: string;
  mobilePhone: string;
  email: string;
  alarmConsent: boolean;
};

export type Purchase = {
  orderNumber: string;
  productName: string;
  quantity: number;
  paymentAmount: number;
  orderStatus: string;
  orderedDate: string;
};

export type Coupon = {
  couponName: string;
  discountType: string;
  discountValue: number;
  minimumOrderAmount: number;
  startedDate: string;
  expiredDate: string;
  used: boolean;
};

export type InquiryImage = { imageUuid: string; imageUrl: string };
export type InquiryComment = {
  commentId: number;
  writerName: string;
  writerType: string;
  content: string;
  createdAt: string;
  images: InquiryImage[];
  admin: boolean;
};
export type InquiryThread = {
  inquiry: { inquiryId: number; title: string; content: string; status: boolean; createdAt: string };
  comments: InquiryComment[];
  images: InquiryImage[];
};

export type DeliveryAddress = {
  id: number;
  addressName: string;
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  province: string;
  detailAddress: string;
  defaultAddress: boolean;
};

export type Dashboard = {
  summary: MypageSummary;
  profile: MypageProfile;
  recentOrders: Purchase[];
  coupons: Coupon[];
};

export type ProfileData = { profile: MypageProfile; addresses: DeliveryAddress[] };
export type ProfileUpdatePayload = MypageProfile;
export type AddressUpdatePayload = {
  addressName: string;
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  province: string;
  baseAddress: string;
  detailAddress: string;
  defaultAddress: boolean;
};
export type MypageSection = 'home' | 'orders' | 'deliveries' | 'inquiries' | 'coupons' | 'deposits' | 'profile';
export type SectionData = Purchase[] | InquiryThread[] | Coupon[] | MypageSummary | ProfileData;

async function requestJson<T>(path: string): Promise<T> {
  const response = await fetchAuthenticated(path);
  if (response.status === 401) {
    window.location.replace('/login');
    throw new Error('로그인이 필요합니다.');
  }
  if (!response.ok) throw new Error('마이페이지 정보를 불러오지 못했습니다.');
  return response.json() as Promise<T>;
}

export function getMypageDashboard() {
  return requestJson<Dashboard>('/api/mypage');
}

export function getMypageSection(section: Exclude<MypageSection, 'home'>) {
  return requestJson<SectionData>(`/api/mypage/${section}`);
}

export async function createInquiry(title: string, content: string, images: File[]) {
  const csrf = await getCsrfToken();
  const body = new FormData();
  body.append('title', title);
  body.append('content', content);
  images.forEach((file) => body.append('images', file));

  const response = await fetchAuthenticated('/api/mypage/inquiries', {
    method: 'POST',
    headers: { [csrf.headerName]: csrf.token },
    body,
  });
  if (response.ok) return;

  try {
    const error = await response.json() as { messages?: string[] };
    throw new Error(error.messages?.[0] || '문의를 등록하지 못했습니다.');
  } catch (reason) {
    if (reason instanceof Error) throw reason;
    throw new Error('문의를 등록하지 못했습니다.');
  }
}

async function mutateJson(path: string, method: 'POST' | 'PUT' | 'DELETE', body?: unknown) {
  const csrf = await getCsrfToken();
  const response = await fetchAuthenticated(path, {
    method,
    headers: {
      [csrf.headerName]: csrf.token,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (response.ok) return;
  const error = await response.json().catch(() => ({})) as { messages?: string[] };
  throw new Error(error.messages?.[0] || '회원정보를 저장하지 못했습니다.');
}

export function updateMypageProfile(payload: ProfileUpdatePayload) {
  return mutateJson('/api/mypage/profile', 'PUT', payload);
}

export function addDeliveryAddress(payload: AddressUpdatePayload) {
  return mutateJson('/api/mypage/profile/addresses', 'POST', payload);
}

export function updateDeliveryAddress(addressId: number, payload: AddressUpdatePayload) {
  return mutateJson(`/api/mypage/profile/addresses/${addressId}`, 'PUT', payload);
}

export function deleteDeliveryAddress(addressId: number) {
  return mutateJson(`/api/mypage/profile/addresses/${addressId}`, 'DELETE');
}
