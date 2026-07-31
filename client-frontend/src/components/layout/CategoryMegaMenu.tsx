import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Menu from 'lucide-react/dist/esm/icons/menu.js';
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

const menuCopy: Record<Locale, { primary: string; secondary: string; tertiary: string; all: string }> = {
  ko: { primary: '카테고리', secondary: '하위 카테고리', tertiary: '상세 카테고리', all: '전체 상품 보기' },
  ja: { primary: 'カテゴリー', secondary: 'サブカテゴリー', tertiary: '詳細カテゴリー', all: 'すべての商品を見る' },
  en: { primary: 'Categories', secondary: 'Subcategories', tertiary: 'Detailed categories', all: 'View all products' },
};

function middleSections(group: CategoryGroup) {
  if (group.name !== '패션의류') return group.sections;
  return group.sections.filter((section) => fashionDepartments.has(section.name));
}

function groupedLeaves(group: CategoryGroup, section: CategorySection) {
  if (group.name !== '패션의류' || section.name === '아동의류') {
    return [{ name: '', items: section.children }];
  }

  return section.children.map((groupName) => {
    const detail = group.sections.find((candidate) => candidate.name === groupName);
    return { name: groupName, items: detail?.children ?? [] };
  });
}

function CategoryMegaMenu() {
  const { locale, copy } = useLocale();
  const [open, setOpen] = useState(false);
  const [activeGroupName, setActiveGroupName] = useState<string | null>(null);
  const [activeSectionName, setActiveSectionName] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const closeTimerRef = useRef<number | null>(null);

  const activeGroup = categoryGroups.find((group) => group.name === activeGroupName) ?? null;
  const activeMiddleSections = activeGroup ? middleSections(activeGroup) : [];
  const activeSection = activeMiddleSections.find((section) => section.name === activeSectionName) ?? null;
  const leafGroups = activeGroup && activeSection ? groupedLeaves(activeGroup, activeSection) : [];
  const labels = menuCopy[locale];

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
      onMouseEnter={openPrimaryMenu}
      onMouseLeave={scheduleClose}
      onFocusCapture={openPrimaryMenu}
    >
      <button
        className="category-button"
        type="button"
        aria-label={copy.header.category}
        aria-expanded={open}
        aria-controls="category-mega-menu"
        onClick={openPrimaryMenu}
      >
        <Menu size={24} aria-hidden="true" />
        <span>{copy.header.category}</span>
      </button>

      {open && (
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
              <a className="category-view-all" href={categorySearchHref([activeGroup.name])}>
                {topLabels[locale][activeGroup.name] ?? activeGroup.name} {labels.all}
              </a>
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
              <a className="category-view-all" href={categorySearchHref([activeGroup.name, activeSection.name])}>
                {activeSection.name} {labels.all}
              </a>
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
    </div>
  );
}

export default CategoryMegaMenu;
