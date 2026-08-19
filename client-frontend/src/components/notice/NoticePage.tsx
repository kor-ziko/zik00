import ChevronLeft from 'lucide-react/dist/esm/icons/chevron-left.js';
import ChevronRight from 'lucide-react/dist/esm/icons/chevron-right.js';
import Pin from 'lucide-react/dist/esm/icons/pin.js';
import { useEffect, useState } from 'react';
import { getNotice, getNotices, type NoticeDetail, type NoticeListResponse } from '../../api/notice';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

const dateFormatter = new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' });
const formatDate = (value: string) => dateFormatter.format(new Date(value));

function NoticePage({ noticeId }: { noticeId?: number }) {
  const [category, setCategory] = useState('전체');
  const [page, setPage] = useState(0);
  const [list, setList] = useState<NoticeListResponse | null>(null);
  const [detail, setDetail] = useState<NoticeDetail | null>(null);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setError('');
    if (noticeId) {
      setDetail(null);
      getNotice(noticeId, controller.signal)
        .then(setDetail)
        .catch((reason: unknown) => {
          if ((reason as Error).name !== 'AbortError') setError(reason instanceof Error ? reason.message : '공지사항을 불러오지 못했습니다.');
        });
    } else {
      setList(null);
      getNotices(category, page, controller.signal)
        .then(setList)
        .catch((reason: unknown) => {
          if ((reason as Error).name !== 'AbortError') setError(reason instanceof Error ? reason.message : '공지사항을 불러오지 못했습니다.');
        });
    }
    return () => controller.abort();
  }, [category, noticeId, page, reloadKey]);

  const categories = ['전체', ...(list?.categories ?? [])];
  const pageBlockStart = Math.floor(page / 5) * 5;
  const visiblePages = list
    ? Array.from({ length: Math.min(5, list.totalPages - pageBlockStart) }, (_, index) => pageBlockStart + index)
    : [];

  return (
    <div className="app-shell information-page-shell">
      <SiteHeader />
      <main className="notice-page header-inner">
        <nav className="information-breadcrumb" aria-label="현재 위치"><a href="/">홈</a><ChevronRight size={13} /><span>공지사항</span></nav>
        <header className="notice-page-heading"><p>NOTICE</p><h1>공지사항</h1><span>서비스 이용에 필요한 소식과 안내를 확인하세요.</span></header>

        {error ? (
          <section className="information-state">
            <h2>공지사항을 불러오지 못했습니다.</h2><p>{error}</p>
            <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 시도</button>
          </section>
        ) : noticeId ? (
          detail ? (
            <article className="notice-detail">
              <header>
                <div><span>{detail.category}</span>{detail.pinned && <b><Pin size={13} />중요</b>}</div>
                <h2>{detail.title}</h2><time dateTime={detail.publishedAt}>{formatDate(detail.publishedAt)}</time>
              </header>
              <div className="notice-detail-content">{detail.content}</div>
              <a className="notice-list-link" href="/notices"><ChevronLeft size={17} />목록으로</a>
            </article>
          ) : <div className="information-loading" role="status">공지사항을 불러오는 중입니다.</div>
        ) : list ? (
          <>
            <div className="notice-category-tabs" role="tablist" aria-label="공지 분류">
              {categories.map((item) => (
                <button type="button" role="tab" aria-selected={category === item} className={category === item ? 'active' : ''}
                  onClick={() => { setCategory(item); setPage(0); }} key={item}>{item}</button>
              ))}
            </div>
            <section className="notice-list" aria-label="공지사항 목록">
              <div className="notice-list-head"><span>분류</span><span>제목</span><span>등록일</span></div>
              {list.items.length === 0 ? <div className="notice-empty">등록된 공지사항이 없습니다.</div> : list.items.map((notice) => (
                <a className={notice.pinned ? 'notice-row pinned' : 'notice-row'} href={`/notices/${notice.id}`} key={notice.id}>
                  <span>{notice.category}</span>
                  <strong>{notice.pinned && <Pin size={14} aria-label="중요 공지" />}{notice.title}</strong>
                  <time dateTime={notice.publishedAt}>{formatDate(notice.publishedAt)}</time>
                </a>
              ))}
            </section>
            {list.totalPages > 1 && (
              <nav className="notice-pagination" aria-label="공지사항 페이지">
                <button type="button" aria-label="이전 페이지 묶음" disabled={pageBlockStart === 0} onClick={() => setPage(Math.max(0, pageBlockStart - 5))}><ChevronLeft size={16} /></button>
                {visiblePages.map((index) => (
                  <button type="button" className={page === index ? 'active' : ''} aria-current={page === index ? 'page' : undefined} onClick={() => setPage(index)} key={index}>{index + 1}</button>
                ))}
                <button type="button" aria-label="다음 페이지 묶음" disabled={pageBlockStart + 5 >= list.totalPages} onClick={() => setPage(pageBlockStart + 5)}><ChevronRight size={16} /></button>
              </nav>
            )}
          </>
        ) : <div className="information-loading" role="status">공지사항을 불러오는 중입니다.</div>}
      </main>
      <SiteFooter />
    </div>
  );
}

export default NoticePage;
