import { type FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Bell, ChevronRight, CircleDollarSign, CreditCard, Gift, HelpCircle,
  ImagePlus, LoaderCircle, MapPin, MessageCircleQuestion, PackageCheck, Pencil,
  Plus, ReceiptText, Search, Trash2, Truck, UserRound, WalletCards, X,
} from 'lucide-react';
import {
  type AddressUpdatePayload, type Coupon, type Dashboard, type DeliveryAddress,
  type InquiryThread, type MypageProfile, type MypageSection, type ProfileData,
  type Purchase, type SectionData, addDeliveryAddress, createInquiry,
  deleteDeliveryAddress, getMypageDashboard, getMypageSection, updateDeliveryAddress,
  updateMypageProfile,
} from '../../api/mypage';
import { fetchAuthenticated, searchJapaneseAddress } from '../../api/auth';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

const menuItems: Array<{ section: MypageSection; label: string; description: string; icon: typeof UserRound }> = [
  { section: 'home', label: '마이페이지', description: '쇼핑 활동 요약', icon: UserRound },
  { section: 'orders', label: '구매내역', description: '주문 및 결제 내역', icon: ReceiptText },
  { section: 'deliveries', label: '배송조회', description: '배송 진행 상태', icon: Truck },
  { section: 'inquiries', label: '1:1 문의내역', description: '문의와 답변 확인', icon: MessageCircleQuestion },
  { section: 'coupons', label: '쿠폰함', description: '보유 쿠폰과 기간', icon: Gift },
  { section: 'deposits', label: '예치금 관리', description: '잔액과 적립 포인트', icon: WalletCards },
  { section: 'profile', label: '회원정보수정', description: '연락처와 배송지', icon: UserRound },
];

function currentSection(): MypageSection {
  const segment = window.location.pathname.replace(/^\/mypage\/?/, '').split('/')[0];
  return menuItems.some((item) => item.section === segment) ? segment as MypageSection : 'home';
}

const won = new Intl.NumberFormat('ko-KR');
const money = (value: number) => `${won.format(value)}원`;
const date = (value?: string) => value ? value.replaceAll('-', '.') : '-';

function EmptyState({ children }: { children: string }) {
  return <div className="mypage-empty"><PackageCheck size={34} /><p>{children}</p></div>;
}

function AuthenticatedImage({ src, alt }: { src: string; alt: string }) {
  const [objectUrl, setObjectUrl] = useState('');

  useEffect(() => {
    let active = true;
    let createdUrl = '';
    fetchAuthenticated(src)
      .then((response) => {
        if (!response.ok) throw new Error('이미지를 불러오지 못했습니다.');
        return response.blob();
      })
      .then((blob) => {
        if (!active) return;
        createdUrl = URL.createObjectURL(blob);
        setObjectUrl(createdUrl);
      })
      .catch(() => setObjectUrl(''));
    return () => {
      active = false;
      if (createdUrl) URL.revokeObjectURL(createdUrl);
    };
  }, [src]);

  return objectUrl ? <img src={objectUrl} alt={alt} loading="lazy" /> : <div className="inquiry-image-loading"><LoaderCircle className="spin" size={20} /></div>;
}

function OrderList({ orders }: { orders: Purchase[] }) {
  if (!orders.length) return <EmptyState>아직 구매내역이 없습니다.</EmptyState>;
  return <div className="mypage-order-list">{orders.map((order) => (
    <article className="mypage-order" key={`${order.orderNumber}-${order.productName}`}>
      <div className="order-date"><span>{date(order.orderedDate)}</span><small>{order.orderNumber}</small></div>
      <div className="order-product-mark"><PackageCheck size={26} /></div>
      <div className="order-product-copy"><strong>{order.productName}</strong><span>수량 {order.quantity}개</span></div>
      <strong className="order-price">{money(order.paymentAmount)}</strong>
      <span className="order-status">{order.orderStatus || '주문접수'}</span>
    </article>
  ))}</div>;
}

function CouponList({ coupons }: { coupons: Coupon[] }) {
  if (!coupons.length) return <EmptyState>사용할 수 있는 쿠폰이 없습니다.</EmptyState>;
  return <div className="mypage-coupon-grid">{coupons.map((coupon, index) => (
    <article className={`mypage-coupon ${coupon.used ? 'used' : ''}`} key={`${coupon.couponName}-${index}`}>
      <span className="coupon-label">ZIK:00 COUPON</span>
      <strong>{coupon.discountType === 'RATE' ? `${coupon.discountValue}%` : money(coupon.discountValue)}</strong>
      <h3>{coupon.couponName}</h3>
      <p>{money(coupon.minimumOrderAmount)} 이상 구매 시</p>
      <footer><span>{date(coupon.startedDate)} ~ {date(coupon.expiredDate)}</span><b>{coupon.used ? '사용완료' : '사용가능'}</b></footer>
    </article>
  ))}</div>;
}

function InquirySection({ threads, onCreated }: { threads: InquiryThread[]; onCreated: () => Promise<void> }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [images, setImages] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setFormError('');
    setNotice('');
    try {
      await createInquiry(title, content, images);
      await onCreated();
      setTitle('');
      setContent('');
      setImages([]);
      setOpen(false);
      setNotice('문의가 등록되었습니다.');
    } catch (reason) {
      setFormError(reason instanceof Error ? reason.message : '문의를 등록하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const selectImages = (files: FileList | null) => {
    const selected = Array.from(files ?? []);
    if (selected.length > 3) {
      setFormError('이미지는 최대 3개까지 첨부할 수 있습니다.');
      return;
    }
    setFormError('');
    setImages(selected);
  };

  return <section className="mypage-content-panel inquiry-panel">
    <header className="inquiry-panel-heading">
      <div><span>MY INQUIRY</span><h2>문의내역</h2></div>
      <button type="button" onClick={() => { setOpen((current) => !current); setFormError(''); setNotice(''); }}>
        {open ? <X size={17} /> : <Plus size={17} />}{open ? '작성 취소' : '문의 등록'}
      </button>
    </header>
    {notice && <p className="inquiry-notice" role="status">{notice}</p>}
    {open && <form className="inquiry-create-form" onSubmit={submit}>
      <div className="inquiry-form-heading"><strong>새 문의 작성</strong><span>문의 내용을 확인한 후 순서대로 답변드립니다.</span></div>
      {formError && <p className="inquiry-form-error" role="alert">{formError}</p>}
      <label><span>제목 <b>*</b></span><input required maxLength={255} value={title} onChange={(event) => setTitle(event.target.value)} placeholder="문의 제목을 입력해주세요." /></label>
      <label><span>문의 내용 <b>*</b></span><textarea required maxLength={5000} value={content} onChange={(event) => setContent(event.target.value)} placeholder="문의 내용을 자세히 입력해주세요." /><small>{content.length.toLocaleString()} / 5,000</small></label>
      <div className="inquiry-upload-row">
        <label className="inquiry-file-button"><ImagePlus size={18} /><span>이미지 첨부</span><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple onChange={(event) => selectImages(event.target.files)} /></label>
        <span>최대 3개 · 파일당 5MB</span>
      </div>
      {images.length > 0 && <ul className="inquiry-file-list">{images.map((image) => <li key={`${image.name}-${image.lastModified}`}><span>{image.name}</span><button type="button" aria-label={`${image.name} 삭제`} onClick={() => setImages((current) => current.filter((item) => item !== image))}><X size={14} /></button></li>)}</ul>}
      <div className="inquiry-form-actions"><button type="button" onClick={() => setOpen(false)}>취소</button><button type="submit" disabled={submitting}>{submitting ? <><LoaderCircle className="spin" size={17} /> 등록 중</> : '문의 등록'}</button></div>
    </form>}
    {threads.length ? <div className="inquiry-list">{threads.map((thread) => <details key={thread.inquiry.inquiryId}><summary><span className={thread.inquiry.status ? 'answered' : ''}>{thread.inquiry.status ? '답변완료' : '답변대기'}</span><strong>{thread.inquiry.title}</strong><time>{thread.inquiry.createdAt}</time><ChevronRight size={18} /></summary><div className="inquiry-body"><p>{thread.inquiry.content}</p>{thread.images.length > 0 && <div className="inquiry-image-grid">{thread.images.map((image) => <div className="inquiry-image-item" key={image.imageUuid}><AuthenticatedImage src={image.imageUrl} alt={`${thread.inquiry.title} 첨부 이미지`} /></div>)}</div>}{thread.comments.map((comment) => <article key={comment.commentId}><b>{comment.writerType === 'ADMIN' ? 'ZIK:00 답변' : comment.writerName}</b><p>{comment.content}</p>{comment.images.length > 0 && <div className="inquiry-image-grid">{comment.images.map((image) => <div className="inquiry-image-item" key={image.imageUuid}><AuthenticatedImage src={image.imageUrl} alt="문의 답변 첨부 이미지" /></div>)}</div>}<small>{comment.createdAt}</small></article>)}</div></details>)}</div> : <EmptyState>등록한 문의가 없습니다.</EmptyState>}
  </section>;
}

function ProfileEditor({ profile, onSaved }: { profile: MypageProfile; onSaved: () => Promise<void> }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(profile);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      await updateMypageProfile(form);
      await onSaved();
      setEditing(false);
      setMessage('회원정보가 수정되었습니다.');
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : '회원정보를 저장하지 못했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return <section className="mypage-content-panel profile-card">
    <header><div><span>MEMBER PROFILE</span><h2>회원정보</h2></div><button className="mypage-edit-button" type="button" onClick={() => { setForm(profile); setEditing((current) => !current); setMessage(''); }}><Pencil size={15} />{editing ? '수정 취소' : '회원정보 수정'}</button></header>
    {message && <p className="profile-form-message">{message}</p>}
    {editing ? <form className="profile-edit-form" onSubmit={save}>
      <label><span>이름</span><input required maxLength={100} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
      <label><span>닉네임</span><input required maxLength={100} value={form.nickname} onChange={(event) => setForm({ ...form, nickname: event.target.value })} /></label>
      <label><span>일반전화</span><input required type="tel" placeholder="02-123-1234" value={form.telephone} onChange={(event) => setForm({ ...form, telephone: event.target.value })} /></label>
      <label><span>휴대전화</span><input required type="tel" placeholder="090-1234-1234" value={form.mobilePhone} onChange={(event) => setForm({ ...form, mobilePhone: event.target.value })} /></label>
      <label className="profile-email-field"><span>이메일</span><input required type="email" maxLength={255} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></label>
      <label className="profile-checkbox-field"><input type="checkbox" checked={form.alarmConsent} onChange={(event) => setForm({ ...form, alarmConsent: event.target.checked })} /><span>이벤트 및 알림 수신 동의</span></label>
      <div className="profile-form-actions"><button type="button" onClick={() => setEditing(false)}>취소</button><button type="submit" disabled={saving}>{saving ? '저장 중' : '저장'}</button></div>
    </form> : <dl><div><dt>이름</dt><dd>{profile.name}</dd></div><div><dt>닉네임</dt><dd>{profile.nickname}</dd></div><div><dt>일반전화</dt><dd>{profile.telephone}</dd></div><div><dt>휴대전화</dt><dd>{profile.mobilePhone}</dd></div><div><dt>이메일</dt><dd>{profile.email}</dd></div><div><dt>알림 수신</dt><dd>{profile.alarmConsent ? '동의' : '미동의'}</dd></div></dl>}
  </section>;
}

const emptyAddress = (profile: MypageProfile): AddressUpdatePayload => ({
  addressName: '', receiverName: profile.name, receiverPhone: profile.mobilePhone,
  zipCode: '', province: '', baseAddress: '', detailAddress: '', defaultAddress: false,
});

function AddressManager({ profile, addresses, onSaved }: { profile: MypageProfile; addresses: DeliveryAddress[]; onSaved: () => Promise<void> }) {
  const [managing, setManaging] = useState(false);
  const [editingId, setEditingId] = useState<number | 'new' | null>(null);
  const [form, setForm] = useState<AddressUpdatePayload>(emptyAddress(profile));
  const [postalQuery, setPostalQuery] = useState('');
  const [addressResults, setAddressResults] = useState<Array<{ zipCode: string; province: string; detailAddress: string }>>([]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  const startNew = () => { setForm(emptyAddress(profile)); setEditingId('new'); setPostalQuery(''); setAddressResults([]); setMessage(''); };
  const startEdit = (address: DeliveryAddress) => {
    setForm({ addressName: address.addressName, receiverName: address.receiverName, receiverPhone: address.receiverPhone, zipCode: '', province: '', baseAddress: '', detailAddress: address.detailAddress, defaultAddress: address.defaultAddress });
    setEditingId(address.id); setPostalQuery(''); setAddressResults([]); setMessage('');
  };
  const searchAddress = async () => {
    const postalCode = postalQuery.replace(/\D/g, '');
    if (postalCode.length !== 7) { setMessage('7자리 일본 우편번호를 입력해주세요.'); return; }
    try { setAddressResults(await searchJapaneseAddress(postalCode)); setMessage(''); } catch { setMessage('주소를 조회하지 못했습니다.'); }
  };
  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setSaving(true); setMessage('');
    try {
      if (editingId === 'new') await addDeliveryAddress(form);
      else if (typeof editingId === 'number') await updateDeliveryAddress(editingId, form);
      await onSaved(); setEditingId(null); setMessage('배송지가 저장되었습니다.');
    } catch (reason) { setMessage(reason instanceof Error ? reason.message : '배송지를 저장하지 못했습니다.'); }
    finally { setSaving(false); }
  };
  const remove = async (addressId: number) => {
    if (!window.confirm('이 배송지를 삭제하시겠습니까?')) return;
    try { await deleteDeliveryAddress(addressId); await onSaved(); setEditingId(null); setMessage('배송지가 삭제되었습니다.'); }
    catch (reason) { setMessage(reason instanceof Error ? reason.message : '배송지를 삭제하지 못했습니다.'); }
  };

  return <section className="mypage-content-panel address-card">
    <header><div><span>DELIVERY ADDRESS</span><h2>배송지</h2></div><button className="mypage-edit-button" type="button" onClick={() => { setManaging((current) => !current); setEditingId(null); setMessage(''); }}><Pencil size={15} />{managing ? '수정 완료' : '배송지 수정'}</button></header>
    {message && <p className="profile-form-message">{message}</p>}
    {addresses.length ? <div className="address-list-react">{addresses.map((address) => <article key={address.id}><div><MapPin size={20} /><strong>{address.addressName}</strong>{address.defaultAddress && <b>기본</b>}</div><p>{address.receiverName} · {address.receiverPhone}</p><p>〒{address.zipCode} {address.province} {address.detailAddress}</p>{managing && <div className="address-item-actions"><button type="button" onClick={() => startEdit(address)}><Pencil size={13} />수정</button><button type="button" onClick={() => remove(address.id)}><Trash2 size={13} />삭제</button></div>}</article>)}</div> : !editingId && <EmptyState>등록된 배송지가 없습니다.</EmptyState>}
    {managing && editingId === null && <button className="address-add-button" type="button" onClick={startNew}><Plus size={16} />새 배송지 등록</button>}
    {managing && editingId !== null && <form className="address-edit-form-react" onSubmit={save}>
      <div className="address-form-grid"><label><span>배송지명</span><input required maxLength={100} value={form.addressName} onChange={(event) => setForm({ ...form, addressName: event.target.value })} placeholder="집" /></label><label><span>수령인</span><input required maxLength={100} value={form.receiverName} onChange={(event) => setForm({ ...form, receiverName: event.target.value })} /></label><label><span>수령인 연락처</span><input required type="tel" maxLength={50} value={form.receiverPhone} onChange={(event) => setForm({ ...form, receiverPhone: event.target.value })} /></label></div>
      <div className="address-postal-search"><label><span>일본 우편번호</span><input value={postalQuery} onChange={(event) => setPostalQuery(event.target.value)} placeholder="100-0005" inputMode="numeric" /></label><button type="button" onClick={searchAddress}><Search size={15} />주소 조회</button></div>
      {addressResults.length > 0 && <div className="address-search-results-react">{addressResults.map((result) => <button type="button" key={`${result.zipCode}-${result.detailAddress}`} onClick={() => { setForm({ ...form, zipCode: result.zipCode, province: result.province, baseAddress: result.detailAddress, detailAddress: '' }); setAddressResults([]); }}><strong>{result.zipCode}</strong><span>{result.province} {result.detailAddress}</span></button>)}</div>}
      {form.baseAddress && <p className="selected-address">선택 주소: 〒{form.zipCode} {form.province} {form.baseAddress}</p>}
      {editingId !== 'new' && !form.baseAddress && <p className="selected-address">주소를 새로 조회하지 않으면 기존 주소가 유지됩니다.</p>}
      <label className="address-detail-field"><span>상세 주소</span><input maxLength={255} required value={form.detailAddress} onChange={(event) => setForm({ ...form, detailAddress: event.target.value })} placeholder="건물명, 호수" /></label>
      <label className="profile-checkbox-field"><input type="checkbox" checked={form.defaultAddress} onChange={(event) => setForm({ ...form, defaultAddress: event.target.checked })} /><span>기본 배송지로 설정</span></label>
      <div className="profile-form-actions"><button type="button" onClick={() => setEditingId(null)}>취소</button><button type="submit" disabled={saving}>{saving ? '저장 중' : '배송지 저장'}</button></div>
    </form>}
  </section>;
}

function MypagePage() {
  const section = useMemo(currentSection, []);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [sectionData, setSectionData] = useState<SectionData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const active = menuItems.find((item) => item.section === section) ?? menuItems[0];

  useEffect(() => {
    setLoading(true);
    Promise.all([
      getMypageDashboard(),
      section === 'home' ? Promise.resolve(null) : getMypageSection(section),
    ])
      .then(([dashboardResult, pageResult]) => {
        setDashboard(dashboardResult);
        setSectionData(pageResult);
      })
      .catch((reason: unknown) => setError(reason instanceof Error ? reason.message : '정보를 불러오지 못했습니다.'))
      .finally(() => setLoading(false));
  }, [section]);

  const refreshProfile = async () => {
    const [profileResult, dashboardResult] = await Promise.all([
      getMypageSection('profile'),
      getMypageDashboard(),
    ]);
    setSectionData(profileResult);
    setDashboard(dashboardResult);
  };

  const renderContent = () => {
    if (!dashboard) return null;
    if (section === 'home') {
      const cards = [
        ['구매 완료', dashboard.summary.completedOrderCount, '건', '/mypage/orders', ReceiptText],
        ['배송 중', dashboard.summary.deliveryTrackingCount, '건', '/mypage/deliveries', Truck],
        ['1:1 문의', dashboard.summary.inquiryCount, '건', '/mypage/inquiries', HelpCircle],
        ['보유 쿠폰', dashboard.summary.couponCount, '장', '/mypage/coupons', Gift],
      ] as const;
      return <>
        <div className="mypage-summary-grid">{cards.map(([label, value, unit, href, Icon]) => (
          <a href={href} className="mypage-summary-card" key={label}><span><Icon size={22} /></span><div><small>{label}</small><strong>{won.format(value)}<em>{unit}</em></strong></div><ChevronRight size={18} /></a>
        ))}</div>
        <div className="mypage-balance-strip">
          <div><CircleDollarSign size={22} /><span>예치금</span><strong>{money(dashboard.summary.depositBalance)}</strong></div>
          <i />
          <div><CreditCard size={22} /><span>적립 포인트</span><strong>{won.format(dashboard.summary.rewardPoint)}P</strong></div>
          <a href="/mypage/deposits">자세히 보기 <ChevronRight size={16} /></a>
        </div>
        <section className="mypage-content-panel"><header><div><span>RECENT ORDERS</span><h2>최근 구매내역</h2></div><a href="/mypage/orders">전체보기 <ChevronRight size={16} /></a></header><OrderList orders={dashboard.recentOrders.slice(0, 3)} /></section>
      </>;
    }
    if (section === 'orders') return <section className="mypage-content-panel"><OrderList orders={(sectionData ?? []) as Purchase[]} /></section>;
    if (section === 'deliveries') {
      const orders = (sectionData ?? []) as Purchase[];
      return <section className="mypage-content-panel"><div className="delivery-guide"><Truck size={25} /><div><strong>배송 진행 상황</strong><p>주문 상품의 현재 처리 상태를 확인할 수 있습니다.</p></div></div><OrderList orders={orders} /></section>;
    }
    if (section === 'coupons') return <CouponList coupons={(sectionData ?? []) as Coupon[]} />;
    if (section === 'deposits') return <div className="mypage-wallet-grid"><article><span><WalletCards size={24} /> 현재 예치금</span><strong>{money(dashboard.summary.depositBalance)}</strong><p>상품 결제 시 현금처럼 사용할 수 있습니다.</p></article><article><span><Gift size={24} /> 적립 포인트</span><strong>{won.format(dashboard.summary.rewardPoint)}P</strong><p>구매 및 이벤트 참여로 적립됩니다.</p></article></div>;
    if (section === 'inquiries') {
      const threads = (sectionData ?? []) as InquiryThread[];
      return <InquirySection threads={threads} onCreated={async () => setSectionData(await getMypageSection('inquiries'))} />;
    }
    const profileData = sectionData as ProfileData | null;
    const profile = profileData?.profile ?? dashboard.profile;
    return <div className="profile-layout"><ProfileEditor profile={profile} onSaved={refreshProfile} /><AddressManager profile={profile} addresses={profileData?.addresses ?? []} onSaved={refreshProfile} /></div>;
  };

  return <div className="app-shell"><SiteHeader /><main className="mypage-main"><div className="header-inner"><div className="mypage-heading"><div><p>MY ZIK:00</p><h1>{active.label}</h1><span>{active.description}</span></div><div className="mypage-heading-user"><strong>{dashboard?.profile.nickname || '회원'}님</strong><small>반갑습니다.</small></div></div><div className="mypage-shell"><aside className="mypage-sidebar"><nav>{menuItems.map(({ section: itemSection, label, icon: Icon }) => <a className={section === itemSection ? 'active' : ''} href={itemSection === 'home' ? '/mypage' : `/mypage/${itemSection}`} key={itemSection}><Icon size={19} /><span>{label}</span><ChevronRight size={16} /></a>)}</nav><div className="mypage-help"><Bell size={20} /><strong>도움이 필요하신가요?</strong><a href="/#support">고객센터 바로가기</a></div></aside><div className="mypage-content">{loading ? <div className="mypage-loading"><LoaderCircle className="spin" /><span>회원정보를 불러오고 있습니다.</span></div> : error ? <div className="mypage-error">{error}</div> : renderContent()}</div></div></div></main><SiteFooter /></div>;
}

export default MypagePage;
