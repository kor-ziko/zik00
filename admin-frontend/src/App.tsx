import {
  Briefcase,
  CircleAlert,
  LayoutDashboard,
  LockKeyhole,
  LogOut,
  MapPin,
  MessageSquareText,
  PanelTop,
  RefreshCw,
  Search,
  Settings,
  SlidersHorizontal,
  TicketPercent,
  UserRound,
  UsersRound,
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { NavLink, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import './App.css';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
const ADMIN_AUTH_EXPIRED_EVENT = 'admin-auth-expired';
const ALL = 'ALL';

type AuthProvider = 'LOCAL' | 'KAKAO' | 'LINE';
type MemberRole = 'USER' | 'ADMIN';
type MemberStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';
type FilterValue<T extends string> = typeof ALL | T;

type MemberSummary = {
  id: number;
  email: string | null;
  name: string;
  nickname: string | null;
  loginId: string | null;
  phone: string | null;
  provider: AuthProvider;
  role: MemberRole;
  status: MemberStatus;
  completedOrderCount: number;
  createdAt: string | null;
};

type MemberAddress = {
  id: number;
  receiverName: string;
  phone: string;
  postalCode: string;
  address1: string;
  address2: string | null;
  countryCode: string;
  isDefault: boolean;
};

type MemberDetail = MemberSummary & {
  providerId: string | null;
  telephone: string | null;
  mobilePhone: string | null;
  birthDate: string | null;
  gender: string | null;
  memo: string | null;
  depositBalance: number;
  rewardPoint: number;
  joinedDate: string | null;
  alarmConsent: boolean;
  addresses: MemberAddress[];
  updatedAt: string | null;
};

type AdminSession = {
  adminId: number;
  loginId: string;
  name: string;
};

const adminNavItems = [
  { path: '/admin/agency', label: '대행관리', icon: Briefcase },
  { path: '/admin/members', label: '회원관리', icon: UsersRound },
  { path: '/admin/boards', label: '게시판관리', icon: MessageSquareText },
  { path: '/admin/homepage', label: '홈페이지 관리', icon: PanelTop },
  { path: '/admin/coupons', label: '쿠폰관리', icon: TicketPercent },
  { path: '/admin/settings', label: '환경설정', icon: Settings },
] as const;

function App() {
  const [adminSession, setAdminSession] = useState<AdminSession | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  useEffect(() => {
    request<AdminSession>('/api/admin/auth/me')
      .then(setAdminSession)
      .catch(() => setAdminSession(null))
      .finally(() => setIsAuthChecking(false));
  }, []);

  useEffect(() => {
    const handleAuthExpired = () => setAdminSession(null);
    window.addEventListener(ADMIN_AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(ADMIN_AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, []);

  const handleLogout = useCallback(async () => {
    try {
      await request<void>('/api/admin/auth/logout', { method: 'POST' });
    } finally {
      setAdminSession(null);
    }
  }, []);

  return (
    <Routes>
      <Route path="/admin/login" element={<LoginPage adminSession={adminSession} onLogin={setAdminSession} />} />
      <Route
        element={
          <ProtectedAdminRoute adminSession={adminSession} isChecking={isAuthChecking}>
            <Shell adminSession={adminSession} onLogout={handleLogout} />
          </ProtectedAdminRoute>
        }
      >
        <Route index element={<Navigate to="/admin/members" replace />} />
        <Route path="/admin" element={<Navigate to="/admin/members" replace />} />
        <Route path="/admin/members" element={<MemberManagementPage />} />
        <Route path="/admin/agency" element={<PlaceholderPage title="대행관리" />} />
        <Route path="/admin/boards" element={<PlaceholderPage title="게시판관리" />} />
        <Route path="/admin/homepage" element={<PlaceholderPage title="홈페이지 관리" />} />
        <Route path="/admin/coupons" element={<PlaceholderPage title="쿠폰관리" />} />
        <Route path="/admin/settings" element={<PlaceholderPage title="환경설정" />} />
      </Route>
      <Route path="*" element={<Navigate to={adminSession ? '/admin/members' : '/admin/login'} replace />} />
    </Routes>
  );
}

function ProtectedAdminRoute({
  adminSession,
  children,
  isChecking,
}: {
  adminSession: AdminSession | null;
  children: ReactNode;
  isChecking: boolean;
}) {
  if (isChecking) {
    return <div className="admin-auth-loading">확인 중</div>;
  }

  if (!adminSession) {
    return <Navigate to="/admin/login" replace />;
  }

  return children;
}

function LoginPage({
  adminSession,
  onLogin,
}: {
  adminSession: AdminSession | null;
  onLogin: (session: AdminSession) => void;
}) {
  const [loginId, setLoginId] = useState('admin');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (adminSession) {
    return <Navigate to="/admin/members" replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const session = await request<AdminSession>('/api/admin/auth/login', {
        method: 'POST',
        body: JSON.stringify({ loginId, password }),
      });
      onLogin(session);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '로그인에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="admin-login-page">
      <section className="admin-login-panel">
        <div className="admin-login-brand">
          <LockKeyhole size={24} />
          <div>
            <strong>ZIK00 Admin</strong>
            <span>관리자 로그인</span>
          </div>
        </div>

        <form className="admin-login-form" onSubmit={handleSubmit}>
          <label>
            <span>아이디</span>
            <input value={loginId} onChange={(event) => setLoginId(event.target.value)} autoComplete="username" />
          </label>
          <label>
            <span>비밀번호</span>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              autoComplete="current-password"
              autoFocus
            />
          </label>

          {errorMessage && (
            <div className="admin-login-error" role="alert">
              {errorMessage}
            </div>
          )}

          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? '확인 중' : '로그인'}
          </button>
        </form>
      </section>
    </main>
  );
}

function Shell({
  adminSession,
  onLogout,
}: {
  adminSession: AdminSession | null;
  onLogout: () => void;
}) {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand">
          <LayoutDashboard size={22} />
          <div>
            <strong>ZIK00</strong>
            <span>Admin</span>
          </div>
        </div>

        <nav className="admin-nav-list" aria-label="관리자 메뉴">
          {adminNavItems.map((item) => (
            <NavLink key={item.path} to={item.path} className={({ isActive }) => `admin-nav-item ${isActive ? 'is-active' : ''}`}>
              <item.icon size={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="admin-account">
          <div>
            <UserRound size={17} />
            <span>{adminSession?.name || '관리자'}</span>
          </div>
          <button type="button" onClick={onLogout} aria-label="로그아웃">
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}

function PlaceholderPage({ title }: { title: string }) {
  return (
    <section className="admin-page">
      <PageHeader title={title} eyebrow="관리자" />
      <div className="admin-empty-panel">준비 중</div>
    </section>
  );
}

function MemberManagementPage() {
  const [members, setMembers] = useState<MemberSummary[]>([]);
  const [selectedMemberId, setSelectedMemberId] = useState<number | null>(null);
  const [memberDetail, setMemberDetail] = useState<MemberDetail | null>(null);
  const [query, setQuery] = useState('');
  const [providerFilter, setProviderFilter] = useState<FilterValue<AuthProvider>>(ALL);
  const [roleFilter, setRoleFilter] = useState<FilterValue<MemberRole>>(ALL);
  const [statusFilter, setStatusFilter] = useState<FilterValue<MemberStatus>>(ALL);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadMembers = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    setSelectedMemberId(null);
    setMemberDetail(null);

    try {
      const data = await request<MemberSummary[]>('/api/admin/members');
      setMembers(data);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '회원 목록을 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loadMemberDetail = useCallback(async (memberId: number) => {
    setSelectedMemberId(memberId);
    setIsDetailLoading(true);
    setErrorMessage(null);

    try {
      const data = await request<MemberDetail>(`/api/admin/members/${memberId}`);
      setMemberDetail(data);
    } catch (error) {
      setMemberDetail(null);
      setErrorMessage(error instanceof Error ? error.message : '회원 상세를 불러오지 못했습니다.');
    } finally {
      setIsDetailLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadMembers();
  }, [loadMembers]);

  const filteredMembers = useMemo(() => {
    const normalizedQuery = normalize(query);

    return members.filter((member) => {
      const searchableText = [
        member.name,
        member.nickname,
        member.loginId,
        member.email,
        member.phone,
        member.provider,
        member.role,
        member.status,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return (
        (!normalizedQuery || searchableText.includes(normalizedQuery)) &&
        (providerFilter === ALL || member.provider === providerFilter) &&
        (roleFilter === ALL || member.role === roleFilter) &&
        (statusFilter === ALL || member.status === statusFilter)
      );
    });
  }, [members, providerFilter, query, roleFilter, statusFilter]);

  return (
    <section className="admin-page">
      <PageHeader
        title="회원관리"
        eyebrow="관리자"
        actions={
          <button className="admin-icon-button" type="button" onClick={loadMembers} aria-label="새로고침">
            <RefreshCw size={18} />
          </button>
        }
      />

      {errorMessage && (
        <div className="admin-alert-line" role="alert">
          <CircleAlert size={18} />
          <span>{errorMessage}</span>
        </div>
      )}

      <div className="admin-member-layout">
        <div className="admin-member-list-panel">
          <div className="admin-tool-row">
            <label className="admin-search-box">
              <Search size={17} />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="회원 검색"
                aria-label="회원 검색"
              />
            </label>

            <div className="admin-filter-group" aria-label="회원 필터">
              <SlidersHorizontal size={17} />
              <select value={providerFilter} onChange={(event) => setProviderFilter(event.target.value as FilterValue<AuthProvider>)}>
                <option value={ALL}>가입경로 전체</option>
                <option value="LOCAL">LOCAL</option>
                <option value="KAKAO">KAKAO</option>
                <option value="LINE">LINE</option>
              </select>
              <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value as FilterValue<MemberRole>)}>
                <option value={ALL}>권한 전체</option>
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as FilterValue<MemberStatus>)}>
                <option value={ALL}>상태 전체</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="SUSPENDED">SUSPENDED</option>
                <option value="WITHDRAWN">WITHDRAWN</option>
              </select>
            </div>
          </div>

          <div className="admin-table-wrap">
            <table className="admin-member-table">
              <thead>
                <tr>
                  <th>회원</th>
                  <th>아이디</th>
                  <th>연락처</th>
                  <th>주문</th>
                  <th>가입일</th>
                </tr>
              </thead>
              <tbody>
                {isLoading && (
                  <tr>
                    <td colSpan={5} className="admin-table-state">
                      불러오는 중
                    </td>
                  </tr>
                )}

                {!isLoading &&
                  filteredMembers.map((member) => (
                    <tr
                      key={member.id}
                      className={selectedMemberId === member.id ? 'is-selected' : ''}
                      onClick={() => void loadMemberDetail(member.id)}
                    >
                      <td>
                        <strong>{member.name || '-'}</strong>
                        <span>{member.email || '-'}</span>
                      </td>
                      <td>{member.loginId || '-'}</td>
                      <td>{member.phone || '-'}</td>
                      <td>{member.completedOrderCount.toLocaleString()}</td>
                      <td>{formatDate(member.createdAt)}</td>
                    </tr>
                  ))}

                {!isLoading && filteredMembers.length === 0 && (
                  <tr>
                    <td colSpan={5} className="admin-table-state">
                      검색 결과 없음
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <MemberDetailPanel member={memberDetail} isLoading={isDetailLoading} />
      </div>
    </section>
  );
}

function MemberDetailPanel({ member, isLoading }: { member: MemberDetail | null; isLoading: boolean }) {
  if (isLoading) {
    return <aside className="admin-detail-panel">불러오는 중</aside>;
  }

  if (!member) {
    return <aside className="admin-detail-panel is-muted">회원을 선택해주세요.</aside>;
  }

  return (
    <aside className="admin-detail-panel">
      <div className="admin-detail-head">
        <div>
          <span className="admin-eyebrow">회원 상세</span>
          <h2>{member.name || '-'}</h2>
        </div>
        <span className="admin-status-pill">{member.status}</span>
      </div>

      <div className="admin-detail-grid">
        <Field label="이메일" value={member.email} />
        <Field label="아이디" value={member.loginId} />
        <Field label="닉네임" value={member.nickname} />
        <Field label="가입경로" value={member.provider} />
        <Field label="휴대폰" value={member.mobilePhone} />
        <Field label="전화번호" value={member.telephone} />
        <Field label="생년월일" value={member.birthDate} />
        <Field label="성별" value={member.gender} />
        <Field label="예치금" value={`${member.depositBalance.toLocaleString()}원`} />
        <Field label="포인트" value={`${member.rewardPoint.toLocaleString()}P`} />
        <Field label="완료주문" value={`${member.completedOrderCount.toLocaleString()}건`} />
        <Field label="가입일" value={formatDate(member.joinedDate)} />
        <Field label="알림동의" value={member.alarmConsent ? '동의' : '미동의'} />
        <Field label="메모" value={member.memo} wide />
      </div>

      <section className="admin-address-section">
        <h3>
          <MapPin size={17} />
          배송지
        </h3>
        {member.addresses.length === 0 && <p className="admin-empty-text">등록된 배송지 없음</p>}
        {member.addresses.map((address) => (
          <div className="admin-address-item" key={address.id}>
            <strong>
              {address.receiverName || '-'} {address.isDefault && <span>기본</span>}
            </strong>
            <p>{address.phone || '-'}</p>
            <p>
              {address.postalCode || '-'} · {address.address1 || '-'}
            </p>
          </div>
        ))}
      </section>
    </aside>
  );
}

function PageHeader({ title, eyebrow, actions }: { title: string; eyebrow: string; actions?: ReactNode }) {
  return (
    <header className="admin-page-header">
      <div>
        <span className="admin-eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
      </div>
      {actions && <div className="admin-header-actions">{actions}</div>}
    </header>
  );
}

function Field({ label, value, wide = false }: { label: string; value: ReactNode; wide?: boolean }) {
  return (
    <div className={wide ? 'admin-field is-wide' : 'admin-field'}>
      <span>{label}</span>
      <strong>{value || '-'}</strong>
    </div>
  );
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: 'include',
    headers,
  });

  if (!response.ok) {
    if (response.status === 401) {
      window.dispatchEvent(new Event(ADMIN_AUTH_EXPIRED_EVENT));
    }
    throw new Error(await errorMessage(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

async function errorMessage(response: Response) {
  try {
    const body = (await response.json()) as { message?: string; error?: string };
    return body.message || body.error || `요청 실패 (${response.status})`;
  } catch {
    return `요청 실패 (${response.status})`;
  }
}

function normalize(value: string) {
  return value.trim().toLowerCase();
}

function formatDate(value: string | null) {
  if (!value) {
    return '-';
  }

  return value.slice(0, 10);
}

export default App;
