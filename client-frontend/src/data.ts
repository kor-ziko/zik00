export type Trend = 'up' | 'down' | 'same' | 'new';

export type PopularKeyword = {
  rank: number;
  label: string;
  trend: Trend;
};

export type HeroSlide = {
  eyebrow: string;
  title: string;
  description: string;
  image: string;
  accent: string;
};

export type Product = {
  id: number;
  name: string;
  category: string;
  price: number;
  originalPrice?: number;
  image: string;
  badge?: string;
};

export const popularKeywords: PopularKeyword[] = [
  { rank: 1, label: '선케어', trend: 'up' },
  { rank: 2, label: '휴대용 선풍기', trend: 'up' },
  { rank: 3, label: '보냉 텀블러', trend: 'new' },
  { rank: 4, label: '장마 레인부츠', trend: 'up' },
  { rank: 5, label: '메쉬 캡', trend: 'same' },
  { rank: 6, label: '썸머 샌들', trend: 'up' },
  { rank: 7, label: '워터프루프 메이크업', trend: 'new' },
  { rank: 8, label: '쿨링 패치', trend: 'up' },
  { rank: 9, label: '린넨 셔츠', trend: 'same' },
  { rank: 10, label: '여름 이불', trend: 'down' },
];

export const heroSlides: HeroSlide[] = [
  {
    eyebrow: 'SEOUL SUMMER 2026',
    title: '한국의 여름을\n가볍게 즐기는 방법',
    description: '서울에서 지금 뜨는 여름 아이템을 일본까지 만나보세요.',
    image: '/assets/hero-seoul-summer.webp',
    accent: '#f2bf3d',
  },
  {
    eyebrow: 'SEOUL STREET',
    title: '오늘의 스타일을\n가볍게 업데이트',
    description: '스니커즈부터 데일리 아이템까지 빠르게 둘러보세요.',
    image: '/assets/hero-style.webp',
    accent: '#315b45',
  },
  {
    eyebrow: 'K-LIFESTYLE',
    title: '취향을 채우는\n작고 좋은 물건들',
    description: '문구, 리빙, 굿즈를 현지 배송부터 통관까지 편리하게.',
    image: '/assets/hero-living.webp',
    accent: '#b56b24',
  },
];

export const products: Product[] = [
  {
    id: 1,
    name: '산뜻한 데일리 선케어 로션',
    category: 'K-뷰티 · 선케어',
    price: 2380,
    originalPrice: 2900,
    image: '/assets/product-suncare.webp',
    badge: '여름 인기',
  },
  {
    id: 2,
    name: '충전식 무선 미니 테이블 팬',
    category: '디지털 · 계절가전',
    price: 4190,
    originalPrice: 5200,
    image: '/assets/product-mini-fan.webp',
    badge: '급상승',
  },
  {
    id: 3,
    name: '24시간 보냉 와이드 텀블러',
    category: '리빙 · 피크닉',
    price: 3650,
    image: '/assets/product-tumbler.webp',
    badge: '신상품',
  },
  {
    id: 4,
    name: '라이트 데님 서머 메쉬 캡',
    category: '패션 · 액세서리',
    price: 2890,
    image: '/assets/product-summer-cap.webp',
  },
  {
    id: 5,
    name: '플랫폼 스트랩 서머 샌들',
    category: '패션 · 슈즈',
    price: 6790,
    originalPrice: 8100,
    image: '/assets/product-sandals.webp',
    badge: '서울 픽',
  },
];
