import ChevronDown from 'lucide-react/dist/esm/icons/chevron-down.js';
import Globe2 from 'lucide-react/dist/esm/icons/globe-2.js';
import Heart from 'lucide-react/dist/esm/icons/heart.js';
import ShoppingBag from 'lucide-react/dist/esm/icons/shopping-bag.js';
import Truck from 'lucide-react/dist/esm/icons/truck.js';
import UserRound from 'lucide-react/dist/esm/icons/user-round.js';
import { useCallback, useEffect, useRef, useState } from 'react';
import { type AuthSession, getAuthSession, logout } from '../../api/auth';
import { useAuthMemory } from '../../auth/AuthMemory';
import { loginHref } from '../../auth/authNavigation';
import { type Locale, useLocale } from '../../locale';
import SearchBox from '../search/SearchBox';
import CategoryMegaMenu from './CategoryMegaMenu';

function Brand() {
  const { copy } = useLocale();
  return (
    <a className="brand" href="/#top" aria-label={`ZIK:00 ${copy.header.navigation[0]}`}>
      <span>ZIK</span><strong>:00</strong>
    </a>
  );
}

function SiteHeader() {
  const { locale, setLocale, copy } = useLocale();
  const [session, setSession] = useState<AuthSession | null>(null);
  const [sessionChecked, setSessionChecked] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [languageOpen, setLanguageOpen] = useState(false);
  const languageRef = useRef<HTMLDivElement>(null);
  const { accessSessionActive } = useAuthMemory();

  const checkSession = useCallback(() => {
    getAuthSession()
      .then(setSession)
      .catch(() => setSession(null))
      .finally(() => setSessionChecked(true));
  }, []);

  useEffect(() => {
    checkSession();

    const checkVisibleSession = () => {
      if (document.visibilityState === 'visible') checkSession();
    };
    window.addEventListener('focus', checkSession);
    window.addEventListener('pageshow', checkSession);
    document.addEventListener('visibilitychange', checkVisibleSession);
    return () => {
      window.removeEventListener('focus', checkSession);
      window.removeEventListener('pageshow', checkSession);
      document.removeEventListener('visibilitychange', checkVisibleSession);
    };
  }, [checkSession]);

  useEffect(() => {
    if (sessionChecked && session?.authenticated && !accessSessionActive) {
      checkSession();
    }
  }, [accessSessionActive, checkSession, session, sessionChecked]);

  useEffect(() => {
    const closeLanguageMenu = (event: MouseEvent) => {
      if (!languageRef.current?.contains(event.target as Node)) setLanguageOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setLanguageOpen(false);
    };
    document.addEventListener('mousedown', closeLanguageMenu);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('mousedown', closeLanguageMenu);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, []);

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    try {
      await logout();
      window.location.replace('/login?logout');
    } catch {
      setSession(null);
      setSessionChecked(true);
      setLoggingOut(false);
    }
  };

  const authenticated = Boolean(sessionChecked && session?.authenticated);
  const mypageHref = authenticated ? '/mypage' : loginHref('/mypage');
  const wishlistHref = authenticated ? '/#wishlist' : loginHref('/#wishlist');
  const supportHref = authenticated ? '/mypage/inquiries' : loginHref('/mypage/inquiries');
  const navigationItems = [
    [copy.header.navigation[0], '/#top'],
    [copy.header.navigation[1], '/#service'],
    [copy.header.navigation[2], '/#reviews'],
    [copy.header.navigation[3], supportHref],
    [copy.header.navigation[4], '/#notices'],
  ];
  const languageOptions: Array<{ code: 'KO' | 'JP' | 'EN'; locale: Locale; label: string }> = [
    { code: 'KO', locale: 'ko', label: '한국어' },
    { code: 'JP', locale: 'ja', label: '日本語' },
    { code: 'EN', locale: 'en', label: 'English' },
  ];
  const selectedLanguage = languageOptions.find((option) => option.locale === locale) ?? languageOptions[0];

  return (
    <header className="site-header">
      <div className="utility-bar">
        <div className="header-inner utility-inner">
          <span>{copy.header.tagline}</span>
          <nav aria-label="회원 메뉴">
            {sessionChecked && session?.authenticated ? (
              <>
                <span className="member-greeting">{session.nickname || 'ZIK:00'}{copy.header.greeting}</span>
                <button className="utility-link-button" type="button" onClick={handleLogout} disabled={loggingOut}>
                  {loggingOut ? copy.header.loggingOut : copy.header.logout}
                </button>
              </>
            ) : sessionChecked ? (
              <a href="/login">{copy.header.login}</a>
            ) : null}
            <a className="utility-support-link" href={supportHref}>{copy.header.support}</a>
            <div className="language-switcher" ref={languageRef}>
              <button
                className="language-button"
                type="button"
                aria-haspopup="menu"
                aria-expanded={languageOpen}
                onClick={() => setLanguageOpen((current) => !current)}
              >
                <Globe2 size={14} aria-hidden="true" /> {selectedLanguage.code}
                <ChevronDown size={13} aria-hidden="true" />
              </button>
              {languageOpen && (
                <div className="language-menu" role="menu">
                  {languageOptions.map((option) => (
                    <button
                      className={locale === option.locale ? 'active' : ''}
                      type="button"
                      role="menuitemradio"
                      aria-checked={locale === option.locale}
                      key={option.locale}
                      onClick={() => { setLocale(option.locale); setLanguageOpen(false); }}
                    >
                      <strong>{option.code}</strong><span>{option.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </nav>
        </div>
      </div>

      <div className="header-inner main-header-row">
        <CategoryMegaMenu />

        <Brand />
        <SearchBox />

        <nav className="primary-actions" aria-label="주요 메뉴">
          <a href={mypageHref}><UserRound size={25} /><span>{copy.header.mypage}</span></a>
          <a href={wishlistHref}><Heart size={25} /><span>{copy.header.wishlist}</span></a>
          <a href="#cart" className="cart-link">
            <ShoppingBag size={25} />
            <span className="cart-count">0</span>
            <span>{copy.header.cart}</span>
          </a>
        </nav>

        <a className="agency-cta" href="#agency">
          <Truck size={27} aria-hidden="true" />
          <span><small>{copy.header.agencyEyebrow}</small>{copy.header.agency}</span>
        </a>
      </div>

      <nav className="category-nav header-inner" aria-label="메인 메뉴">
        {navigationItems.map(([label, href]) => (
          <a key={label} href={href}>{label}</a>
        ))}
      </nav>
    </header>
  );
}

export { Brand };
export default SiteHeader;
