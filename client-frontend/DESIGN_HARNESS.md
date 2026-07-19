# ZIK:00 사용자 메인 디자인 하네스

이 문서는 사용자 프론트 메인 화면의 기준 디자인과 동작을 정리한다. 현재 단계에서는 실제 상품 API 대신 `src/data.ts`의 목업 데이터를 사용한다.

## 실행

```bash
cd zik00/client-frontend
npm install
npm run dev
```

브라우저에서 `http://127.0.0.1:5174`를 연다.

## 화면 구조

```text
App
├── SiteHeader
│   ├── 회원 메뉴
│   ├── 카테고리 버튼
│   ├── SearchBox
│   └── 마이페이지·찜·장바구니
├── HeroCarousel
├── ServiceStrip
├── ProductSection
├── QuickMenu
└── SiteFooter
```

## 디자인 기준

- 최대 콘텐츠 폭: `1240px`
- 주요 색상: 검정 `#20242b`, 파랑 `#246bfd`, 빨강 `#e45163`, 노랑 `#f2bf3d`
- 글꼴: `Noto Sans KR`
- 데스크톱 우선으로 구성하고 `820px`, `540px`에서 모바일 레이아웃으로 변경한다.
- 오른쪽 빠른 메뉴는 스크롤 중에도 화면 오른쪽에 고정한다.
- 상품 이미지는 동일한 비율을 유지해 상품명이 바뀌어도 레이아웃이 흔들리지 않게 한다.

## 검색 상태

검색창에 포커스가 들어오면 최근 검색어와 인기 검색어를 표시한다.

- 최근 검색어는 브라우저 `localStorage`에 최대 8개 저장한다.
- 같은 검색어를 다시 입력하면 중복을 제거하고 맨 앞으로 이동한다.
- 개별 삭제와 전체 삭제를 지원한다.
- 인기 검색어는 현재 `src/data.ts`의 목업 데이터다.
- `Escape` 키 또는 검색 영역 바깥 클릭으로 패널을 닫는다.

## API 연결 지점

백엔드가 준비되면 다음 순서로 목업을 교체한다.

| 기능 | 현재 | 예정 API |
|---|---|---|
| 인기 검색어 | `popularKeywords` | `GET /api/search/popular` |
| 메인 배너 | `heroSlides` | `GET /api/banners?placement=MAIN` |
| 여름 추천 상품 | `products` | `GET /api/products?theme=SUMMER` |
| 상품 검색 | 최근 검색어만 저장 | `GET /api/products/search?keyword=` |
| 찜하기 | 화면 버튼만 존재 | `POST /api/wishlist/{productId}` |

## 주요 파일

```text
src/
├── components/
│   ├── home/
│   ├── layout/
│   └── search/
├── hooks/useRecentSearches.ts
├── data.ts
├── App.tsx
└── styles.css
```

`data.ts`는 디자인 확인용 데이터, `components`는 화면, `hooks`는 화면과 분리된 상태 관리 역할을 담당한다.
