import ArrowLeft from 'lucide-react/dist/esm/icons/arrow-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Menu from 'lucide-react/dist/esm/icons/menu.js';
import X from 'lucide-react/dist/esm/icons/x.js';
import { useEffect, useRef, useState } from 'react';
import { categoryGroups, categorySearchHref, type CategoryGroup, type CategorySection } from '../../categoryData';
import { type Locale, useLocale } from '../../locale';

const fashionDepartments = new Set(['여성의류', '남성의류', '아동의류']);

const topLabels: Record<Locale, Record<string, string>> = {
  ko: {},
  ja: {
    '패션의류': 'ファッション',
    '뷰티·미용': 'ビューティー',
    '가전': '家電',
    '생활잡화': '生活雑貨',
    '스포츠·레저': 'スポーツ・レジャー',
    '컬렉터블·완구·취미': 'コレクション・ホビー',
    '도서·음반·콘텐츠': '本・音楽・コンテンツ',
    '출산·유아동': 'ベビー・キッズ',
    '애견용품': 'ペット用品',
    '자동차용품': 'カー用品',
  },
  en: {
    '패션의류': 'Fashion',
    '뷰티·미용': 'Beauty',
    '가전': 'Electronics',
    '생활잡화': 'Home & living',
    '스포츠·레저': 'Sports & leisure',
    '컬렉터블·완구·취미': 'Collectibles & hobbies',
    '도서·음반·콘텐츠': 'Books & media',
    '출산·유아동': 'Baby & kids',
    '애견용품': 'Pet supplies',
    '자동차용품': 'Automotive',
  },
};

const menuCopy: Record<Locale, { primary: string; secondary: string; tertiary: string; back: string; close: string }> = {
  ko: { primary: '카테고리', secondary: '하위 카테고리', tertiary: '상세 카테고리', back: '이전 카테고리', close: '카테고리 닫기' },
  ja: { primary: 'カテゴリー', secondary: 'サブカテゴリー', tertiary: '詳細カテゴリー', back: '前のカテゴリー', close: 'カテゴリーを閉じる' },
  en: { primary: 'Categories', secondary: 'Subcategories', tertiary: 'Detailed categories', back: 'Previous category', close: 'Close categories' },
};

function middleSections(group: CategoryGroup) {
  if (group.name !== '패션의류') return group.sections;
  return group.sections.filter((section) => fashionDepartments.has(section.name));
}

function groupedLeaves(group: CategoryGroup, section: CategorySection) {
  if (group.name !== '패션의류') {
    return [{ name: '', items: section.children }];
  }

  return section.children.map((groupName) => {
    const detail = group.sections.find((candidate) => candidate.name === groupName);
    return { name: groupName, items: detail?.children ?? [] };
  });
}

function CategoryMegaMenu() {
  const { locale, copy } = useLocale();
  const [mobileMenu, setMobileMenu] = useState(() => window.matchMedia('(max-width: 820px)').matches);
  const [open, setOpen] = useState(false);
  const [activeGroupName, setActiveGroupName] = useState<string | null>(null);
  const [activeSectionName, setActiveSectionName] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const categoryButtonRef = useRef<HTMLButtonElement>(null);
  const mobileCloseButtonRef = useRef<HTMLButtonElement>(null);
  const closeTimerRef = useRef<number | null>(null);

  const activeGroup = categoryGroups.find((group) => group.name === activeGroupName) ?? null;
  const activeMiddleSections = activeGroup ? middleSections(activeGroup) : [];
  const activeSection = activeMiddleSections.find((section) => section.name === activeSectionName) ?? null;
  const leafGroups = activeGroup && activeSection ? groupedLeaves(activeGroup, activeSection) : [];
  const labels = menuCopy[locale];
  const mobileMenuTitle = activeSection?.name
    ?? (activeGroup ? topLabels[locale][activeGroup.name] ?? activeGroup.name : copy.header.category);

  const cancelClose = () => {
    if (closeTimerRef.current !== null) window.clearTimeout(closeTimerRef.current);
    closeTimerRef.current = null;
  };

  const scheduleClose = () => {
    cancelClose();
    closeTimerRef.current = window.setTimeout(() => {
      setOpen(false);
      setActiveGroupName(null);
      setActiveSectionName(null);
    }, 180);
  };

  const selectGroup = (group: CategoryGroup) => {
    setActiveGroupName(group.name);
    setActiveSectionName(null);
  };

  const openPrimaryMenu = () => {
    cancelClose();
    if (!open) {
      setActiveGroupName(null);
      setActiveSectionName(null);
    }
    setOpen(true);
  };

  const closeMenu = () => {
    setOpen(false);
    setActiveGroupName(null);
    setActiveSectionName(null);
  };

  const closeMobileMenu = () => {
    closeMenu();
    window.requestAnimationFrame(() => categoryButtonRef.current?.focus());
  };

  const openMobileMenu = () => {
    if (open) {
      closeMobileMenu();
      return;
    }
    setActiveGroupName(null);
    setActiveSectionName(null);
    setOpen(true);
  };

  const showPreviousMobileLevel = () => {
    if (activeSectionName !== null) {
      setActiveSectionName(null);
      return;
    }
    if (activeGroupName !== null) {
      setActiveGroupName(null);
      return;
    }
    closeMobileMenu();
  };

  useEffect(() => {
    const mediaQuery = window.matchMedia('(max-width: 820px)');
    const updateMode = () => {
      setMobileMenu(mediaQuery.matches);
      setOpen(false);
      setActiveGroupName(null);
      setActiveSectionName(null);
    };
    mediaQuery.addEventListener('change', updateMode);
    return () => mediaQuery.removeEventListener('change', updateMode);
  }, []);

  useEffect(() => {
    if (!mobileMenu || !open) return undefined;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    mobileCloseButtonRef.current?.focus();
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [mobileMenu, open]);

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) closeMenu();
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') closeMenu();
    };
    document.addEventListener('mousedown', closeOnOutsideClick);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      cancelClose();
      document.removeEventListener('mousedown', closeOnOutsideClick);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, []);

  return (
    <div
      className="category-menu-anchor"
      ref={rootRef}
      onMouseEnter={mobileMenu ? undefined : openPrimaryMenu}
      onMouseLeave={mobileMenu ? undefined : scheduleClose}
      onFocusCapture={mobileMenu ? undefined : openPrimaryMenu}
    >
      <button
        className="category-button"
        type="button"
        ref={categoryButtonRef}
        aria-label={copy.header.category}
        aria-expanded={open}
        aria-controls="category-mega-menu"
        aria-haspopup={mobileMenu ? 'dialog' : undefined}
        onClick={mobileMenu ? openMobileMenu : openPrimaryMenu}
      >
        <Menu size={24} aria-hidden="true" />
        <span>{copy.header.category}</span>
      </button>

      {open && !mobileMenu && (
        <div
          className={`category-mega-menu ${activeSection ? 'stage-tertiary' : activeGroup ? 'stage-secondary' : 'stage-primary'}`}
          id="category-mega-menu"
        >
          <nav className="category-menu-column category-menu-primary" aria-label={labels.primary}>
            {categoryGroups.map((group) => (
              <a
                className={activeGroup?.name === group.name ? 'active' : ''}
                href={categorySearchHref([group.name])}
                key={group.name}
                onMouseEnter={() => selectGroup(group)}
                onFocus={() => selectGroup(group)}
              >
                <span>{topLabels[locale][group.name] ?? group.name}</span>
                <ChevronRight size={15} aria-hidden="true" />
              </a>
            ))}
          </nav>

          {activeGroup && (
            <nav className="category-menu-column category-menu-secondary" aria-label={labels.secondary}>
              {activeMiddleSections.map((section) => (
                <a
                  className={activeSection?.name === section.name ? 'active' : ''}
                  href={categorySearchHref([activeGroup.name, section.name])}
                  key={section.name}
                  onMouseEnter={() => setActiveSectionName(section.name)}
                  onFocus={() => setActiveSectionName(section.name)}
                >
                  <span>{section.name}</span>
                  <ChevronRight size={15} aria-hidden="true" />
                </a>
              ))}
            </nav>
          )}

          {activeGroup && activeSection && (
            <div className="category-menu-column category-menu-tertiary" aria-label={labels.tertiary}>
              <div className="category-leaf-groups">
                {leafGroups.map((leafGroup) => {
                  const groupPath = [activeGroup.name, activeSection.name, leafGroup.name].filter(Boolean);
                  return (
                    <section key={leafGroup.name || activeSection.name}>
                      {leafGroup.name && <a className="category-leaf-heading" href={categorySearchHref(groupPath)}>{leafGroup.name}</a>}
                      {leafGroup.items.length > 0 && (
                        <div className="category-leaf-list">
                          {leafGroup.items.map((item) => (
                            <a href={categorySearchHref([...groupPath, item])} key={item}>{item}</a>
                          ))}
                        </div>
                      )}
                    </section>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {open && mobileMenu && (
        <>
          <button
            className="category-mobile-backdrop"
            type="button"
            aria-label={labels.close}
            onClick={closeMobileMenu}
          />
          <section
            className="category-mobile-drawer"
            id="category-mega-menu"
            role="dialog"
            aria-modal="true"
            aria-labelledby="category-mobile-title"
          >
            <header className="category-mobile-header">
              {activeGroup ? (
                <button type="button" onClick={showPreviousMobileLevel} aria-label={labels.back}>
                  <ArrowLeft size={21} aria-hidden="true" />
                </button>
              ) : <span aria-hidden="true" />}
              <h2 id="category-mobile-title">{mobileMenuTitle}</h2>
              <button
                type="button"
                ref={mobileCloseButtonRef}
                onClick={closeMobileMenu}
                aria-label={labels.close}
              >
                <X size={21} aria-hidden="true" />
              </button>
            </header>

            <div className="category-mobile-content">
              {!activeGroup && categoryGroups.map((group) => (
                <button
                  className="category-mobile-row"
                  type="button"
                  key={group.name}
                  onClick={() => selectGroup(group)}
                >
                  <span>{topLabels[locale][group.name] ?? group.name}</span>
                  <ChevronRight size={17} aria-hidden="true" />
                </button>
              ))}

              {activeGroup && !activeSection && (
                <>
                  {activeMiddleSections.map((section) => (
                    <button
                      className="category-mobile-row"
                      type="button"
                      key={section.name}
                      onClick={() => setActiveSectionName(section.name)}
                    >
                      <span>{section.name}</span>
                      <ChevronRight size={17} aria-hidden="true" />
                    </button>
                  ))}
                </>
              )}

              {activeGroup && activeSection && (
                <>
                  <div className="category-mobile-leaf-groups">
                    {leafGroups.map((leafGroup) => {
                      const groupPath = [activeGroup.name, activeSection.name, leafGroup.name].filter(Boolean);
                      return (
                        <section key={leafGroup.name || activeSection.name}>
                          {leafGroup.name && (
                            <a className="category-mobile-leaf-heading" href={categorySearchHref(groupPath)}>
                              {leafGroup.name}
                            </a>
                          )}
                          {leafGroup.items.map((item) => (
                            <a className="category-mobile-leaf-link" href={categorySearchHref([...groupPath, item])} key={item}>
                              <span>{item}</span>
                            </a>
                          ))}
                        </section>
                      );
                    })}
                  </div>
                </>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

export default CategoryMegaMenu;
