import { Calculator, Headphones, PackageCheck, Search } from 'lucide-react';

function QuickMenu() {
  return (
    <aside className="quick-menu" aria-label="빠른 메뉴">
      <a href="#delivery"><Search size={20} /><span>현지 배송조회</span></a>
      <a href="#schedule"><PackageCheck size={20} /><span>출고 일정</span></a>
      <a href="#estimate"><Calculator size={20} /><span>예상비용 계산</span></a>
      <a href="#support"><Headphones size={20} /><span>1:1 문의</span></a>
      <a href="#top" className="to-top">TOP</a>
    </aside>
  );
}

export default QuickMenu;
