import { Search, UserX, X } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { memberManagementApi, type MemberListItem } from '../../../api/memberManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { MemberPageHeader } from '../MemberManagementNav';

export function MemberListPage() {
  const [items, setItems] = useState<MemberListItem[]>([]);
  const [query, setQuery] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [withdrawing, setWithdrawing] = useState<MemberListItem | null>(null);
  const [confirmation, setConfirmation] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const load = useCallback(async () => { setLoading(true); setMessage(''); try { setItems(await memberManagementApi.members()); } catch (e) { setMessage(errorOf(e)); } finally { setLoading(false); } }, []);
  useEffect(() => { void load(); }, [load]);
  useAdminPageRefresh(load);
  const filtered = useMemo(() => items.filter((item) => `${item.name} ${item.nickname ?? ''} ${item.loginId ?? ''} ${item.email ?? ''} ${item.phone ?? ''}`.toLowerCase().includes(query.trim().toLowerCase())), [items, query]);
  const closeWithdrawal = () => { if (!submitting) { setWithdrawing(null); setConfirmation(''); } };
  const withdraw = async () => {
    if (!withdrawing || confirmation !== '회원탈퇴') return;
    setSubmitting(true); setMessage('');
    try {
      await memberManagementApi.withdrawMember(withdrawing.id);
      setItems((current) => current.filter((item) => item.id !== withdrawing.id));
      setWithdrawing(null); setConfirmation('');
    } catch (error) { setMessage(errorOf(error)); }
    finally { setSubmitting(false); }
  };
  return <section className="admin-page">
    <MemberPageHeader title="회원리스트" description="가입 회원의 기본 정보와 주문·포인트·예치금 현황을 확인합니다." />
    <div className="member-toolbar"><label className="admin-search-box"><Search size={17}/><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="아이디, 이름, 닉네임으로 검색"/></label><span>총 {filtered.length.toLocaleString()}명</span></div>
    {message && <p className="member-message error">{message}</p>}
    <div className="member-table-panel"><table className="member-data-table"><thead><tr><th>회원</th><th>아이디</th><th>연락처</th><th>가입일</th><th>주문</th><th>적립포인트</th><th>예치금</th><th>상태</th><th>관리</th></tr></thead><tbody>
      {loading && <tr><td colSpan={9} className="member-empty">불러오는 중</td></tr>}
      {!loading && filtered.map((item) => <tr key={item.id}><td><strong>{item.name || '-'}</strong><small>{item.nickname || item.email || '-'}</small></td><td>{item.loginId || '-'}</td><td>{item.phone || '-'}</td><td>{date(item.joinedDate)}</td><td>{item.completedOrderCount.toLocaleString()}건</td><td>{item.rewardPoint.toLocaleString()}P</td><td>{item.depositBalance.toLocaleString()}원</td><td><span className={`member-status ${item.status.toLowerCase()}`}>{status(item.status)}</span></td><td><button className="member-withdraw-button" type="button" onClick={() => { setWithdrawing(item); setConfirmation(''); }}><UserX size={14}/>탈퇴 처리</button></td></tr>)}
      {!loading && filtered.length === 0 && <tr><td colSpan={9} className="member-empty">검색 결과가 없습니다.</td></tr>}
    </tbody></table></div>
    {withdrawing && <div className="admin-member-modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) closeWithdrawal(); }}>
      <div className="admin-member-modal" role="dialog" aria-modal="true" aria-labelledby="member-withdraw-title">
        <button className="admin-member-modal-close" type="button" aria-label="닫기" onClick={closeWithdrawal}><X size={19}/></button>
        <UserX size={27}/><h2 id="member-withdraw-title">회원을 탈퇴 처리할까요?</h2>
        <p><strong>{withdrawing.name || withdrawing.loginId || '선택한 회원'}</strong>님의 모든 로그인 세션이 종료되고 탈퇴회원 목록으로 이동합니다.</p>
        <label><span>계속하려면 <b>회원탈퇴</b>를 입력해주세요.</span><input autoFocus value={confirmation} onChange={(event) => setConfirmation(event.target.value)} placeholder="회원탈퇴"/></label>
        <div><button type="button" onClick={closeWithdrawal}>취소</button><button type="button" disabled={confirmation !== '회원탈퇴' || submitting} onClick={() => void withdraw()}>{submitting ? '처리 중' : '탈퇴 처리'}</button></div>
      </div>
    </div>}
  </section>;
}

function date(value: string | null) { return value ? new Date(value).toLocaleDateString('ko-KR') : '-'; }
function status(value: string) { return value === 'ACTIVE' ? '정상' : value === 'SUSPENDED' ? '정지' : '탈퇴'; }
function errorOf(error: unknown) { return error instanceof Error ? error.message : '회원 정보를 불러오지 못했습니다.'; }
