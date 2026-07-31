import { createContext, type ReactNode, useContext, useEffect, useMemo, useState } from 'react';

export type Locale = 'ko' | 'ja' | 'en';

type LocaleCopy = {
  header: {
    tagline: string;
    login: string;
    logout: string;
    loggingOut: string;
    greeting: string;
    support: string;
    category: string;
    mypage: string;
    wishlist: string;
    cart: string;
    agencyEyebrow: string;
    agency: string;
    navigation: [string, string, string, string, string];
  };
  search: {
    label: string;
    placeholder: string;
    submit: string;
    clear: string;
    scopes: [string, string, string];
    recent: string;
    clearAll: string;
    noRecent: string;
    popular: string;
    updatedAt: string;
    live: string;
  };
  hero: {
    ariaLabel: string;
    cta: string;
    previous: string;
    next: string;
    select: string;
    slides: Array<{ eyebrow: string; title: string; description: string }>;
  };
  service: Array<{ title: string; body: string }>;
  products: {
    eyebrow: string;
    title: string;
    all: string;
    shipping: string;
    favorite: string;
  };
  quick: [string, string, string, string];
  footer: {
    about: string;
    terms: string;
    privacy: string;
    intro: string;
    company: string;
    copyright: string;
  };
};

const copies: Record<Locale, LocaleCopy> = {
  ko: {
    header: {
      tagline: '한국 상품을 일본까지, 쉽고 빠르게',
      login: '로그인',
      logout: '로그아웃',
      loggingOut: '로그아웃 중',
      greeting: '님, 안녕하세요.',
      support: '고객센터',
      category: '카테고리',
      mypage: '마이페이지',
      wishlist: '찜',
      cart: '장바구니',
      agencyEyebrow: '찾는 상품이 없다면',
      agency: '배송대행 신청',
      navigation: ['홈', '서비스 소개', '리뷰', '고객센터', '공지사항'],
    },
    search: {
      label: '상품 검색',
      placeholder: '상품명 또는 URL을 검색해보세요',
      submit: '검색',
      clear: '검색어 지우기',
      scopes: ['전체', '제목', '제목 + 내용'],
      recent: '최근 검색어',
      clearAll: '전체 삭제',
      noRecent: '최근 검색어가 없습니다.',
      popular: '인기 검색어',
      updatedAt: '오늘 오후 2시 업데이트',
      live: '실시간',
    },
    hero: {
      ariaLabel: '주요 기획전',
      cta: '기획전 보기',
      previous: '이전 배너',
      next: '다음 배너',
      select: '배너 선택',
      slides: [
        { eyebrow: 'SEOUL SUMMER 2026', title: '한국의 여름을\n가볍게 즐기는 방법', description: '서울에서 지금 뜨는 여름 아이템을 일본까지 만나보세요.' },
        { eyebrow: 'SEOUL STREET', title: '오늘의 스타일을\n가볍게 업데이트', description: '스니커즈부터 데일리 아이템까지 빠르게 둘러보세요.' },
        { eyebrow: 'K-LIFESTYLE', title: '취향을 채우는\n작고 좋은 물건들', description: '문구, 리빙, 굿즈를 현지 배송부터 통관까지 편리하게.' },
      ],
    },
    service: [
      { title: '검수부터 포장까지', body: '안전한 배송 대행' },
      { title: '비용을 한눈에', body: '예상 금액 미리 확인' },
      { title: '진행 상황 확인', body: '주문부터 배송까지' },
    ],
    products: { eyebrow: 'SEOUL SUMMER PICKS', title: '이번 여름 한국에서 뜨는 상품', all: '전체 보기', shipping: '예상 국제배송비 별도', favorite: '찜하기' },
    quick: ['현지 배송조회', '출고 일정', '예상비용 계산', '1:1 문의'],
    footer: {
      about: '회사소개',
      terms: '이용약관',
      privacy: '개인정보처리방침',
      intro: '한국의 좋은 상품을 일본까지 편리하게 연결합니다.',
      company: '상호명: ZIK:00 · 운영시간: 평일 10:00–17:00',
      copyright: '© 2026 ZIK:00. All rights reserved.',
    },
  },
  ja: {
    header: {
      tagline: '韓国の商品を日本まで、かんたん・スピーディーに',
      login: 'ログイン',
      logout: 'ログアウト',
      loggingOut: 'ログアウト中',
      greeting: 'さん、こんにちは。',
      support: 'カスタマーサポート',
      category: 'カテゴリー',
      mypage: 'マイページ',
      wishlist: 'お気に入り',
      cart: 'カート',
      agencyEyebrow: '商品が見つからない場合',
      agency: '配送代行を申請',
      navigation: ['ホーム', 'サービス紹介', 'レビュー', 'カスタマーサポート', 'お知らせ'],
    },
    search: {
      label: '商品検索',
      placeholder: '商品名またはURLを検索してください',
      submit: '検索',
      clear: '検索ワードを削除',
      scopes: ['すべて', 'タイトル', 'タイトル + 内容'],
      recent: '最近の検索',
      clearAll: 'すべて削除',
      noRecent: '最近の検索はありません。',
      popular: '人気検索ワード',
      updatedAt: '本日午後2時更新',
      live: 'リアルタイム',
    },
    hero: {
      ariaLabel: 'メインキャンペーン',
      cta: '特集を見る',
      previous: '前のバナー',
      next: '次のバナー',
      select: 'バナー選択',
      slides: [
        { eyebrow: 'SEOUL SUMMER 2026', title: '韓国の夏を\n軽やかに楽しむ方法', description: 'ソウルで今注目のサマーアイテムを日本までお届けします。' },
        { eyebrow: 'SEOUL STREET', title: '今日のスタイルを\n軽やかにアップデート', description: 'スニーカーからデイリーアイテムまで、すぐにチェック。' },
        { eyebrow: 'K-LIFESTYLE', title: '好きを満たす\n小さくて素敵なもの', description: '文具、リビング、グッズを現地配送から通関まで便利に。' },
      ],
    },
    service: [
      { title: '検品から梱包まで', body: '安心の配送代行' },
      { title: '費用をひと目で', body: '予想金額を事前確認' },
      { title: '進行状況を確認', body: '注文から配送まで' },
    ],
    products: { eyebrow: 'SEOUL SUMMER PICKS', title: 'この夏、韓国で話題の商品', all: 'すべて見る', shipping: '国際配送料は別途', favorite: 'お気に入りに追加' },
    quick: ['韓国内配送照会', '出庫スケジュール', '予想費用を計算', '1:1 お問い合わせ'],
    footer: {
      about: '会社紹介',
      terms: '利用規約',
      privacy: 'プライバシーポリシー',
      intro: '韓国の良い商品を日本まで便利につなぎます。',
      company: '会社名: ZIK:00 · 営業時間: 平日 10:00–17:00',
      copyright: '© 2026 ZIK:00. All rights reserved.',
    },
  },
  en: {
    header: {
      tagline: 'Korean products delivered to Japan, simply and quickly',
      login: 'Log in',
      logout: 'Log out',
      loggingOut: 'Logging out',
      greeting: ', welcome back.',
      support: 'Support',
      category: 'Categories',
      mypage: 'My page',
      wishlist: 'Wishlist',
      cart: 'Cart',
      agencyEyebrow: 'Cannot find an item?',
      agency: 'Request delivery',
      navigation: ['Home', 'Our service', 'Reviews', 'Support', 'Notices'],
    },
    search: {
      label: 'Product search',
      placeholder: 'Search by product name or URL',
      submit: 'Search',
      clear: 'Clear search',
      scopes: ['All', 'Title', 'Title + description'],
      recent: 'Recent searches',
      clearAll: 'Clear all',
      noRecent: 'No recent searches.',
      popular: 'Popular searches',
      updatedAt: 'Updated today at 2 PM',
      live: 'Live',
    },
    hero: {
      ariaLabel: 'Featured collections',
      cta: 'View collection',
      previous: 'Previous banner',
      next: 'Next banner',
      select: 'Select banner',
      slides: [
        { eyebrow: 'SEOUL SUMMER 2026', title: 'A lighter way to\nenjoy summer in Korea', description: 'Discover the summer items trending in Seoul, delivered to Japan.' },
        { eyebrow: 'SEOUL STREET', title: 'Give today’s style\na light update', description: 'Browse sneakers and everyday pieces in one place.' },
        { eyebrow: 'K-LIFESTYLE', title: 'Small, lovely things\nthat fit your taste', description: 'Stationery, living goods and collectibles, from local delivery through customs.' },
      ],
    },
    service: [
      { title: 'Inspection to packing', body: 'Reliable delivery service' },
      { title: 'Costs at a glance', body: 'Check estimates in advance' },
      { title: 'Track every step', body: 'From order to delivery' },
    ],
    products: { eyebrow: 'SEOUL SUMMER PICKS', title: 'Trending in Korea this summer', all: 'View all', shipping: 'International shipping calculated separately', favorite: 'Add to wishlist' },
    quick: ['Local delivery tracking', 'Dispatch schedule', 'Estimate costs', '1:1 support'],
    footer: {
      about: 'About us',
      terms: 'Terms of use',
      privacy: 'Privacy policy',
      intro: 'Connecting great Korean products to Japan with ease.',
      company: 'Company: ZIK:00 · Hours: Weekdays 10:00–17:00',
      copyright: '© 2026 ZIK:00. All rights reserved.',
    },
  },
};

const LOCALE_STORAGE_KEY = 'zik.locale';

type LocaleContextValue = {
  locale: Locale;
  copy: LocaleCopy;
  setLocale: (locale: Locale) => void;
};

const LocaleContext = createContext<LocaleContextValue | null>(null);

function initialLocale(): Locale {
  try {
    const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY);
    if (stored === 'ko' || stored === 'ja' || stored === 'en') return stored;
  } catch {
    // Korean remains the default when browser storage is unavailable.
  }
  return 'ko';
}

export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(initialLocale);

  useEffect(() => {
    document.documentElement.lang = locale;
    try {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, locale);
    } catch {
      // The selected locale still remains active for the current page.
    }
  }, [locale]);

  const value = useMemo(() => ({
    locale,
    copy: copies[locale],
    setLocale: setLocaleState,
  }), [locale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale() {
  const context = useContext(LocaleContext);
  if (context === null) throw new Error('useLocale must be used inside LocaleProvider.');
  return context;
}
