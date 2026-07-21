import { type ErrorInfo, type ReactNode, Component } from 'react';
import { ArrowLeft, Home, TriangleAlert } from 'lucide-react';
import SiteFooter from '../layout/SiteFooter';
import SiteHeader from '../layout/SiteHeader';

type ErrorPageProps = {
  status?: number;
};

function requestedStatus(): number {
  const value = Number(new URLSearchParams(window.location.search).get('status'));
  return Number.isInteger(value) && value >= 400 && value <= 599 ? value : 500;
}

function ErrorPage({ status }: ErrorPageProps) {
  const errorStatus = status ?? requestedStatus();
  const notFound = errorStatus === 404;

  const goBack = () => {
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    window.location.assign('/');
  };

  return (
    <div className="app-shell error-page-shell">
      <SiteHeader />
      <main className="error-page-main">
        <section className="error-page-card" aria-labelledby="error-page-title">
          <div className="error-page-icon"><TriangleAlert size={34} aria-hidden="true" /></div>
          <p className="error-page-status">ERROR {errorStatus}</p>
          <h1 id="error-page-title">{notFound ? '페이지를 찾을 수 없습니다.' : '요청을 처리하지 못했습니다.'}</h1>
          <p>{notFound
            ? '입력한 주소가 정확한지 확인하거나 홈으로 이동해주세요.'
            : '일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.'}</p>
          <div className="error-page-actions">
            <button type="button" onClick={goBack}><ArrowLeft size={18} /> 이전 페이지</button>
            <a href="/"><Home size={18} /> 홈으로 이동</a>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}

type ErrorBoundaryProps = { children: ReactNode };
type ErrorBoundaryState = { failed: boolean };

export class AppErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { failed: false };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Client rendering failed.', error, errorInfo);
  }

  render() {
    return this.state.failed ? <ErrorPage status={500} /> : this.props.children;
  }
}

export default ErrorPage;
