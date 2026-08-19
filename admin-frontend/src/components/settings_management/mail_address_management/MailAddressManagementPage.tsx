import { ExternalLink, Eye, EyeOff, Info, Save, Send } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { mailAddressApi, type MailAddressPayload, type MailDeliveryStatus, type MailProvider } from '../../../api/settingsManagement';
import { useAdminPageRefresh } from '../../../hooks/useAdminPageRefresh';
import { SettingsPageHeader } from '../SettingsManagementNav';

const providerLabels:Record<MailProvider,string>={NAVER:'네이버 메일',GMAIL:'Gmail',CUSTOM:'직접 설정'};
const providerHosts:Record<Exclude<MailProvider,'CUSTOM'>,string>={NAVER:'smtp.naver.com',GMAIL:'smtp.gmail.com'};
const blank=():MailAddressPayload=>({provider:'NAVER',host:providerHosts.NAVER,port:587,username:'',password:'',senderName:'ZIK:00',active:true});

export function MailAddressManagementPage(){
  const [form,setForm]=useState<MailAddressPayload>(blank);
  const [passwordConfigured,setPasswordConfigured]=useState(false);
  const [showPassword,setShowPassword]=useState(false);
  const [status,setStatus]=useState<MailDeliveryStatus|null>(null);
  const [testRecipient,setTestRecipient]=useState('');
  const [message,setMessage]=useState('');
  const [saving,setSaving]=useState(false);
  const [testing,setTesting]=useState(false);

  const load=useCallback(async()=>{try{const [saved,currentStatus]=await Promise.all([mailAddressApi.find(),mailAddressApi.status()]);setStatus(currentStatus);if(saved){setForm({provider:saved.provider,host:saved.host,port:saved.port,username:saved.username,password:'',senderName:saved.senderName,active:saved.active});setPasswordConfigured(saved.passwordConfigured)}else{setForm(blank());setPasswordConfigured(false)}}catch(error){setMessage(errorText(error))}},[]);
  useEffect(()=>{void load()},[load]);
  useAdminPageRefresh(load);

  function changeProvider(provider:MailProvider){setForm(value=>({...value,provider,host:provider==='CUSTOM'?'':providerHosts[provider],port:587}))}
  async function save(){const validation=validate(form,passwordConfigured);if(validation){setMessage(validation);return}setSaving(true);setMessage('');try{const saved=await mailAddressApi.save(form);setForm(value=>({...value,host:saved.host,password:''}));setPasswordConfigured(saved.passwordConfigured);setStatus(await mailAddressApi.status());setMessage('회사 발신 메일 정보를 저장했습니다.')}catch(error){setMessage(errorText(error))}finally{setSaving(false)}}
  async function test(){const validation=validate(form,passwordConfigured);if(validation){setMessage(validation);return}if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(testRecipient.trim())){setMessage('테스트 메일을 받을 주소를 정확히 입력해주세요.');return}setTesting(true);setMessage('');try{const saved=await mailAddressApi.save(form);setForm(value=>({...value,host:saved.host,password:''}));setPasswordConfigured(saved.passwordConfigured);await mailAddressApi.test(testRecipient.trim());setStatus(await mailAddressApi.status());setMessage('현재 입력한 설정을 저장하고 테스트 메일을 발송했습니다. 받은편지함과 스팸함을 확인해주세요.')}catch(error){setMessage(errorText(error))}finally{setTesting(false)}}

  return <section className="admin-page"><SettingsPageHeader title="메일주소관리" description="자동 메일에 사용할 회사 발신 계정을 등록하고 연결 상태를 확인합니다."/><div className="settings-content-layout singleton"><div className="settings-editor">
    <div className={`mail-delivery-status ${status?.configured?'ready':'warning'}`}><strong>발송 계정 상태</strong><span>{status?.message??'상태를 확인하고 있습니다.'}</span></div>
    <label>메일 서비스<select value={form.provider} onChange={event=>changeProvider(event.target.value as MailProvider)}><option value="" disabled hidden>메일 서비스를 선택하세요</option>{Object.entries(providerLabels).map(([value,label])=><option key={value} value={value}>{label}</option>)}</select></label>
    {form.provider==='CUSTOM'&&<div className="settings-form-grid"><label>SMTP 서버<input value={form.host} maxLength={255} placeholder="smtp.company.com" onChange={event=>setForm({...form,host:event.target.value})}/></label><label>SMTP 포트<input type="number" min="1" max="65535" value={form.port} onChange={event=>setForm({...form,port:Number(event.target.value)})}/></label></div>}
    <div className="settings-form-grid"><label>회사 메일 주소<input type="email" value={form.username} maxLength={255} placeholder="company@example.com" onChange={event=>setForm({...form,username:event.target.value})}/></label><label>메일 비밀번호<span className="mail-password-input"><input type={showPassword?'text':'password'} value={form.password} maxLength={500} placeholder={passwordConfigured?'변경할 때만 새 비밀번호 입력':'메일 비밀번호 입력'} onChange={event=>setForm({...form,password:event.target.value})}/><button type="button" title={showPassword?'비밀번호 숨기기':'비밀번호 보기'} onClick={()=>setShowPassword(value=>!value)}>{showPassword?<EyeOff size={17}/>:<Eye size={17}/>}</button></span>{form.provider==='NAVER'&&<small className="mail-provider-note">애플리케이션 비밀번호와 네이버 메일의 IMAP/SMTP 사용함 설정이 모두 필요합니다.</small>}{form.provider==='GMAIL'&&<small className="mail-provider-note">16자리 앱 비밀번호가 필요하며 개인 Gmail은 IMAP이 기본 활성화되어 별도 설정이 필요하지 않습니다.</small>}</label></div>
    <label>발신자명<input value={form.senderName} maxLength={100} placeholder="ZIK:00" onChange={event=>setForm({...form,senderName:event.target.value})}/></label>
    <label className="settings-active"><input type="checkbox" checked={form.active} onChange={event=>setForm({...form,active:event.target.checked})}/> 이 발신 계정 사용</label>
    <div className="mail-test-panel"><label>테스트 받을 이메일<input type="email" value={testRecipient} placeholder="받을 이메일 주소" onChange={event=>setTestRecipient(event.target.value)}/></label><button className="admin-blue-outline-button" type="button" disabled={testing} onClick={()=>void test()}><Send size={16}/>{testing?'발송 중':'저장 후 테스트 발송'}</button></div>
    <div className="settings-editor-footer"><span className="settings-secret-note">저장된 비밀번호는 암호화되며 화면에 다시 표시되지 않습니다.</span><button className="admin-blue-outline-button" type="button" disabled={saving} onClick={()=>void save()}><Save size={16}/>{saving?'저장 중':'저장'}</button></div>
    {message&&<p className="board-message">{message}</p>}
    <section className="mail-setup-guide" aria-labelledby="mail-setup-guide-title">
      <div className="mail-setup-guide-title"><Info size={18}/><div><h3 id="mail-setup-guide-title">발신 메일 준비 방법</h3><p>일반 로그인 비밀번호가 아닌 메일 서비스의 애플리케이션 비밀번호를 사용합니다.</p></div></div>
      <div className="mail-setup-guide-grid">
        <article>
          <div className="mail-setup-guide-heading"><strong>네이버 메일</strong><span>SMTP 587 · TLS</span></div>
          <p className="mail-setup-requirement"><b>필수:</b> 네이버 메일 환경설정에서 IMAP/SMTP를 사용함으로 설정</p>
          <ol>
            <li>네이버ID 보안설정에서 2단계 인증을 활성화합니다.</li>
            <li>2단계 인증 관리의 애플리케이션 비밀번호에서 <b>직접 입력</b>을 선택하고 <b>ZIK00 SMTP</b>로 생성합니다.</li>
            <li>네이버 메일 환경설정의 POP3/IMAP 설정에서 <b>IMAP/SMTP 사용함</b>을 저장합니다.</li>
            <li>회사 메일 주소에는 전체 네이버 주소, 메일 비밀번호에는 생성된 애플리케이션 비밀번호를 입력합니다.</li>
          </ol>
          <a href="https://help.naver.com/service/5640/contents/8584?lang=ko&osType=PC" target="_blank" rel="noreferrer">네이버 설정 도움말<ExternalLink size={14}/></a>
        </article>
        <article>
          <div className="mail-setup-guide-heading"><strong>Gmail</strong><span>SMTP 587 · TLS</span></div>
          <p className="mail-setup-requirement neutral"><b>IMAP:</b> 개인 Gmail은 기본 활성화되어 별도의 사용 설정 없음</p>
          <ol>
            <li>Google 계정 보안에서 2단계 인증을 활성화합니다.</li>
            <li>앱 비밀번호에서 <b>ZIK00 SMTP</b>를 만들고 생성된 16자리 비밀번호를 확인합니다.</li>
            <li>회사 메일 주소에는 전체 Gmail 주소, 메일 비밀번호에는 앱 비밀번호를 입력합니다.</li>
            <li>개인 Gmail은 IMAP이 기본 활성화되어 별도로 사용 설정을 변경하지 않아도 됩니다.</li>
          </ol>
          <a href="https://support.google.com/mail/answer/185833?hl=ko" target="_blank" rel="noreferrer">Google 설정 도움말<ExternalLink size={14}/></a>
        </article>
      </div>
    </section>
  </div></div></section>
}

function validate(form:MailAddressPayload,passwordConfigured:boolean){if(!form.provider)return '메일 서비스를 선택해주세요.';if(form.provider==='CUSTOM'&&!form.host.trim())return 'SMTP 서버 주소를 입력해주세요.';if(form.provider==='CUSTOM'&&(!Number.isInteger(form.port)||form.port<1||form.port>65535))return 'SMTP 포트를 확인해주세요.';if(!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.username.trim()))return '회사 메일 주소 형식이 올바르지 않습니다.';if(!passwordConfigured&&!form.password.trim())return '메일 비밀번호를 입력해주세요.';if(!form.senderName.trim())return '발신자명을 입력해주세요.';return ''}
function errorText(error:unknown){return error instanceof Error?error.message:'요청을 처리하지 못했습니다.'}
