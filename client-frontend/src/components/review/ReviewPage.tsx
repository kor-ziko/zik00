import ChevronDown from 'lucide-react/dist/esm/icons/chevron-down.js';
import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Quote from 'lucide-react/dist/esm/icons/quote.js';
import Star from 'lucide-react/dist/esm/icons/star.js';
import { useEffect, useState } from 'react';
import { getReviews, type ReviewListResponse } from '../../api/review';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

const dateFormatter = new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });

function Stars({ rating, size = 15 }: { rating: number; size?: number }) {
  return (
    <span className="client-review-stars" aria-label={`별점 ${rating}점`}>
      {Array.from({ length: 5 }, (_, index) => (
        <Star size={size} fill={index < rating ? 'currentColor' : 'none'} key={index} />
      ))}
    </span>
  );
}

function ReviewPage() {
  const [rating, setRating] = useState<number | null>(null);
  const [sort, setSort] = useState('latest');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<ReviewListResponse | null>(null);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setError('');
    getReviews(rating, sort, page, controller.signal)
      .then(setData)
      .catch((reason: unknown) => {
        if ((reason as Error).name !== 'AbortError') setError(reason instanceof Error ? reason.message : '리뷰를 불러오지 못했습니다.');
      });
    return () => controller.abort();
  }, [page, rating, reloadKey, sort]);

  const maximumCount = Math.max(...(data?.ratingCounts.map((item) => item.count) ?? [1]), 1);
  const pageBlockStart = Math.floor(page / 5) * 5;
  const visiblePages = data
    ? Array.from({ length: Math.min(5, data.totalPages - pageBlockStart) }, (_, index) => pageBlockStart + index)
    : [];

  return (
    <div className="app-shell information-page-shell">
      <SiteHeader />
      <main className="review-page header-inner">
        <nav className="information-breadcrumb" aria-label="현재 위치"><a href="/">홈</a><ChevronRight size={13} /><span>리뷰</span></nav>
        <header className="review-page-heading">
          <p>REAL REVIEW</p><h1>고객 리뷰</h1><span>ZIK:00을 이용한 고객의 경험을 확인해 보세요.</span>
        </header>

        {error ? (
          <section className="information-state"><h2>리뷰를 불러오지 못했습니다.</h2><p>{error}</p><button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button></section>
        ) : !data ? (
          <div className="information-loading" role="status">리뷰를 불러오는 중입니다.</div>
        ) : (
          <>
            <section className="client-review-summary" aria-label="리뷰 평점 요약">
              <div className="client-review-average"><strong>{data.averageRating.toFixed(1)}</strong><Stars rating={Math.round(data.averageRating)} size={19} /><span>{data.totalCount.toLocaleString()}개의 리뷰</span></div>
              <div className="client-review-rating-bars">
                {data.ratingCounts.map((item) => (
                  <button type="button" className={rating === item.rating ? 'active' : ''} onClick={() => { setRating(rating === item.rating ? null : item.rating); setPage(0); }} key={item.rating}>
                    <span>{item.rating}점</span><i><b style={{ width: `${(item.count / maximumCount) * 100}%` }} /></i><em>{item.count}</em>
                  </button>
                ))}
              </div>
              <div className="client-review-summary-copy"><Quote size={28} /><strong>상품 선택에 도움이 되는 실제 경험</strong><p>공개가 승인된 리뷰만 표시됩니다.</p></div>
            </section>

            <div className="client-review-toolbar">
              <div className="client-review-rating-filter" role="group" aria-label="별점 필터">
                <button type="button" className={rating === null ? 'active' : ''} onClick={() => { setRating(null); setPage(0); }}>전체</button>
                {[5, 4, 3, 2, 1].map((value) => <button type="button" className={rating === value ? 'active' : ''} onClick={() => { setRating(value); setPage(0); }} key={value}>{value}점</button>)}
              </div>
              <label className="client-review-sort"><span className="sr-only">리뷰 정렬</span><select value={sort} onChange={(event) => { setSort(event.target.value); setPage(0); }}><option value="latest">최신순</option><option value="rating">평점 높은순</option></select><ChevronDown size={15} /></label>
            </div>

            {data.items.length === 0 ? (
              <div className="client-review-empty"><h2>해당 평점의 리뷰가 없습니다.</h2><button type="button" onClick={() => setRating(null)}>전체 리뷰 보기</button></div>
            ) : (
              <section className="client-review-grid" aria-label="고객 리뷰 목록">
                {data.items.map((review) => (
                  <article className={review.featured ? 'client-review-card featured' : 'client-review-card'} key={review.id}>
                    {review.imageUrl && <img src={review.imageUrl} alt={review.productName} loading="lazy" />}
                    <div className="client-review-card-body">
                      <div className="client-review-card-meta"><Stars rating={review.rating} />{review.featured && <b>추천 리뷰</b>}</div>
                      <h2>{review.title}</h2><p>{review.content}</p>
                      <div className="client-review-product"><span>{review.productName}</span><small>{review.authorName} · {dateFormatter.format(new Date(review.createdAt))}</small></div>
                    </div>
                  </article>
                ))}
              </section>
            )}

            {data.totalPages > 1 && (
              <nav className="notice-pagination" aria-label="리뷰 페이지">
                <button type="button" aria-label="이전 페이지 묶음" disabled={pageBlockStart === 0} onClick={() => setPage(Math.max(0, pageBlockStart - 5))}><ChevronLeft size={16} /></button>
                {visiblePages.map((index) => <button type="button" className={page === index ? 'active' : ''} aria-current={page === index ? 'page' : undefined} onClick={() => setPage(index)} key={index}>{index + 1}</button>)}
                <button type="button" aria-label="다음 페이지 묶음" disabled={pageBlockStart + 5 >= data.totalPages} onClick={() => setPage(pageBlockStart + 5)}><ChevronRight size={16} /></button>
              </nav>
            )}
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}

export default ReviewPage;
