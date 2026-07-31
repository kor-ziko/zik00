import Calculator from 'lucide-react/dist/esm/icons/calculator.js';
import Headphones from 'lucide-react/dist/esm/icons/headphones.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import Search from 'lucide-react/dist/esm/icons/search.js';
import { useAuthMemory } from '../../auth/AuthMemory';
import { loginHref } from '../../auth/authNavigation';
import { useLocale } from '../../locale';

function QuickMenu() {
  const { accessSessionActive } = useAuthMemory();
  const { copy } = useLocale();
  const deliveryHref = accessSessionActive ? '/#delivery' : loginHref('/#delivery');
  const scheduleHref = accessSessionActive ? '/#schedule' : loginHref('/#schedule');
  const inquiryHref = accessSessionActive ? '/mypage/inquiries' : loginHref('/mypage/inquiries');

  return (
    <aside className="quick-menu" aria-label="빠른 메뉴">
      <a href={deliveryHref}><Search size={20} /><span>{copy.quick[0]}</span></a>
      <a href={scheduleHref}><PackageCheck size={20} /><span>{copy.quick[1]}</span></a>
      <a href="#estimate"><Calculator size={20} /><span>{copy.quick[2]}</span></a>
      <a href={inquiryHref}><Headphones size={20} /><span>{copy.quick[3]}</span></a>
      <a href="#top" className="to-top">TOP</a>
    </aside>
  );
}

export default QuickMenu;
