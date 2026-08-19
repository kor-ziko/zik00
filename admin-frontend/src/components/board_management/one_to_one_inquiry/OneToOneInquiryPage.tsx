import { ImagePlus, Search, Send, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { boardApi, type InquiryDetail, type InquirySummary } from '../../../api/boardManagement';
import { BoardPageHeader } from '../BoardManagementNav';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';

export function OneToOneInquiryPage() {
  const [items, setItems] = useState<InquirySummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [reply, setReply] = useState('');
  const [images, setImages] = useState<File[]>([]);
  const [message, setMessage] = useState('');
  const filtered = useMemo(() => items.filter((item) => (status === 'ALL' || (status === 'ANSWERED') === item.answered) && `${item.memberName} ${item.title}`.toLowerCase().includes(query.toLowerCase())), [items, query, status]);

  useEffect(() => { void load(); }, []);
  useAdminPageRefresh(load);
  async function load() { try { const data = await boardApi.inquiries(); setItems(data); setSelectedId((current) => data.some((item) => item.inquiryId === current) ? current : data[0]?.inquiryId ?? null); } catch (e) { showError(e); } }
  useEffect(() => { if (selectedId) boardApi.inquiry(selectedId).then(setDetail).catch(showError); else setDetail(null); }, [selectedId]);
  function showError(error: unknown) { setMessage(error instanceof Error ? error.message : '요청을 처리하지 못했습니다.'); }
  async function sendReply() { if (!detail || !reply.trim()) return; try { const updated = await boardApi.replyInquiry(detail.inquiryId, reply, images); setDetail(updated); setReply(''); setImages([]); setItems((all) => all.map((item) => item.inquiryId === updated.inquiryId ? { ...item, answered: true, commentCount: updated.comments.length } : item)); } catch (e) { showError(e); } }

  return (
    <section className="admin-page">
      <BoardPageHeader title="1:1문의 관리" description="회원 문의 내용과 첨부 이미지를 확인하고 답변합니다." />
      <div className="board-workspace inquiry-workspace">
        <aside className="board-list-panel">
          <div className="board-search"><Search size={16} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="회원 또는 제목 검색" /></div>
          <select className="board-status-filter" value={status} onChange={(e) => setStatus(e.target.value)}><option value="ALL">전체 문의</option><option value="PENDING">답변 대기</option><option value="ANSWERED">답변 완료</option></select>
          <div className="board-list">{filtered.map((item) => <button key={item.inquiryId} className={selectedId === item.inquiryId ? 'active' : ''} onClick={() => setSelectedId(item.inquiryId)}><span>{item.answered ? '답변 완료' : '답변 대기'} · {item.memberName}</span><strong>{item.title}</strong><small>{item.createdAt} · 댓글 {item.commentCount}</small></button>)}{filtered.length === 0 && <p className="board-empty">문의가 없습니다.</p>}</div>
        </aside>
        {detail ? <div className="board-editor inquiry-detail">
          <header><div><span className={`inquiry-status ${detail.answered ? 'done' : ''}`}>{detail.answered ? '답변 완료' : '답변 대기'}</span><h2>{detail.title}</h2><p>{detail.memberName} · {detail.memberEmail ?? '이메일 없음'} · {detail.createdAt}</p></div></header>
          <article className="inquiry-body"><p>{detail.content}</p><ImageList images={detail.images} /></article>
          <div className="inquiry-comments">{detail.comments.map((item) => <article key={item.commentId} className={item.writerType === 'ADMIN' ? 'admin-reply' : ''}><div><strong>{item.writerName}</strong><time>{item.createdAt}</time></div><p>{item.content}</p><ImageList images={item.images} /></article>)}</div>
          <section className="inquiry-reply"><h3>관리자 답변</h3><textarea rows={5} maxLength={2000} value={reply} onChange={(e) => setReply(e.target.value)} placeholder="답변을 입력하세요" /><div className="inquiry-file-row"><label className="admin-secondary-button"><ImagePlus size={16} />이미지 첨부<input type="file" accept="image/*" multiple hidden onChange={(e) => setImages([...images, ...Array.from(e.target.files ?? [])].slice(0, 5))} /></label>{images.map((file, index) => <span key={`${file.name}-${index}`}>{file.name}<button title="첨부 삭제" onClick={() => setImages(images.filter((_, i) => i !== index))}><X size={13} /></button></span>)}</div><button className="admin-primary-button" disabled={!reply.trim()} onClick={sendReply}><Send size={16} />답변 등록</button></section>
          {message && <p className="board-message">{message}</p>}
        </div> : <div className="board-empty-detail">확인할 문의를 선택하세요.</div>}
      </div>
    </section>
  );
}

function ImageList({ images }: { images: { imageUuid: string; imageUrl: string }[] }) { return images.length ? <div className="inquiry-images">{images.map((image) => <a key={image.imageUuid} href={image.imageUrl} target="_blank" rel="noreferrer"><img src={image.imageUrl} alt="문의 첨부" /></a>)}</div> : null; }
