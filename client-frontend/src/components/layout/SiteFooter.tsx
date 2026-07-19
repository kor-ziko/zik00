import { HelpCircle } from 'lucide-react';
import { Brand } from './SiteHeader';

function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="header-inner footer-links">
        <a href="#about">회사소개</a>
        <a href="#terms">이용약관</a>
        <a href="#privacy"><strong>개인정보처리방침</strong></a>
        <a href="#support">고객센터</a>
      </div>
      <div className="header-inner footer-main">
        <div className="footer-brand"><Brand /></div>
        <div>
          <p>한국의 좋은 상품을 일본까지 편리하게 연결합니다.</p>
          <p className="company-info">상호명: ZIK:00 · 고객센터: 02-0000-0000 · 운영시간: 평일 10:00–17:00</p>
          <p className="copyright">© 2026 ZIK:00. All rights reserved.</p>
        </div>
        <a className="support-call" href="#support"><HelpCircle size={20} />고객센터</a>
      </div>
    </footer>
  );
}

export default SiteFooter;
