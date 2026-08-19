import CheckCircle2 from 'lucide-react/dist/esm/icons/check-circle-2.js';
import CircleX from 'lucide-react/dist/esm/icons/circle-x.js';
import Clock3 from 'lucide-react/dist/esm/icons/clock-3.js';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

function PaymentCompletePage() {
  const params = new URLSearchParams(window.location.search);
  const paymentId = params.get('paymentId') || '';
  const result = params.get('status');
  const message = params.get('message') || '결제가 취소되었거나 승인되지 않았습니다.';
  const status = result === 'paid' ? 'success' : result === 'pending' ? 'pending' : 'error';

  return <div className="app-shell shopping-page-shell"><SiteHeader /><main className="payment-result-page header-inner">
    {status === 'success' ? <section><CheckCircle2 size={48} /><h1>결제가 완료되었습니다.</h1><p>주문 내역에서 처리 상태를 확인할 수 있습니다.</p><small>주문번호 {paymentId}</small><div><a href="/mypage/orders">주문 내역 보기</a><a href="/">홈으로</a></div></section> : status === 'pending' ? <section className="is-pending"><Clock3 size={48} /><h1>결제가 접수되었습니다.</h1><p>입금 확인이 필요한 결제수단입니다. 결제가 확인되면 주문이 확정됩니다.</p><small>주문번호 {paymentId}</small><div><a href="/mypage/orders">주문 내역 보기</a><a href="/">홈으로</a></div></section> : <section className="is-error"><CircleX size={48} /><h1>결제를 완료하지 못했습니다.</h1><p>{message}</p><div><a href="/cart">장바구니로 돌아가기</a><a href="/">홈으로</a></div></section>}
  </main><SiteFooter /></div>;
}

export default PaymentCompletePage;
