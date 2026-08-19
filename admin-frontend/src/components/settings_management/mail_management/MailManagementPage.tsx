import { AlignCenter, AlignLeft, AlignRight, Bold, Code2, Eye, EyeOff, Italic, Plus, Save, Send, Trash2, Underline } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { mailTemplateApi, type MailDeliveryStatus, type MailTemplate, type MailTemplatePayload, type MailTemplateType } from '../../../api/settingsManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { SettingsPageHeader } from '../SettingsManagementNav';

const blank = (): MailTemplatePayload => ({ code:'', name:'', templateType:'SIGNUP', subject:'', senderName:'', replyTo:'', content:'', defaultTemplate:false, displayOrder:1, active:true });
const typeLabels:Record<MailTemplateType,string>={SIGNUP:'회원가입',MARKETING:'광고·이벤트',NOTICE:'공지·안내',CUSTOM:'직접 작성'};

export function MailManagementPage(){
  const [items,setItems]=useState<MailTemplate[]>([]);
  const [selectedId,setSelectedId]=useState<number|null>(null);
  const [form,setForm]=useState<MailTemplatePayload>(blank);
  const [message,setMessage]=useState('');
  const [saving,setSaving]=useState(false);
  const [sendingTest,setSendingTest]=useState(false);
  const [testRecipient,setTestRecipient]=useState('');
  const [deliveryStatus,setDeliveryStatus]=useState<MailDeliveryStatus|null>(null);
  const [editorMode,setEditorMode]=useState<'visual'|'html'>('visual');
  const contentRef=useRef<HTMLTextAreaElement>(null);
  const visualEditorRef=useRef<HTMLDivElement>(null);

  const select=useCallback((item:MailTemplate)=>{setSelectedId(item.id);setForm(toPayload(item));setMessage('')},[]);
  const load=useCallback(async()=>{try{const [data,status]=await Promise.all([mailTemplateApi.findAll(),mailTemplateApi.deliveryStatus()]);setDeliveryStatus(status);setItems(data);const selected=data.find(item=>item.id===selectedId)??data[0];if(selected)select(selected);else{setSelectedId(null);setForm(blank())}}catch(error){setMessage(errorText(error))}},[select,selectedId]);
  useEffect(()=>{void load()},[load]);useAdminPageRefresh(load);

  function createNew(){setSelectedId(null);setForm({...blank(),displayOrder:items.length+1});setMessage('')}
  async function save(){const payload={...form,content:normalizeEditorHtml(form.content)};const validation=validate(payload);if(validation){setMessage(validation);return}setSaving(true);setMessage('');try{const saved=selectedId?await mailTemplateApi.update(selectedId,payload):await mailTemplateApi.create(payload);const data=await mailTemplateApi.findAll();setItems(data);select(data.find(item=>item.id===saved.id)??saved);setMessage('메일 템플릿을 저장했습니다.')}catch(error){setMessage(errorText(error))}finally{setSaving(false)}}
  async function remove(){if(!selectedId||!confirm('이 메일 템플릿을 삭제할까요?'))return;try{await mailTemplateApi.delete(selectedId);const next=items.filter(item=>item.id!==selectedId);setItems(next);if(next[0])select(next[0]);else createNew();setMessage('메일 템플릿을 삭제했습니다.')}catch(error){setMessage(errorText(error))}}
  function updateContent(content:string){setForm(value=>({...value,content}))}
  function syncVisualContent(){if(visualEditorRef.current)updateContent(normalizeEditorHtml(visualEditorRef.current.innerHTML))}
  function insertHtml(html:string){
    if(editorMode==='visual'){
      const editor=visualEditorRef.current;
      if(!editor)return;
      editor.focus();
      const rendered=resolveEditorHtml(html);
      if(!document.execCommand('insertHTML',false,rendered))editor.insertAdjacentHTML('beforeend',rendered);
      syncVisualContent();
      return;
    }
    const current=form.content;const start=contentRef.current?.selectionStart??current.length;const end=contentRef.current?.selectionEnd??start;const next=`${current.slice(0,start)}${html}${current.slice(end)}`;updateContent(next);requestAnimationFrame(()=>{contentRef.current?.focus();contentRef.current?.setSelectionRange(start+html.length,start+html.length)})
  }
  function formatSelection(open:string,close:string,placeholder='내용',command?:string){
    if(editorMode==='visual'&&command){visualEditorRef.current?.focus();document.execCommand(command,false);syncVisualContent();return}
    const current=form.content;const start=contentRef.current?.selectionStart??current.length;const end=contentRef.current?.selectionEnd??start;const selected=current.slice(start,end)||placeholder;insertHtml(`${open}${selected}${close}`)
  }
  function alignVisual(alignment:'left'|'center'|'right'){
    if(editorMode==='visual'){visualEditorRef.current?.focus();document.execCommand(alignment==='left'?'justifyLeft':alignment==='center'?'justifyCenter':'justifyRight',false);syncVisualContent();return}
    formatSelection(`<div style="text-align:${alignment};">`,'</div>')
  }
  async function sendTest(){if(!selectedId){setMessage('메일 템플릿을 먼저 저장해주세요.');return}if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(testRecipient.trim())){setMessage('테스트 메일을 받을 이메일을 정확히 입력해주세요.');return}setSendingTest(true);setMessage('');try{await mailTemplateApi.sendTest(selectedId,testRecipient.trim());setMessage('테스트 메일을 발송했습니다. 받은편지함과 스팸함을 확인해주세요.')}catch(error){setMessage(errorText(error))}finally{setSendingTest(false)}}

  return <section className="admin-page"><SettingsPageHeader title="메일관리" description="자동 발송과 광고·안내에 사용할 메일 템플릿을 종류별로 관리합니다."/><div className="settings-content-layout">
    <aside className="settings-list"><div className="settings-list-head"><strong>메일 템플릿</strong><button type="button" onClick={createNew} title="메일 템플릿 추가"><Plus size={17}/></button></div>{items.map(item=><button key={item.id} className={selectedId===item.id?'active':''} onClick={()=>select(item)}><span>{item.active?<Eye size={13}/>:<EyeOff size={13}/>} {typeLabels[item.templateType]}{item.defaultTemplate?' · 기본':''}</span><strong>{item.name}</strong><small>{item.code}</small></button>)}{items.length===0&&<p>등록된 메일 템플릿이 없습니다.</p>}</aside>
    <div className="settings-editor"><div className={`mail-delivery-status ${deliveryStatus?.enabled&&deliveryStatus.configured?'ready':'warning'}`}><strong>메일 발송 상태</strong><span>{deliveryStatus?.message??'발송 상태를 확인하고 있습니다.'}</span></div><div className="settings-form-grid"><label>메일 코드<input value={form.code} maxLength={100} placeholder="WELCOME" onChange={event=>setForm({...form,code:event.target.value.toUpperCase()})}/></label><label>템플릿명<input value={form.name} maxLength={200} placeholder="회원가입 환영 메일" onChange={event=>setForm({...form,name:event.target.value})}/></label></div>
      <div className="settings-form-grid"><label>메일 종류<select value={form.templateType} onChange={event=>setForm({...form,templateType:event.target.value as MailTemplateType,defaultTemplate:false})}><option value="" disabled hidden>메일 종류를 선택하세요</option>{Object.entries(typeLabels).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select></label><label>노출 순서<input type="number" min="0" value={form.displayOrder} onChange={event=>setForm({...form,displayOrder:Number(event.target.value)})}/></label></div>
      <label>메일 제목<input value={form.subject} maxLength={300} placeholder="${name}님, 가입을 환영합니다" onChange={event=>setForm({...form,subject:event.target.value})}/></label>
      <label>발신자명<input value={form.senderName} maxLength={100} placeholder="ZIK:00" onChange={event=>setForm({...form,senderName:event.target.value})}/></label>
      <div className="settings-content-field">
        <strong>메일 본문</strong>
        <div className="mail-editor-toolbar">
          <div className="mail-format-tools">
            <button type="button" title="굵게" onClick={()=>formatSelection('<strong>','</strong>','내용','bold')}><Bold size={16}/></button>
            <button type="button" title="기울임" onClick={()=>formatSelection('<em>','</em>','내용','italic')}><Italic size={16}/></button>
            <button type="button" title="밑줄" onClick={()=>formatSelection('<u>','</u>','내용','underline')}><Underline size={16}/></button><span/>
            <button type="button" title="왼쪽 정렬" onClick={()=>alignVisual('left')}><AlignLeft size={16}/></button>
            <button type="button" title="가운데 정렬" onClick={()=>alignVisual('center')}><AlignCenter size={16}/></button>
            <button type="button" title="오른쪽 정렬" onClick={()=>alignVisual('right')}><AlignRight size={16}/></button>
          </div>
          <div className="mail-editor-actions">
            <div className="mail-editor-mode-tabs" role="tablist" aria-label="메일 편집 방식">
              <button type="button" role="tab" aria-selected={editorMode==='visual'} className={editorMode==='visual'?'active':''} onClick={()=>setEditorMode('visual')}><Eye size={15}/>실제 메일</button>
              <button type="button" role="tab" aria-selected={editorMode==='html'} className={editorMode==='html'?'active':''} onClick={()=>{syncVisualContent();setEditorMode('html')}}><Code2 size={15}/>HTML</button>
            </div>
          </div>
        </div>
        <div className="mail-content-workspace">
          <div className="mail-pane-title"><span>{editorMode==='visual'?(form.templateType==='SIGNUP'?'회원가입 메일 고정 형식':'실제 메일 편집'):'HTML 편집'}</span><small><code>${'{name}'}</code> <code>${'{nickname}'}</code> <code>${'{email}'}</code> <code>${'{loginId}'}</code> <code>${'{mobilePhone}'}</code></small></div>
          {editorMode==='visual'?(form.templateType==='SIGNUP'?<div className="signup-mail-preview-shell"><div className="signup-mail-preview-card"><div className="signup-mail-preview-head"><strong>{form.senderName||'ZIK:00'}</strong><span>{form.senderName||'ZIK:00'}에서 보내는 메일입니다.</span></div><div className="signup-mail-preview-line"/><h2>회원가입 완료</h2><div className="signup-mail-title-line"/><div key={`signup-visual-${selectedId??'new'}`} ref={visualEditorRef} className="signup-mail-content-editor" contentEditable suppressContentEditableWarning dangerouslySetInnerHTML={{__html:resolveEditorHtml(form.content)}} onInput={syncVisualContent} onBlur={syncVisualContent}/><div className="signup-mail-member-info"><div><strong>아이디</strong><b>:</b><span>sample_member</span></div><div><strong>사서함</strong><b>:</b><span>ZK000001</span></div><div><strong>휴대폰</strong><b>:</b><span>010-1234-5678</span></div></div><div className="signup-mail-preview-footer">본메일은 고객님의 메일수신 동의에 의한 발신전용 메일입니다. 자세한 문의사항은 고객센터를 이용해 주시기 바랍니다.<br/><br/>상호: {form.senderName||'ZIK:00'}{form.replyTo?` | e-mail: ${form.replyTo}`:''}<br/>Copyright © ZIK:00 All rights reserved.</div></div></div>:<div key={`visual-${selectedId??'new'}`} ref={visualEditorRef} className="mail-visual-editor" contentEditable suppressContentEditableWarning dangerouslySetInnerHTML={{__html:resolveEditorHtml(form.content)}} onInput={syncVisualContent} onBlur={syncVisualContent}/>):<div className="mail-html-editor"><textarea aria-label="메일 본문 HTML" ref={contentRef} rows={20} value={form.content} onChange={event=>updateContent(event.target.value)}/></div>}
        </div>
      </div>
      <div className="mail-test-panel"><label>테스트 받을 이메일<input type="email" value={testRecipient} placeholder="받을 이메일 주소" onChange={event=>setTestRecipient(event.target.value)}/></label><button className="admin-blue-outline-button" type="button" disabled={sendingTest||!selectedId||!deliveryStatus?.enabled||!deliveryStatus?.configured} onClick={()=>void sendTest()}><Send size={16}/>{sendingTest?'발송 중':'테스트 발송'}</button></div><div className="settings-editor-footer"><div className="mail-template-checks"><label className="settings-active"><input type="checkbox" checked={form.active} onChange={event=>setForm({...form,active:event.target.checked})}/> 사용</label><label className="settings-active"><input type="checkbox" checked={form.defaultTemplate} onChange={event=>setForm({...form,defaultTemplate:event.target.checked})}/> 이 종류의 기본 템플릿</label></div><div>{selectedId&&<button className="admin-danger-button" type="button" onClick={()=>void remove()}><Trash2 size={16}/>삭제</button>}<button className="admin-blue-outline-button" type="button" disabled={saving} onClick={()=>void save()}><Save size={16}/>{saving?'저장 중':'저장'}</button></div></div>{message&&<p className="board-message">{message}</p>}
    </div></div></section>
}

function toPayload(item:MailTemplate):MailTemplatePayload{return{code:item.code,name:item.name,templateType:item.templateType,subject:item.subject,senderName:item.senderName,replyTo:item.replyTo,content:item.content,defaultTemplate:item.defaultTemplate,displayOrder:item.displayOrder,active:item.active}}
function resolveEditorHtml(content:string){
  if(!content||typeof window==='undefined')return content;
  const document=new DOMParser().parseFromString(content,'text/html');
  sanitizeDocument(document);
  return document.body.innerHTML;
}
function normalizeEditorHtml(content:string){
  if(!content||typeof window==='undefined')return content;
  const document=new DOMParser().parseFromString(content,'text/html');
  sanitizeDocument(document);
  return document.body.innerHTML;
}
function sanitizeDocument(document:Document){
  document.querySelectorAll('script,iframe,object,embed,img,picture,source').forEach(element=>element.remove());
  document.querySelectorAll('*').forEach(element=>Array.from(element.attributes).forEach(attribute=>{if(attribute.name.toLowerCase().startsWith('on'))element.removeAttribute(attribute.name)}));
}
function validate(form:MailTemplatePayload){if(!/^[A-Za-z0-9_-]+$/.test(form.code.trim()))return '메일 코드는 영문, 숫자, 밑줄, 하이픈만 사용할 수 있습니다.';if(!form.name.trim())return '템플릿명을 입력해주세요.';if(!form.templateType)return '메일 종류를 선택해주세요.';if(!form.subject.trim())return '메일 제목을 입력해주세요.';if(!form.senderName.trim())return '발신자명을 입력해주세요.';if(!form.content.trim())return '메일 본문을 입력해주세요.';if(!Number.isInteger(form.displayOrder)||form.displayOrder<0)return '노출 순서는 0 이상의 정수여야 합니다.';return ''}
function errorText(error:unknown){return error instanceof Error?error.message:'요청을 처리하지 못했습니다.'}
