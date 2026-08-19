import { Coins, Search } from 'lucide-react';
import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { memberManagementApi, type RewardPointHistory, type RewardPointMember } from '../../../api/memberManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { MemberPageHeader } from '../MemberManagementNav';

export function RewardPointPage() {
  const [members, setMembers] = useState<RewardPointMember[]>([]); const [histories, setHistories] = useState<RewardPointHistory[]>([]);
  const [memberId, setMemberId] = useState(''); const [amount, setAmount] = useState(''); const [reason, setReason] = useState(''); const [query, setQuery] = useState('');
  const [message, setMessage] = useState(''); const [saving, setSaving] = useState(false);
  const load = useCallback(async () => { setMessage(''); try { const [memberData, historyData] = await Promise.all([memberManagementApi.pointMembers(), memberManagementApi.pointHistories()]); setMembers(memberData); setHistories(historyData); } catch (e) { setMessage(errorOf(e)); } }, []);
  useEffect(() => { void load(); }, [load]); useAdminPageRefresh(load);
  const filtered = useMemo(() => histories.filter((item) => `${item.memberName} ${item.loginId} ${item.reason}`.toLowerCase().includes(query.toLowerCase())), [histories, query]);
  async function submit(event: FormEvent) { event.preventDefault(); const value = Number(amount); if (!memberId || !Number.isInteger(value) || value === 0 || !reason.trim()) { setMessage('회원, 증감 포인트, 사유를 모두 입력해주세요.'); return; } setSaving(true); setMessage(''); try { await memberManagementApi.adjustPoint(Number(memberId), value, reason); setAmount(''); setReason(''); setMessage('포인트를 반영했습니다.'); await load(); } catch (e) { setMessage(errorOf(e)); } finally { setSaving(false); } }
  return <section className="admin-page"><MemberPageHeader title="적립포인트" description="회원별 포인트를 지급하거나 차감하고 모든 변동 이력을 확인합니다."/>
    <div className="member-split-layout"><form className="member-action-panel" onSubmit={submit}><div className="member-panel-title"><Coins size={19}/><h2>포인트 반영</h2></div>
      <label>회원<select value={memberId} onChange={(e)=>setMemberId(e.target.value)} required><option value="" disabled>회원을 선택하세요</option>{members.map((item)=><option key={item.memberId} value={item.memberId}>{item.name} ({item.loginId}) · {item.balance.toLocaleString()}P</option>)}</select></label>
      <label>증감 포인트<input type="number" value={amount} onChange={(e)=>setAmount(e.target.value)} placeholder="지급은 양수, 차감은 음수"/></label>
      <label>처리 사유<textarea rows={4} value={reason} onChange={(e)=>setReason(e.target.value)} placeholder="포인트 지급 또는 차감 사유"/></label>
      <button className="member-primary-button" disabled={saving}>{saving ? '반영 중' : '포인트 반영'}</button>{message && <p className="member-message">{message}</p>}
    </form><div className="member-history-panel"><div className="member-toolbar compact"><label className="admin-search-box"><Search size={16}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="회원 또는 사유 검색"/></label><span>{filtered.length}건</span></div>
      <div className="member-table-panel"><table className="member-data-table"><thead><tr><th>처리일</th><th>회원</th><th>변동</th><th>잔액</th><th>사유</th></tr></thead><tbody>{filtered.map((item)=><tr key={item.id}><td>{dateTime(item.createdAt)}</td><td><strong>{item.memberName}</strong><small>{item.loginId}</small></td><td className={item.amount >= 0 ? 'amount-plus' : 'amount-minus'}>{signed(item.amount)}P</td><td>{item.balanceAfter.toLocaleString()}P</td><td>{item.reason}</td></tr>)}{filtered.length===0&&<tr><td colSpan={5} className="member-empty">포인트 이력이 없습니다.</td></tr>}</tbody></table></div>
    </div></div></section>;
}
function signed(value:number){return `${value>0?'+':''}${value.toLocaleString()}`;} function dateTime(value:string){return new Date(value).toLocaleString('ko-KR');} function errorOf(e:unknown){return e instanceof Error?e.message:'요청을 처리하지 못했습니다.';}
