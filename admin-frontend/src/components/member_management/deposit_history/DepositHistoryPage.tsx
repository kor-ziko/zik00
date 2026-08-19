import { Search } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { memberManagementApi, type DepositHistory } from '../../../api/memberManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { MemberPageHeader } from '../MemberManagementNav';

export function DepositHistoryPage(){const [items,setItems]=useState<DepositHistory[]>([]);const [query,setQuery]=useState('');const [type,setType]=useState('ALL');const [message,setMessage]=useState('');
  const load=useCallback(async()=>{setMessage('');try{setItems(await memberManagementApi.depositHistories());}catch(e){setMessage(e instanceof Error?e.message:'예치금 내역을 불러오지 못했습니다.');}},[]);useEffect(()=>{void load();},[load]);useAdminPageRefresh(load);
  const filtered=useMemo(()=>items.filter((item)=>(type==='ALL'||item.transactionType===type)&&`${item.memberName} ${item.loginId} ${item.description}`.toLowerCase().includes(query.toLowerCase())),[items,query,type]);
  return <section className="admin-page"><MemberPageHeader title="예치금사용내역" description="회원별 예치금 충전과 사용 흐름, 처리 후 잔액을 확인합니다."/><div className="member-toolbar"><label className="admin-search-box"><Search size={17}/><input value={query} onChange={(e)=>setQuery(e.target.value)} placeholder="회원 또는 내용 검색"/></label><select value={type} onChange={(e)=>setType(e.target.value)}><option value="ALL">전체 구분</option><option value="CHARGE">충전</option><option value="USE">사용</option><option value="REFUND">환불</option><option value="ADJUST">조정</option></select></div>{message&&<p className="member-message error">{message}</p>}
    <div className="member-table-panel"><table className="member-data-table"><thead><tr><th>처리일</th><th>회원</th><th>구분</th><th>변동금액</th><th>처리 후 잔액</th><th>내용</th></tr></thead><tbody>{filtered.map((item)=><tr key={item.id}><td>{new Date(item.createdAt).toLocaleString('ko-KR')}</td><td><strong>{item.memberName}</strong><small>{item.loginId}</small></td><td><span className={`member-status ${item.transactionType.toLowerCase()}`}>{typeName(item.transactionType)}</span></td><td className={item.amount>=0?'amount-plus':'amount-minus'}>{item.amount>0?'+':''}{item.amount.toLocaleString()}원</td><td>{item.balanceAfter.toLocaleString()}원</td><td>{item.description}</td></tr>)}{filtered.length===0&&<tr><td colSpan={6} className="member-empty">예치금 내역이 없습니다.</td></tr>}</tbody></table></div></section>;
}
function typeName(value:string){return ({CHARGE:'충전',USE:'사용',REFUND:'환불',ADJUST:'조정'} as Record<string,string>)[value]??value;}
