import {
  Briefcase,
  CircleAlert,
  ImagePlus,
  Inbox,
  LayoutDashboard,
  LockKeyhole,
  LogOut,
  MapPin,
  MessageSquareText,
  PanelTop,
  RefreshCw,
  Search,
  Send,
  Settings,
  SlidersHorizontal,
  TicketPercent,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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

type InquiryStatusFilter = typeof ALL | 'PENDING' | 'ANSWERED';

type InquirySummary = {
  inquiryId: number;
  memberId: number;
  memberName: string;
  title: string;
  answered: boolean;
  createdAt: string;
  commentCount: number;
  imageCount: number;
};

type InquiryImage = {
  imageUuid: string;
  imageUrl: string;
};

type InquiryComment = {
  commentId: number;
  writerType: 'USER' | 'ADMIN';
  writerName: string;
  content: string;
  createdAt: string;
  images: InquiryImage[];
};

type InquiryDetail = {
  inquiryId: number;
  memberId: number;
  memberName: string;
  memberNickname: string | null;
  memberEmail: string | null;
  title: string;
  content: string;
  answered: boolean;
  createdAt: string;
  images: InquiryImage[];
  comments: InquiryComment[];
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
        <Route path="/admin/boards" element={<InquiryManagementPage />} />
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

function InquiryManagementPage() {
  const [inquiries, setInquiries] = useState<InquirySummary[]>([]);
  const [selectedInquiryId, setSelectedInquiryId] = useState<number | null>(null);
  const [inquiryDetail, setInquiryDetail] = useState<InquiryDetail | null>(null);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<InquiryStatusFilter>(ALL);
  const [replyContent, setReplyContent] = useState('');
  const [replyImages, setReplyImages] = useState<File[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [isReplying, setIsReplying] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const imagePreviews = useMemo(
    () => replyImages.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [replyImages],
  );

  useEffect(() => {
    return () => imagePreviews.forEach((preview) => URL.revokeObjectURL(preview.url));
  }, [imagePreviews]);

  const loadInquiryDetail = useCallback(async (inquiryId: number) => {
    setSelectedInquiryId(inquiryId);
    setIsDetailLoading(true);
    setErrorMessage(null);
    setReplyContent('');
    setReplyImages([]);

    try {
      setInquiryDetail(await request<InquiryDetail>(`/api/admin/inquiries/${inquiryId}`));
    } catch (error) {
      setInquiryDetail(null);
      setErrorMessage(error instanceof Error ? error.message : '문의 상세를 불러오지 못했습니다.');
    } finally {
      setIsDetailLoading(false);
    }
  }, []);

  const loadInquiries = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const data = await request<InquirySummary[]>('/api/admin/inquiries');
      setInquiries(data);
      return data;
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '문의 목록을 불러오지 못했습니다.');
      return [];
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadInquiries();
  }, [loadInquiries]);

  const filteredInquiries = useMemo(() => {
    const normalizedQuery = normalize(query);
    return inquiries.filter((inquiry) => {
      const matchesQuery = !normalizedQuery || `${inquiry.title} ${inquiry.memberName}`.toLowerCase().includes(normalizedQuery);
      const matchesStatus =
        statusFilter === ALL ||
        (statusFilter === 'ANSWERED' && inquiry.answered) ||
        (statusFilter === 'PENDING' && !inquiry.answered);
      return matchesQuery && matchesStatus;
    });
  }, [inquiries, query, statusFilter]);

  const handleRefresh = async () => {
    const data = await loadInquiries();
    if (selectedInquiryId && data.some((inquiry) => inquiry.inquiryId === selectedInquiryId)) {
      await loadInquiryDetail(selectedInquiryId);
    }
  };

  const handleImageSelection = (files: FileList | null) => {
    if (!files) {
      return;
    }
    const nextImages = [...replyImages, ...Array.from(files)];
    if (nextImages.length > 3) {
      setErrorMessage('답변 사진은 최대 3장까지 첨부할 수 있습니다.');
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
      return;
    }
    setReplyImages(nextImages);
    setErrorMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleReply = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedInquiryId || !replyContent.trim()) {
      setErrorMessage('답변 내용을 입력해주세요.');
      return;
    }

    const body = new FormData();
    body.append('content', replyContent.trim());
    replyImages.forEach((image) => body.append('images', image));

    setIsReplying(true);
    setErrorMessage(null);
    try {
      const detail = await request<InquiryDetail>(`/api/admin/inquiries/${selectedInquiryId}/replies`, {
        method: 'POST',
        body,
      });
      setInquiryDetail(detail);
      setReplyContent('');
      setReplyImages([]);
      await loadInquiries();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '답변을 등록하지 못했습니다.');
    } finally {
      setIsReplying(false);
    }
  };

  return (
    <section className="admin-page">
      <PageHeader
        title="1:1 문의 관리"
        eyebrow="게시판관리"
        actions={
          <button className="admin-icon-button" type="button" onClick={() => void handleRefresh()} aria-label="새로고침" title="새로고침">
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

      <div className="admin-inquiry-layout">
        <section className="admin-inquiry-list-panel">
          <div className="admin-inquiry-tools">
            <label className="admin-search-box">
              <Search size={17} />
              <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="제목 또는 회원명 검색" />
            </label>
            <div className="admin-status-segments" aria-label="문의 상태 필터">
              {([
                [ALL, '전체'],
                ['PENDING', '답변대기'],
                ['ANSWERED', '답변완료'],
              ] as const).map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  className={statusFilter === value ? 'is-active' : ''}
                  onClick={() => setStatusFilter(value)}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="admin-inquiry-list">
            {isLoading && <p className="admin-inquiry-state">불러오는 중</p>}
            {!isLoading && filteredInquiries.length === 0 && (
              <div className="admin-inquiry-state">
                <Inbox size={22} />
                <span>문의 없음</span>
              </div>
            )}
            {!isLoading &&
              filteredInquiries.map((inquiry) => (
                <button
                  type="button"
                  key={inquiry.inquiryId}
                  className={`admin-inquiry-row ${selectedInquiryId === inquiry.inquiryId ? 'is-selected' : ''}`}
                  onClick={() => void loadInquiryDetail(inquiry.inquiryId)}
                >
                  <div>
                    <strong>{inquiry.title}</strong>
                    <span>{inquiry.memberName}</span>
                  </div>
                  <div className="admin-inquiry-row-meta">
                    <span className={`admin-inquiry-status ${inquiry.answered ? 'is-answered' : 'is-pending'}`}>
                      {inquiry.answered ? '답변완료' : '답변대기'}
                    </span>
                    <time>{inquiry.createdAt}</time>
                    <span>댓글 {inquiry.commentCount} · 사진 {inquiry.imageCount}</span>
                  </div>
                </button>
              ))}
          </div>
        </section>

        <section className="admin-inquiry-detail-panel">
          {isDetailLoading && <div className="admin-inquiry-detail-state">불러오는 중</div>}
          {!isDetailLoading && !inquiryDetail && (
            <div className="admin-inquiry-detail-state">
              <MessageSquareText size={24} />
              <span>처리할 문의를 선택해주세요.</span>
            </div>
          )}
          {!isDetailLoading && inquiryDetail && (
            <>
              <header className="admin-inquiry-detail-head">
                <div>
                  <span className="admin-eyebrow">문의 #{inquiryDetail.inquiryId}</span>
                  <h2>{inquiryDetail.title}</h2>
                  <p>
                    {inquiryDetail.memberName}
                    {inquiryDetail.memberNickname ? ` (${inquiryDetail.memberNickname})` : ''}
                    {inquiryDetail.memberEmail ? ` · ${inquiryDetail.memberEmail}` : ''}
                  </p>
                </div>
                <span className={`admin-inquiry-status ${inquiryDetail.answered ? 'is-answered' : 'is-pending'}`}>
                  {inquiryDetail.answered ? '답변완료' : '답변대기'}
                </span>
              </header>

              <article className="admin-inquiry-original">
                <div>
                  <strong>사용자 문의</strong>
                  <time>{inquiryDetail.createdAt}</time>
                </div>
                <p>{inquiryDetail.content}</p>
                {inquiryDetail.images.length > 0 && <InquiryImageGrid images={inquiryDetail.images} />}
              </article>

              <div className="admin-inquiry-conversation">
                {inquiryDetail.comments.length === 0 && <p className="admin-empty-text">등록된 댓글이 없습니다.</p>}
                {inquiryDetail.comments.map((comment) => (
                  <article
                    className={`admin-inquiry-comment ${comment.writerType === 'ADMIN' ? 'is-admin' : 'is-user'}`}
                    key={comment.commentId}
                  >
                    <div>
                      <strong>{comment.writerName}</strong>
                      <span>{comment.writerType === 'ADMIN' ? '관리자 답변' : '사용자'}</span>
                      <time>{comment.createdAt}</time>
                    </div>
                    <p>{comment.content}</p>
                    {comment.images.length > 0 && <InquiryImageGrid images={comment.images} />}
                  </article>
                ))}
              </div>

              <form className="admin-inquiry-reply" onSubmit={handleReply}>
                <label>
                  <span>관리자 답변</span>
                  <textarea
                    value={replyContent}
                    onChange={(event) => setReplyContent(event.target.value)}
                    placeholder="답변 내용을 입력하세요."
                    maxLength={2000}
                    rows={5}
                    required
                  />
                </label>

                {imagePreviews.length > 0 && (
                  <div className="admin-reply-previews">
                    {imagePreviews.map((preview, index) => (
                      <div key={`${preview.file.name}-${index}`}>
                        <img src={preview.url} alt={preview.file.name} />
                        <button
                          type="button"
                          onClick={() => setReplyImages((images) => images.filter((_, imageIndex) => imageIndex !== index))}
                          aria-label={`${preview.file.name} 삭제`}
                          title="첨부 삭제"
                        >
                          <X size={15} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}

                <div className="admin-reply-actions">
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/png,image/jpeg,image/gif,image/webp"
                    multiple
                    hidden
                    onChange={(event) => handleImageSelection(event.target.files)}
                  />
                  <button
                    className="admin-attach-button"
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    disabled={replyImages.length >= 3}
                    title="사진 첨부"
                  >
                    <ImagePlus size={17} />
                    사진 {replyImages.length}/3
                  </button>
                  <button className="admin-reply-button" type="submit" disabled={isReplying}>
                    <Send size={17} />
                    {isReplying ? '등록 중' : '답변 등록'}
                  </button>
                </div>
              </form>
            </>
          )}
        </section>
      </div>
    </section>
  );
}

function InquiryImageGrid({ images }: { images: InquiryImage[] }) {
  return (
    <div className="admin-inquiry-images">
      {images.map((image) => (
        <a key={image.imageUuid} href={image.imageUrl} target="_blank" rel="noreferrer">
          <img src={image.imageUrl} alt="문의 첨부사진" />
        </a>
      ))}
    </div>
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
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
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
    const body = (await response.json()) as { message?: string; error?: string; detail?: string };
    return body.message || body.detail || body.error || `요청 실패 (${response.status})`;
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
