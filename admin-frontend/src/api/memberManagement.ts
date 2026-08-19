const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
const AUTH_EXPIRED_EVENT = 'admin-auth-expired';

export type MemberListItem = {
  id: number; name: string; nickname: string | null; loginId: string | null; email: string | null;
  phone: string | null; status: string; completedOrderCount: number; rewardPoint: number;
  depositBalance: number; joinedDate: string | null;
};

export type WithdrawnMember = {
  id: number; name: string; nickname: string | null; loginId: string | null; email: string | null;
  joinedDate: string | null; withdrawnAt: string | null; memo: string | null;
};

export type RewardPointMember = { memberId: number; name: string; nickname: string | null; loginId: string | null; balance: number };
export type RewardPointHistory = {
  id: number; memberId: number; memberName: string; loginId: string; amount: number;
  balanceAfter: number; reason: string; createdAt: string;
};

export type DepositRequestItem = {
  id: number; memberId: number; memberName: string; loginId: string; amount: number; depositorName: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED'; adminMemo: string | null; requestedAt: string; processedAt: string | null;
};

export type DepositHistory = {
  id: number; memberId: number; memberName: string; loginId: string; transactionType: string;
  amount: number; balanceAfter: number; description: string; createdAt: string;
};

const base = '/api/admin/member-management';
export const memberManagementApi = {
  members: () => request<MemberListItem[]>(`${base}/members`),
  withdrawMember: (memberId: number) => request<void>(`${base}/members/${memberId}`, { method: 'DELETE' }),
  withdrawnMembers: () => request<WithdrawnMember[]>(`${base}/withdrawn-members`),
  pointMembers: () => request<RewardPointMember[]>(`${base}/reward-points/members`),
  pointHistories: () => request<RewardPointHistory[]>(`${base}/reward-points/histories`),
  adjustPoint: (memberId: number, amount: number, reason: string) =>
    request<RewardPointHistory>(`${base}/reward-points/adjustments`, json('POST', { memberId, amount, reason })),
  depositRequests: () => request<DepositRequestItem[]>(`${base}/deposit-requests`),
  approveDeposit: (id: number, memo: string) => request<DepositRequestItem>(`${base}/deposit-requests/${id}/approve`, json('POST', { memo })),
  rejectDeposit: (id: number, memo: string) => request<DepositRequestItem>(`${base}/deposit-requests/${id}/reject`, json('POST', { memo })),
  depositHistories: () => request<DepositHistory[]>(`${base}/deposit-histories`),
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
    throw new Error(payload?.message || payload?.detail || '요청을 처리하지 못했습니다.');
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
