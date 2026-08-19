import { ExternalLink, Eye, EyeOff, Link2, PanelTop, Plus, Save, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { getSitePages, homepageContentApi, uploadHomepageImage, type HomepageContent, type HomepageContentPayload, type SitePage } from '../../api/homepageManagement';
import { useAdminPageRefresh } from '../../hooks/useAdminPageRefresh';
import { WebPageHeader } from './WebManagementNav';
import { openClientPage } from '../../utils/clientUrl';

export type ContentPageConfig = {
  endpoint: string; title: string; description: string; itemName: string;
  subtitleLabel?: string; contentLabel?: string; image?: boolean; link?: boolean;
  schedule?: boolean; singleton?: boolean; applicationType?: boolean; linkLabel?: boolean;
  previewPath?: (item: HomepageContentPayload) => string;
};

const empty = (): HomepageContentPayload => ({ title:'', subtitle:'', content:'', imageUrl:'', linkUrl:'', linkLabel:'', applicationType:'', displayOrder:1, active:true, startsAt:null, endsAt:null });

export function HomepageContentManagementPage({config}:{config:ContentPageConfig}) {
  const api = homepageContentApi(config.endpoint);
  const [items,setItems]=useState<HomepageContent[]>([]);
  const [selectedId,setSelectedId]=useState<number|null>(null);
  const [form,setForm]=useState<HomepageContentPayload>(empty);
  const [message,setMessage]=useState('');
  const [saving,setSaving]=useState(false);
  const [uploadingImage,setUploadingImage]=useState(false);
  const [sitePages,setSitePages]=useState<SitePage[]>([]);
  const [linkMode,setLinkMode]=useState<'page'|'url'>('page');

  async function load(){try{const data=await api.findAll();setItems(data);const selected=data.find(x=>x.id===selectedId)??data[0];if(selected){setSelectedId(selected.id);setForm(toPayload(selected));setLinkMode(isInternalPath(selected.linkUrl)?'page':'url');}else{setSelectedId(null);setForm(empty());}}catch(e){setMessage(errorText(e));}}
  useEffect(()=>{void load();if(config.link)getSitePages().then(setSitePages).catch(()=>setMessage('홈페이지 페이지 목록을 불러오지 못했습니다.'));},[]); useAdminPageRefresh(load);
  function select(item:HomepageContent){setSelectedId(item.id);setForm(toPayload(item));setLinkMode(isInternalPath(item.linkUrl)?'page':'url');setMessage('');}
  function createNew(){setSelectedId(null);setForm(empty());setLinkMode('page');setMessage('');}
  async function save(){if(!form.title.trim())return setMessage('제목을 입력해주세요.');if(config.applicationType&&!form.applicationType)return setMessage('적용 구분을 선택해주세요.');if(config.image&&!form.imageUrl?.trim())return setMessage('이미지 파일을 등록해주세요.');if(config.link&&!form.linkUrl?.trim())return setMessage('연결할 페이지 또는 URL을 선택해주세요.');if(form.linkUrl&&!validLink(form.linkUrl))return setMessage('연결 URL은 홈페이지 경로 또는 http(s) 주소만 사용할 수 있습니다.');if(form.startsAt&&form.endsAt&&new Date(form.endsAt)<new Date(form.startsAt))return setMessage('노출 종료일은 시작일 이후여야 합니다.');setSaving(true);setMessage('');try{const saved=selectedId?await api.update(selectedId,form):await api.create(form);const next=selectedId?items.map(x=>x.id===saved.id?saved:x):[...items,saved];setItems(next.sort(contentSort));setSelectedId(saved.id);setForm(toPayload(saved));setMessage(`${config.itemName}을 저장했습니다.`);}catch(e){setMessage(errorText(e));}finally{setSaving(false);}}
  async function remove(){if(!selectedId||!window.confirm(`${config.itemName}을 삭제할까요?`))return;try{await api.delete(selectedId);const next=items.filter(x=>x.id!==selectedId);setItems(next);if(next[0])select(next[0]);else createNew();}catch(e){setMessage(errorText(e));}}
  async function uploadImage(file:File){setUploadingImage(true);setMessage('');try{const uploaded=await uploadHomepageImage(file);setForm(current=>({...current,imageUrl:uploaded.imageUrl}));}catch(e){setMessage(errorText(e));}finally{setUploadingImage(false);}}

  return <section className="admin-page"><WebPageHeader title={config.title} description={config.description}/><div className="web-content-layout">
    <aside className="web-content-list"><div className="web-list-head"><strong>{config.itemName} 목록</strong>{!config.singleton&&<button type="button" onClick={createNew} title={`${config.itemName} 추가`}><Plus size={17}/></button>}</div>{items.map(item=><button key={item.id} className={selectedId===item.id?'active':''} onClick={()=>select(item)}><span>{item.active?<Eye size={14}/>:<EyeOff size={14}/>} {config.applicationType&&`${applicationTypeLabel(item.applicationType)} · `}순서 {item.displayOrder}</span><strong>{displayTitle(item.title)}</strong><small>{item.subtitle||'부가 문구 없음'}</small></button>)}{!items.length&&<p>등록된 내용이 없습니다.</p>}</aside>
    <div className="web-content-editor">
      {config.image&&<div className="web-image-preview">{form.imageUrl?<img src={previewUrl(form.imageUrl)} alt="미리보기"/>:<ImagePlaceholder/>}</div>}
      <div className="board-form-grid"><label>제목<input value={form.title} maxLength={200} onChange={e=>setForm({...form,title:e.target.value})}/></label>{!config.singleton&&<label>노출 순서<input type="number" min="0" value={form.displayOrder} onChange={e=>setForm({...form,displayOrder:Number(e.target.value)})}/></label>}</div>
      {config.applicationType&&<label>적용 구분<select required value={form.applicationType??''} onChange={e=>{const applicationType=e.target.value;setForm({...form,applicationType,displayOrder:selectedId?form.displayOrder:nextOrder(items,applicationType)})}}><option value="" disabled hidden>적용 구분을 선택하세요</option><option value="DELIVERY_AGENCY">배송대행</option><option value="PURCHASE_AGENCY">구매대행</option></select></label>}
      {config.subtitleLabel&&<label>{config.subtitleLabel}<input value={form.subtitle??''} maxLength={300} onChange={e=>setForm({...form,subtitle:e.target.value})}/></label>}
      {config.contentLabel&&<label>{config.contentLabel}<textarea rows={6} value={form.content??''} maxLength={20000} onChange={e=>setForm({...form,content:e.target.value})}/></label>}
      {config.image&&<label>이미지 파일<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" disabled={uploadingImage} onChange={e=>{const file=e.target.files?.[0];if(file)void uploadImage(file)}}/><small className="web-upload-help">{uploadingImage?'업로드 중...':'JPG, PNG, GIF, WEBP · 최대 5MB'}</small></label>}
      {config.link&&<div className="web-link-section"><div className="web-link-mode" role="group" aria-label="연결 방식"><button type="button" className={linkMode==='page'?'active':''} onClick={()=>{setLinkMode('page');setForm({...form,linkUrl:''})}}><PanelTop size={16}/>홈페이지 페이지</button><button type="button" className={linkMode==='url'?'active':''} onClick={()=>{setLinkMode('url');setForm({...form,linkUrl:''})}}><Link2 size={16}/>직접 URL</button></div><div className="board-form-grid">{linkMode==='page'?<label>연결할 홈페이지 페이지<select value={form.linkUrl??''} onChange={e=>setForm({...form,linkUrl:e.target.value})}><option value="" disabled hidden>페이지를 선택하세요</option>{Object.entries(groupPages(sitePages)).map(([group,pages])=><optgroup key={group} label={group}>{pages.map(page=><option key={page.path} value={page.path}>{page.label}</option>)}</optgroup>)}</select></label>:<label>연결 URL<input type="url" value={form.linkUrl??''} placeholder="https://example.com" onChange={e=>setForm({...form,linkUrl:e.target.value})}/></label>}{config.linkLabel!==false&&<label>링크 버튼 문구<input value={form.linkLabel??''} onChange={e=>setForm({...form,linkLabel:e.target.value})}/></label>}</div></div>}
      {config.schedule&&<div className="board-form-grid"><label>노출 시작<input type="datetime-local" value={dateInput(form.startsAt)} onChange={e=>setForm({...form,startsAt:e.target.value||null})}/></label><label>노출 종료<input type="datetime-local" value={dateInput(form.endsAt)} onChange={e=>setForm({...form,endsAt:e.target.value||null})}/></label></div>}
      <div className="board-editor-footer"><label className="web-active-check"><input type="checkbox" checked={form.active} onChange={e=>setForm({...form,active:e.target.checked})}/> 홈페이지에 노출</label><div className="board-actions">{selectedId&&config.previewPath&&<button className="admin-secondary-button" type="button" onClick={()=>openClientPage(config.previewPath!(form))}><ExternalLink size={16}/>홈페이지에서 확인</button>}{selectedId&&<button className="admin-danger-button" onClick={remove}><Trash2 size={16}/>삭제</button>}<button className="admin-blue-outline-button" onClick={save} disabled={saving}><Save size={16}/>{saving?'저장 중':'저장'}</button></div></div>
      {message&&<p className="board-message">{message}</p>}
    </div></div></section>;
}

function ImagePlaceholder(){return <div className="web-image-empty">이미지 미리보기</div>}
function toPayload(x:HomepageContent):HomepageContentPayload{return{title:x.title,subtitle:x.subtitle,content:x.content,imageUrl:x.imageUrl,linkUrl:x.linkUrl,linkLabel:x.linkLabel,applicationType:x.applicationType??'',displayOrder:x.displayOrder,active:x.active,startsAt:x.startsAt,endsAt:x.endsAt}}
function dateInput(value:string|null){return value?value.slice(0,16):''}
function displayTitle(value:string){return value.replace(/\\n|[\r\n]+/g,' ')}
function previewUrl(value:string){
  if(!value.startsWith('/assets/')||window.location.port!=='5173')return value;
  return `${window.location.protocol}//${window.location.hostname}:5174${value}`;
}
function errorText(e:unknown){return e instanceof Error?e.message:'요청을 처리하지 못했습니다.'}
function isInternalPath(value:string|null){return !value||value.startsWith('/')||value.startsWith('#')}
function groupPages(pages:SitePage[]){return pages.reduce<Record<string,SitePage[]>>((groups,page)=>{(groups[page.group]??=[]).push(page);return groups},{})}
function nextOrder(items:HomepageContent[],applicationType:string){return Math.max(0,...items.filter(item=>item.applicationType===applicationType).map(item=>item.displayOrder))+1}
function applicationTypeLabel(value:string|null){return value==='PURCHASE_AGENCY'?'구매대행':'배송대행'}
function contentSort(a:HomepageContent,b:HomepageContent){return (a.applicationType??'').localeCompare(b.applicationType??'')||a.displayOrder-b.displayOrder}
function validLink(value:string){if(value.startsWith('/')||value.startsWith('#'))return true;try{const url=new URL(value);return url.protocol==='http:'||url.protocol==='https:'}catch{return false}}
