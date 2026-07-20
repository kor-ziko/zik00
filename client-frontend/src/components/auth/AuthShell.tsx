import type { ReactNode } from 'react';
import { ArrowLeft, LockKeyhole } from 'lucide-react';

type AuthShellProps = {
  children: ReactNode;
  step?: string;
};

function AuthShell({ children, step }: AuthShellProps) {
  return (
    <div className="auth-page">
      <header className="auth-topbar">
        <div className="auth-container auth-topbar-inner">
          <a className="auth-brand" href="/" aria-label="ZIK:00 홈">
            <span>ZIK</span><strong>:00</strong>
          </a>
          <div className="auth-topbar-meta">
            {step && <span className="auth-step">{step}</span>}
            <span><LockKeyhole size={15} aria-hidden="true" /> 안전한 회원 인증</span>
          </div>
        </div>
      </header>
      {children}
      <footer className="auth-footer auth-container">
        <a href="/"><ArrowLeft size={15} aria-hidden="true" /> 쇼핑 계속하기</a>
        <span>© ZIK:00. 한국의 좋은 상품을 일본까지.</span>
      </footer>
    </div>
  );
}

export default AuthShell;
