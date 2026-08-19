import HelpCircle from 'lucide-react/dist/esm/icons/circle-help.js';
import { useAuthMemory } from '../../auth/AuthMemory';
import { loginHref } from '../../auth/authNavigation';
import { useLocale } from '../../locale';
import { Brand } from './SiteHeader';
import { useHomepageContent } from '../../homepage/HomepageContentContext';

function SiteFooter() {
  const { copy } = useLocale();
  const { accessSessionActive } = useAuthMemory();
  const supportHref = accessSessionActive ? '/mypage/inquiries' : loginHref('/mypage/inquiries');
  const managed = useHomepageContent('FOOTER_COPYRIGHT')[0];

  return (
    <footer className="site-footer">
      <div className="header-inner footer-links">
        <a href="/service-intro">{copy.footer.about}</a>
        <a href="#terms">{copy.footer.terms}</a>
        <a href="#privacy"><strong>{copy.footer.privacy}</strong></a>
      </div>
      <div className="header-inner footer-main">
        <div className="footer-brand"><Brand /></div>
        <div>
          <p>{managed?.title || copy.footer.intro}</p>
          <p className="company-info">{managed?.subtitle || copy.footer.company}</p>
          <p className="copyright">{managed?.content || copy.footer.copyright}</p>
        </div>
        <a className="support-call" href={supportHref}>
          <HelpCircle size={16} aria-hidden="true" />
          {copy.header.support}
        </a>
      </div>
    </footer>
  );
}

export default SiteFooter;
