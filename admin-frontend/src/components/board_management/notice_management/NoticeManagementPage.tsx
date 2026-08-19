import { ExternalLink, Save, Search, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { boardApi, type AdminNotice, type NoticePayload } from '../../../api/boardManagement';
import { BoardPageHeader } from '../BoardManagementNav';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { NoticeCategorySelect } from '../notice_create/NoticeCategorySelect';
import { openClientPage } from '../../../utils/clientUrl';

export function NoticeManagementPage() {
  const [items, setItems] = useState<AdminNotice[]>([]);
  const [selected, setSelected] = useState<AdminNotice | null>(null);
  const [query, setQuery] = useState('');
  const [message, setMessage] = useState('');
  const filtered = useMemo(() => items.filter((item) => `${item.category} ${item.title}`.toLowerCase().includes(query.toLowerCase())), [items, query]);

  useEffect(() => { void load(); }, []);
  useAdminPageRefresh(load);
  async function load() { try { const data = await boardApi.notices(); setItems(data); setSelected((current) => data.find((item) => item.id === current?.id) ?? data[0] ?? null); } catch (e) { setMessage(e instanceof Error ? e.message : '목록을 불러오지 못했습니다.'); } }
  async function save() { if (!selected) return; setMessage(''); try { const updated = await boardApi.updateNotice(selected.id, payload(selected)); setItems((all) => all.map((item) => item.id === updated.id ? updated : item)); setSelected(updated); setMessage('변경사항을 저장했습니다.'); } catch (e) { setMessage(e instanceof Error ? e.message : '저장하지 못했습니다.'); } }
  async function remove() { if (!selected || !window.confirm('이 공지사항을 삭제할까요?')) return; try { await boardApi.deleteNotice(selected.id); const next = items.filter((item) => item.id !== selected.id); setItems(next); setSelected(next[0] ?? null); } catch (e) { setMessage(e instanceof Error ? e.message : '삭제하지 못했습니다.'); } }

  return (
    <section className="admin-page">
      <BoardPageHeader title="공지사항 관리" description="등록된 공지사항을 검색하고 수정하거나 삭제합니다." />
      <div className="board-workspace">
        <aside className="board-list-panel">
          <div className="board-search"><Search size={16} /><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="제목 또는 분류 검색" /></div>
          <div className="board-list">{filtered.map((item) => <button key={item.id} className={selected?.id === item.id ? 'active' : ''} onClick={() => setSelected(item)}><span>{item.pinned && '고정 · '}{item.category}</span><strong>{item.title}</strong><small>{item.published ? '공개' : '비공개'} · {formatDate(item.publishedAt)}</small></button>)}{filtered.length === 0 && <p className="board-empty">공지사항이 없습니다.</p>}</div>
        </aside>
        {selected ? <div className="board-editor">
          <div className="board-form-grid"><NoticeCategorySelect value={selected.category} onChange={(category) => setSelected((current) => current ? { ...current, category } : current)} onError={setMessage} /><label>게시일<input type="datetime-local" value={toInputDate(selected.publishedAt)} onChange={(e) => setSelected({ ...selected, publishedAt: e.target.value || null })} /></label></div>
          <label>제목<input value={selected.title} onChange={(e) => setSelected({ ...selected, title: e.target.value })} /></label>
          <label>내용<textarea rows={15} value={selected.content} onChange={(e) => setSelected({ ...selected, content: e.target.value })} /></label>
          <div className="board-editor-footer"><div className="board-checks"><label><input type="checkbox" checked={selected.pinned} onChange={(e) => setSelected({ ...selected, pinned: e.target.checked })} /> 상단 고정</label><label><input type="checkbox" checked={selected.published} onChange={(e) => setSelected({ ...selected, published: e.target.checked })} /> 공개</label></div><div className="board-actions"><button className="admin-secondary-button" type="button" onClick={()=>openClientPage(`/notices/${selected.id}`)}><ExternalLink size={16}/>홈페이지에서 확인</button><button className="admin-danger-button" onClick={remove}><Trash2 size={16} />삭제</button><button className="admin-blue-outline-button" onClick={save}><Save size={16} />등록</button></div></div>
          {message && <p className="board-message">{message}</p>}
        </div> : <div className="board-empty-detail">관리할 공지사항을 선택하세요.</div>}
      </div>
    </section>
  );
}

function payload(item: AdminNotice): NoticePayload { const { category, title, content, pinned, published, publishedAt } = item; return { category, title, content, pinned, published, publishedAt }; }
function formatDate(value: string | null) { return value ? new Date(value).toLocaleDateString('ko-KR') : '-'; }
function toInputDate(value: string | null) { return value ? value.slice(0, 16) : ''; }
