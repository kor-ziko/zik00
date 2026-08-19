import { MessageSquarePlus, Save, Search, Star, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { boardApi, type AdminReview } from '../../../api/boardManagement';
import { BoardPageHeader } from '../BoardManagementNav';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';

export function ReviewManagementPage() {
  const [items, setItems] = useState<AdminReview[]>([]);
  const [selected, setSelected] = useState<AdminReview | null>(null);
  const [query, setQuery] = useState('');
  const [comment, setComment] = useState('');
  const [message, setMessage] = useState('');
  const [uploadingImage, setUploadingImage] = useState(false);
  const filtered = useMemo(() => items.filter((item) => `${item.authorName} ${item.title} ${item.productName}`.toLowerCase().includes(query.toLowerCase())), [items, query]);

  useEffect(() => { void load(); }, []);
  useAdminPageRefresh(load);
  async function load() { try { const data = await boardApi.reviews(); setItems(data); setSelected((current) => data.find((item) => item.id === current?.id) ?? data[0] ?? null); } catch (e) { showError(e); } }
  function replace(updated: AdminReview) { setItems((all) => all.map((item) => item.id === updated.id ? updated : item)); setSelected(updated); }
  function showError(error: unknown) { setMessage(error instanceof Error ? error.message : '요청을 처리하지 못했습니다.'); }
  async function save() { if (!selected) return; setMessage(''); try { const { id: _id, createdAt: _created, updatedAt: _updated, comments: _comments, ...body } = selected; replace(await boardApi.updateReview(selected.id, body)); setMessage('리뷰를 저장했습니다.'); } catch (e) { showError(e); } }
  async function remove() { if (!selected || !window.confirm('이 리뷰를 삭제할까요?')) return; try { await boardApi.deleteReview(selected.id); const next = items.filter((item) => item.id !== selected.id); setItems(next); setSelected(next[0] ?? null); } catch (e) { showError(e); } }
  async function addComment() { if (!selected || !comment.trim()) return; try { replace(await boardApi.addReviewComment(selected.id, comment)); setComment(''); } catch (e) { showError(e); } }
  async function uploadImage(file: File) { if (!selected) return; setUploadingImage(true); setMessage(''); try { const uploaded = await boardApi.uploadReviewImage(file); setSelected({ ...selected, imageUrl: uploaded.imageUrl }); } catch (e) { showError(e); } finally { setUploadingImage(false); } }

  return (
    <section className="admin-page">
      <BoardPageHeader title="리뷰 관리" description="회원 리뷰를 확인하고 공개 상태, 내용, 관리자 답글을 관리합니다." />
      <div className="board-workspace">
        <aside className="board-list-panel">
          <div className="board-search"><Search size={16} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="작성자, 상품, 제목 검색" /></div>
          <div className="board-list">{filtered.map((item) => <button key={item.id} className={selected?.id === item.id ? 'active' : ''} onClick={() => setSelected(item)}><span>{item.productName}</span><strong>{item.title}</strong><small>{'★'.repeat(item.rating)} · {item.authorName} · {item.published ? '공개' : '비공개'}</small></button>)}{filtered.length === 0 && <p className="board-empty">리뷰가 없습니다.</p>}</div>
        </aside>
        {selected ? <div className="board-editor review-editor">
          <div className="review-summary"><div className="review-image">{selected.imageUrl ? <img src={selected.imageUrl} alt="" /> : <Star size={28} />}</div><div><strong>{selected.productName}</strong><span>{new Date(selected.createdAt).toLocaleString('ko-KR')}</span></div></div>
          <div className="board-form-grid"><label>작성자<input value={selected.authorName} onChange={(e) => setSelected({ ...selected, authorName: e.target.value })} /></label><label>평점<select value={selected.rating} onChange={(e) => setSelected({ ...selected, rating: Number(e.target.value) })}>{[5,4,3,2,1].map((score) => <option key={score} value={score}>{score}점</option>)}</select></label></div>
          <label>상품명<input value={selected.productName} onChange={(e) => setSelected({ ...selected, productName: e.target.value })} /></label>
          <label>제목<input value={selected.title} onChange={(e) => setSelected({ ...selected, title: e.target.value })} /></label>
          <label>리뷰 내용<textarea rows={7} value={selected.content} onChange={(e) => setSelected({ ...selected, content: e.target.value })} /></label>
          <label>이미지 파일<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" disabled={uploadingImage} onChange={(e) => { const file = e.target.files?.[0]; if (file) void uploadImage(file); }} /><small className="board-upload-help">{uploadingImage ? '업로드 중...' : 'JPG, PNG, GIF, WEBP · 최대 5MB'}</small></label>
          <div className="board-editor-footer"><div className="board-checks"><label><input type="checkbox" checked={selected.featured} onChange={(e) => setSelected({ ...selected, featured: e.target.checked })} /> 추천 리뷰</label><label><input type="checkbox" checked={selected.published} onChange={(e) => setSelected({ ...selected, published: e.target.checked })} /> 공개</label></div><div className="board-actions"><button className="admin-danger-button" onClick={remove}><Trash2 size={16} />삭제</button><button className="admin-blue-outline-button" onClick={save}><Save size={16} />저장</button></div></div>
          <section className="review-comments"><h2>관리자 답글 <span>{selected.comments.length}</span></h2>{selected.comments.map((item) => <article key={item.id}><strong>{item.adminName}</strong><time>{new Date(item.createdAt).toLocaleString('ko-KR')}</time><p>{item.content}</p></article>)}<div className="review-comment-form"><textarea rows={3} value={comment} maxLength={2000} onChange={(e) => setComment(e.target.value)} placeholder="고객에게 보여줄 답글을 입력하세요" /><button className="admin-blue-outline-button" onClick={addComment} disabled={!comment.trim()}><MessageSquarePlus size={16} />답글 등록</button></div></section>
          {message && <p className="board-message">{message}</p>}
        </div> : <div className="board-empty-detail">관리할 리뷰를 선택하세요.</div>}
      </div>
    </section>
  );
}
