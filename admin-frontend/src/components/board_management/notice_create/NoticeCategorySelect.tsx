import { Plus, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { boardApi, type NoticeCategory } from '../../../api/boardManagement';

type Props = {
  value: string;
  onChange: (value: string) => void;
  onError: (message: string) => void;
};

export function NoticeCategorySelect({ value, onChange, onError }: Props) {
  const [categories, setCategories] = useState<NoticeCategory[]>([]);
  const [adding, setAdding] = useState(false);
  const [newName, setNewName] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    boardApi.noticeCategories()
      .then((items) => {
        setCategories(items);
      })
      .catch((error) => onError(error instanceof Error ? error.message : '분류를 불러오지 못했습니다.'));
  }, []);

  async function addCategory() {
    if (!newName.trim()) return;
    setSaving(true);
    try {
      const created = await boardApi.createNoticeCategory(newName.trim());
      setCategories((items) => [...items, created]);
      onChange(created.name);
      setNewName('');
      setAdding(false);
    } catch (error) {
      onError(error instanceof Error ? error.message : '분류를 추가하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  }

  const options = value && !categories.some((item) => item.name === value)
    ? [{ id: -1, name: value, displayOrder: -1 }, ...categories]
    : categories;

  return <label>분류
    <div className="notice-category-control">
      <select value={value} onChange={(event) => onChange(event.target.value)} required>
        <option value="" disabled hidden>분류를 선택하세요</option>
        {options.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
      </select>
      <button type="button" className="admin-secondary-button notice-category-add" title="새 분류 추가" onClick={() => setAdding((open) => !open)}>{adding ? <X size={16} /> : <Plus size={16} />}</button>
    </div>
    {adding && <div className="notice-category-new">
      <input value={newName} maxLength={50} placeholder="새 분류 이름" onChange={(event) => setNewName(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); void addCategory(); } }} />
      <button type="button" className="admin-blue-outline-button" disabled={saving || !newName.trim()} onClick={() => void addCategory()}>{saving ? '추가 중' : '추가'}</button>
    </div>}
  </label>;
}
