import Calculator from 'lucide-react/dist/esm/icons/calculator.js';
import Clock3 from 'lucide-react/dist/esm/icons/clock-3.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';

function ServiceStrip() {
  return (
    <section className="service-strip" aria-label="서비스 특징">
      <div><PackageCheck size={22} /><span><strong>검수부터 포장까지</strong>안전한 배송 대행</span></div>
      <div><Calculator size={22} /><span><strong>비용을 한눈에</strong>예상 금액 미리 확인</span></div>
      <div><Clock3 size={22} /><span><strong>진행 상황 확인</strong>주문부터 배송까지</span></div>
    </section>
  );
}

export default ServiceStrip;
