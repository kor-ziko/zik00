import { ExternalLink, Save } from 'lucide-react';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { boardApi, type NoticePayload } from '../../../api/boardManagement';
import { BoardPageHeader } from '../BoardManagementNav';
import { NoticeCategorySelect } from './NoticeCategorySelect';
import { openClientPage } from '../../../utils/clientUrl';

const initialForm = (): NoticePayload => ({
  category: '', title: '', content: '', pinned: false, published: true,
  publishedAt: new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 16),
});

export function NoticeCreatePage() {
  const [form, setForm] = useState(initialForm);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [createdNoticeId, setCreatedNoticeId] = useState<number | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setMessage('');
    try {
      const created = await boardApi.createNotice(form);
      setCreatedNoticeId(created.id);
      setForm(initialForm()); setMessage('공지사항을 등록했습니다.');
    } catch (error) { setMessage(error instanceof Error ? error.message : '등록하지 못했습니다.'); }
    finally { setSaving(false); }
  }

  return (
    <section className="admin-page">
      <BoardPageHeader title="공지사항 등록" description="고객에게 노출할 공지 내용을 작성하고 공개 상태를 설정합니다." />
      <form className="board-editor board-editor-single" onSubmit={submit}>
        <div className="board-form-grid">
          <NoticeCategorySelect value={form.category} onChange={(category) => setForm((current) => ({ ...current, category }))} onError={setMessage} />
          <label>게시일<input type="datetime-local" value={form.publishedAt ?? ''} onChange={(e) => setForm({ ...form, publishedAt: e.target.value || null })} /></label>
        </div>
        <label>제목<input value={form.title} maxLength={255} placeholder="공지 제목을 입력하세요" onChange={(e) => setForm({ ...form, title: e.target.value })} required /></label>
        <label>내용<textarea value={form.content} maxLength={20000} rows={16} placeholder="공지 내용을 입력하세요" onChange={(e) => setForm({ ...form, content: e.target.value })} required /></label>
        <div className="board-editor-footer">
          <div className="board-checks">
            <label><input type="checkbox" checked={form.pinned} onChange={(e) => setForm({ ...form, pinned: e.target.checked })} /> 상단 고정</label>
            <label><input type="checkbox" checked={form.published} onChange={(e) => setForm({ ...form, published: e.target.checked })} /> 즉시 공개</label>
          </div>
          <div className="board-actions">{createdNoticeId&&<button className="admin-secondary-button" type="button" onClick={()=>openClientPage(`/notices/${createdNoticeId}`)}><ExternalLink size={16}/>홈페이지에서 확인</button>}<button className="admin-blue-outline-button" disabled={saving}><Save size={17} />{saving ? '등록 중' : '공지 등록'}</button></div>
        </div>
        {message && <p className="board-message" role="status">{message}</p>}
      </form>
    </section>
  );
}
