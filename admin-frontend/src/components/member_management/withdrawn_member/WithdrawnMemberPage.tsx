import { Search } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { memberManagementApi, type WithdrawnMember } from '../../../api/memberManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { MemberPageHeader } from '../MemberManagementNav';

export function WithdrawnMemberPage() {
  const [items, setItems] = useState<WithdrawnMember[]>([]); const [query, setQuery] = useState(''); const [loading, setLoading] = useState(true); const [message, setMessage] = useState('');
  const load = useCallback(async () => { setLoading(true); setMessage(''); try { setItems(await memberManagementApi.withdrawnMembers()); } catch (e) { setMessage(e instanceof Error ? e.message : '탈퇴회원 목록을 불러오지 못했습니다.'); } finally { setLoading(false); } }, []);
  useEffect(() => { void load(); }, [load]); useAdminPageRefresh(load);
  const filtered = useMemo(() => items.filter((item) => `${item.name} ${item.nickname ?? ''} ${item.loginId ?? ''} ${item.email ?? ''}`.toLowerCase().includes(query.trim().toLowerCase())), [items, query]);
  return <section className="admin-page"><MemberPageHeader title="탈퇴회원" description="탈퇴 처리된 회원과 탈퇴 시점을 별도로 확인합니다."/>
    <div className="member-toolbar"><label className="admin-search-box"><Search size={17}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="탈퇴회원 검색"/></label><span>총 {filtered.length.toLocaleString()}명</span></div>
    {message && <p className="member-message error">{message}</p>}
    <div className="member-table-panel"><table className="member-data-table"><thead><tr><th>회원</th><th>아이디</th><th>이메일</th><th>가입일</th><th>탈퇴일</th><th>회원 메모</th></tr></thead><tbody>
      {loading && <tr><td colSpan={6} className="member-empty">불러오는 중</td></tr>}{!loading && filtered.map((item)=><tr key={item.id}><td><strong>{item.name || '-'}</strong><small>{item.nickname || '-'}</small></td><td>{item.loginId || '-'}</td><td>{item.email || '-'}</td><td>{date(item.joinedDate)}</td><td>{dateTime(item.withdrawnAt)}</td><td>{item.memo || '-'}</td></tr>)}
      {!loading && filtered.length === 0 && <tr><td colSpan={6} className="member-empty">탈퇴회원이 없습니다.</td></tr>}
    </tbody></table></div></section>;
}
function date(value: string | null) { return value ? new Date(value).toLocaleDateString('ko-KR') : '-'; }
function dateTime(value: string | null) { return value ? new Date(value).toLocaleString('ko-KR') : '-'; }
