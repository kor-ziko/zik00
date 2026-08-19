import { Plus, Save, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { adminSettingsApi, type AdminItem, type AdminPayload } from '../../../api/settingsManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { SettingsPageHeader } from '../SettingsManagementNav';

const blank = (): AdminPayload => ({ loginId: '', name: '', password: '', active: true });

export function AdminManagementPage() {
  const [items, setItems] = useState<AdminItem[]>([]);
  const [currentAdminId, setCurrentAdminId] = useState<number | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [form, setForm] = useState<AdminPayload>(blank);
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const select = useCallback((item: AdminItem) => {
    setSelectedId(item.id);
    setForm({ loginId: item.loginId, name: item.name, password: '', active: item.active });
    setMessage('');
  }, []);

  const load = useCallback(async () => {
    try {
      const [data, current] = await Promise.all([adminSettingsApi.findAll(), adminSettingsApi.current()]);
      setItems(data);
      setCurrentAdminId(current.adminId);
      const first = data.find((item) => item.id === selectedId) ?? data[0];
      if (first) select(first);
    } catch (error) {
      setMessage(errorText(error));
    }
  }, [select, selectedId]);

  useEffect(() => { void load(); }, [load]);
  useAdminPageRefresh(load);

  async function save() {
    if (!/^[A-Za-z0-9._-]{4,50}$/.test(form.loginId.trim())) {
      setMessage('관리자 아이디는 4~50자의 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.');
      return;
    }
    if (!form.name.trim()) {
      setMessage('관리자 이름을 입력해주세요.');
      return;
    }
    if ((!selectedId && form.password.length < 8) || (selectedId && form.password.length > 0 && form.password.length < 8)) {
      setMessage('비밀번호는 8자 이상이어야 합니다.');
      return;
    }
    setSaving(true);
    try {
      const saved = selectedId
        ? await adminSettingsApi.update(selectedId, form)
        : await adminSettingsApi.create(form);
      setItems((current) => selectedId
        ? current.map((item) => item.id === saved.id ? saved : item)
        : [...current, saved]);
      select(saved);
      setMessage('관리자 정보를 저장했습니다.');
    } catch (error) {
      setMessage(errorText(error));
    } finally {
      setSaving(false);
    }
  }

  async function remove() {
    if (!selectedId) return;
    if (selectedId === currentAdminId) {
      setMessage('현재 로그인 중인 관리자 계정은 삭제할 수 없습니다. 다른 관리자 계정으로 로그인한 뒤 삭제해주세요.');
      return;
    }
    if (!confirm('관리자 계정을 삭제할까요?')) return;
    try {
      await adminSettingsApi.delete(selectedId);
      const next = items.filter((item) => item.id !== selectedId);
      setItems(next);
      if (next[0]) select(next[0]);
      else { setSelectedId(null); setForm(blank()); }
      setMessage('관리자 계정을 삭제했습니다.');
    } catch (error) {
      setMessage(errorText(error));
    }
  }

  const isCurrentAdmin = selectedId !== null && selectedId === currentAdminId;

  return <section className="admin-page">
    <SettingsPageHeader title="관리자관리" description="관리자 계정을 추가하고 사용 여부와 비밀번호를 관리합니다." />
    <div className="settings-content-layout">
      <aside className="settings-list">
        <div className="settings-list-head"><strong>관리자 목록</strong><button onClick={() => { setSelectedId(null); setForm(blank()); setMessage(''); }} title="관리자 추가"><Plus size={17} /></button></div>
        {items.map((item) => <button key={item.id} className={selectedId === item.id ? 'active' : ''} onClick={() => select(item)}>
          <span>{item.active ? '사용 중' : '사용 중지'}{item.id === currentAdminId ? ' · 현재 계정' : ''}</span>
          <strong>{item.name}</strong><small>{item.loginId}</small>
        </button>)}
      </aside>
      <div className="settings-editor">
        <div className="settings-form-grid">
          <label>관리자 아이디<input value={form.loginId} disabled={selectedId !== null} onChange={(event) => setForm({ ...form, loginId: event.target.value })} /></label>
          <label>관리자 이름<input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
        </div>
        <label>{selectedId ? '새 비밀번호' : '비밀번호'}<input type="password" value={form.password} placeholder={selectedId ? '변경할 때만 입력하세요' : '8자 이상'} onChange={(event) => setForm({ ...form, password: event.target.value })} /></label>
        {isCurrentAdmin && <p className="settings-account-note">현재 로그인 중인 계정입니다. 이 계정을 삭제하려면 다른 관리자 계정으로 로그인해야 합니다.</p>}
        <div className="settings-editor-footer">
          <label className="settings-active"><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> 계정 사용</label>
          <div>{selectedId && !isCurrentAdmin && <button className="admin-danger-button" onClick={remove}><Trash2 size={16} />삭제</button>}<button className="admin-blue-outline-button" onClick={save} disabled={saving}><Save size={16} />{saving ? '저장 중' : '저장'}</button></div>
        </div>
        {message && <p className="board-message">{message}</p>}
      </div>
    </div>
  </section>;
}

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '요청을 처리하지 못했습니다.';
}
