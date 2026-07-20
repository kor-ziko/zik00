import { type FormEvent, useEffect, useMemo, useRef, useState } from 'react';
import { ArrowLeft, Check, ChevronRight, LoaderCircle, MapPin, Search, UserRound } from 'lucide-react';
import {
  ApiError,
  type RegistrationDetailPayload,
  type AddressResult,
  getRegistrationDetailSession,
  searchJapaneseAddress,
  submitRegistrationDetail,
} from '../../api/auth';
import AuthShell from './AuthShell';

const prefectures = [
  '北海道', '青森県', '岩手県', '宮城県', '秋田県', '山形県', '福島県',
  '茨城県', '栃木県', '群馬県', '埼玉県', '千葉県', '東京都', '神奈川県',
  '新潟県', '富山県', '石川県', '福井県', '山梨県', '長野県', '岐阜県',
  '静岡県', '愛知県', '三重県', '滋賀県', '京都府', '大阪府', '兵庫県',
  '奈良県', '和歌山県', '鳥取県', '島根県', '岡山県', '広島県', '山口県',
  '徳島県', '香川県', '愛媛県', '高知県', '福岡県', '佐賀県', '長崎県',
  '熊本県', '大分県', '宮崎県', '鹿児島県', '沖縄県',
];

const initialForm: RegistrationDetailPayload = {
  nameKanji: '', nameKatakana: '', birthDate: '', gender: '', nickname: '',
  zipCode: '', province: '', baseAddress: '', detailAddress: '',
  telephone: '', mobilePhone: '',
};

function formatTelephone(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 10);
  if (digits.startsWith('02') && digits.length === 9) return `${digits.slice(0, 2)}-${digits.slice(2, 5)}-${digits.slice(5)}`;
  if (digits.startsWith('02') && digits.length === 10) return `${digits.slice(0, 2)}-${digits.slice(2, 6)}-${digits.slice(6)}`;
  if (digits.length === 10) return `${digits.slice(0, 3)}-${digits.slice(3, 6)}-${digits.slice(6)}`;
  return value;
}

function formatMobilePhone(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11);
  return digits.length === 11 ? `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}` : value;
}

function yesterday() {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return date.toISOString().slice(0, 10);
}

function RegistrationDetailPage() {
  const [form, setForm] = useState(initialForm);
  const [addresses, setAddresses] = useState<AddressResult[]>([]);
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionError, setSessionError] = useState('');
  const [searchingAddress, setSearchingAddress] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<string[]>([]);
  const [addressMessage, setAddressMessage] = useState('');
  const submittingRef = useRef(false);
  const maxBirthDate = useMemo(yesterday, []);

  useEffect(() => {
    getRegistrationDetailSession()
      .catch((error) => {
        if (error instanceof ApiError && error.status === 409) {
          window.location.replace('/login/terms');
          return;
        }
        if (error instanceof ApiError && error.status === 401) {
          window.location.replace('/login?reason=registration-expired');
          return;
        }
        setSessionError('가입 정보를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.');
      })
      .finally(() => setCheckingSession(false));
  }, []);

  const update = (name: keyof RegistrationDetailPayload, value: string) => {
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleAddressSearch = async () => {
    const postalCode = form.zipCode.replace(/\D/g, '');
    setErrors([]);
    setAddresses([]);
    if (postalCode.length !== 7) {
      setAddressMessage('7자리 일본 우편번호를 입력해주세요. 예: 1000005');
      return;
    }
    setSearchingAddress(true);
    setAddressMessage('주소를 조회하고 있습니다.');
    try {
      const results = await searchJapaneseAddress(postalCode);
      setAddresses(results);
      if (results.length === 0) setAddressMessage('해당 우편번호의 주소를 찾지 못했습니다.');
      else if (results.length === 1) selectAddress(results[0]);
      else setAddressMessage('주소가 여러 개 검색되었습니다. 알맞은 주소를 선택해주세요.');
    } catch {
      setAddressMessage('주소 조회에 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setSearchingAddress(false);
    }
  };

  const selectAddress = (address: AddressResult) => {
    setForm((current) => ({
      ...current,
      zipCode: address.zipCode,
      province: address.province,
      baseAddress: address.detailAddress,
      detailAddress: '',
    }));
    setAddresses([]);
    setAddressMessage('조회한 주소를 입력했습니다. 나머지 상세 주소를 작성해주세요.');
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submittingRef.current) return;
    submittingRef.current = true;
    setErrors([]);
    setSubmitting(true);
    try {
      await submitRegistrationDetail(form);
      window.location.replace('/');
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 401) {
          window.location.replace('/login?reason=registration-expired');
          return;
        }
        setErrors(error.messages);
      } else {
        setErrors(['회원정보 저장 중 오류가 발생했습니다. 다시 시도해주세요.']);
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  if (checkingSession) {
    return <div className="auth-loading"><LoaderCircle className="spin" aria-hidden="true" /><span>로그인 정보를 확인하고 있습니다.</span></div>;
  }

  if (sessionError) {
    return (
      <AuthShell>
        <main className="auth-container oauth-callback-page">
          <h1>가입 정보를 확인하지 못했습니다.</h1>
          <p className="form-alert" role="alert">{sessionError}</p>
          <a href="/login">로그인으로 돌아가기</a>
        </main>
      </AuthShell>
    );
  }

  return (
    <AuthShell step="회원가입 3 / 4">
      <main className="auth-container additional-layout">
        <header className="additional-heading">
          <div><p className="auth-kicker">COMPLETE YOUR PROFILE</p><h1>회원정보를 완성해주세요.</h1></div>
          <p>첫 로그인에 한 번만 입력합니다.<br />입력한 주소는 기본 배송지로 저장됩니다.</p>
        </header>

        <div className="signup-progress" aria-label="가입 진행 단계">
          <span className="done"><Check size={14} /> Google 인증</span>
          <i />
          <span className="done"><Check size={14} /> 약관동의</span>
          <i />
          <span className="active">추가정보 입력</span>
          <i />
          <span>가입 완료</span>
        </div>

        <form className="additional-form" onSubmit={handleSubmit}>
          {errors.length > 0 && <div className="form-alert error-list" role="alert">{errors.map((error) => <p key={error}>{error}</p>)}</div>}

          <section className="form-section" aria-labelledby="personal-title">
            <div className="form-section-heading"><span><UserRound size={21} /></span><div><h2 id="personal-title">기본 정보</h2><p>수령인 확인에 필요한 정보를 입력해주세요.</p></div></div>
            <div className="form-grid form-grid-two">
              <label><span>이름(한자) <b>*</b></span><input required maxLength={100} value={form.nameKanji} onChange={(e) => update('nameKanji', e.target.value)} placeholder="山田 太郎" autoComplete="name" /></label>
              <label><span>이름(카타카나) <b>*</b></span><input required maxLength={100} value={form.nameKatakana} onChange={(e) => update('nameKatakana', e.target.value)} placeholder="ヤマダ タロウ" /></label>
              <label><span>생년월일 <b>*</b></span><input required type="date" max={maxBirthDate} value={form.birthDate} onChange={(e) => update('birthDate', e.target.value)} /></label>
              <fieldset className="gender-field"><legend>성별 <b>*</b></legend><div className="gender-options">{['남자','여자','기타'].map((value) => <label key={value} className={form.gender === value ? 'selected' : ''}><input required type="radio" name="gender" value={value} checked={form.gender === value} onChange={(e) => update('gender', e.target.value)} /><span>{value}</span></label>)}</div></fieldset>
              <label><span>닉네임 <b>*</b></span><input required maxLength={100} value={form.nickname} onChange={(e) => update('nickname', e.target.value)} placeholder="ZIK:00에서 사용할 이름" /></label>
              <label><span>일반전화 <b>*</b></span><input required type="tel" maxLength={13} value={form.telephone} onChange={(e) => update('telephone', e.target.value)} onBlur={(e) => update('telephone', formatTelephone(e.target.value))} placeholder="02-123-1234" autoComplete="tel" /></label>
              <label><span>휴대전화 <b>*</b></span><input required type="tel" maxLength={13} value={form.mobilePhone} onChange={(e) => update('mobilePhone', e.target.value)} onBlur={(e) => update('mobilePhone', formatMobilePhone(e.target.value))} placeholder="090-1234-1234" autoComplete="tel-national" /></label>
            </div>
          </section>

          <section className="form-section" aria-labelledby="address-title">
            <div className="form-section-heading"><span><MapPin size={21} /></span><div><h2 id="address-title">기본 배송지</h2><p>일본 내 상품을 받을 주소를 등록해주세요.</p></div></div>
            <div className="address-fields">
              <div className="postal-search-row">
                <label><span>우편번호 <b>*</b></span><input required value={form.zipCode} onChange={(e) => { update('zipCode', e.target.value); update('baseAddress', ''); }} placeholder="100-0005" inputMode="numeric" /></label>
                <button type="button" onClick={handleAddressSearch} disabled={searchingAddress}>{searchingAddress ? <LoaderCircle className="spin" size={18} /> : <Search size={18} />} 주소 조회</button>
              </div>
              {addressMessage && <p className="address-message" aria-live="polite">{addressMessage}</p>}
              {addresses.length > 0 && <div className="address-result-list">{addresses.map((address) => <button type="button" key={`${address.zipCode}-${address.detailAddress}`} onClick={() => selectAddress(address)}><MapPin size={17} /><span><strong>{address.zipCode}</strong>{address.province} {address.detailAddress}</span><ChevronRight size={17} /></button>)}</div>}
              <div className="form-grid">
                <label><span>도도부현 <b>*</b></span><select required value={form.province} onChange={(e) => update('province', e.target.value)}><option value="">선택해주세요</option>{prefectures.map((item) => <option value={item} key={item}>{item}</option>)}</select></label>
                <label><span>조회 주소 <b>*</b></span><input required readOnly value={form.baseAddress} placeholder="우편번호로 주소를 조회해주세요" /></label>
                <label><span>상세 주소 <b>*</b></span><input required maxLength={150} value={form.detailAddress} onChange={(e) => update('detailAddress', e.target.value)} placeholder="1-2-3 ○○マンション 101号室" autoComplete="street-address" /></label>
              </div>
            </div>
          </section>

          <div className="form-submit-row">
            <a className="detail-back-button" href="/login/terms"><ArrowLeft size={18} /> 약관동의로 돌아가기</a>
            <button className="complete-button" type="submit" disabled={submitting}>{submitting ? <><LoaderCircle className="spin" size={19} /> 저장 중</> : <>가입 완료 <ChevronRight size={20} /></>}</button>
          </div>
        </form>
      </main>
    </AuthShell>
  );
}

export default RegistrationDetailPage;
