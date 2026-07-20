import { ArrowRight, BadgeCheck, PackageCheck, ShieldCheck } from 'lucide-react';
import AuthShell from './AuthShell';

const benefits = [
  { icon: PackageCheck, title: '주문부터 배송까지', body: '구매 내역과 일본 배송 현황을 한곳에서 확인해요.' },
  { icon: BadgeCheck, title: '회원 전용 혜택', body: '쿠폰과 적립금, 개인화된 상품 추천을 받아보세요.' },
  { icon: ShieldCheck, title: '안전한 소셜 인증', body: '별도 비밀번호 없이 소셜 계정으로 안전하게 시작해요.' },
];

function LoginPage() {
  const params = new URLSearchParams(window.location.search);
  const hasError = params.has('error');
  const loggedOut = params.has('logout');
  const registrationExpired = params.get('reason') === 'registration-expired';

  return (
    <AuthShell>
      <main className="auth-container login-layout">
        <section className="login-story" aria-labelledby="login-story-title">
          <p className="auth-kicker">WELCOME TO ZIK:00</p>
          <h1 id="login-story-title">한국 쇼핑을<br /><em>더 가까운 일상</em>으로.</h1>
          <p className="login-story-copy">
            좋아하는 한국 상품을 찾고, 주문하고, 일본에서 받아보는 모든 과정을 한 계정으로 관리하세요.
          </p>
          <div className="login-benefits">
            {benefits.map(({ icon: Icon, title, body }) => (
              <article key={title}>
                <span><Icon size={22} strokeWidth={1.8} aria-hidden="true" /></span>
                <div><strong>{title}</strong><p>{body}</p></div>
              </article>
            ))}
          </div>
        </section>

        <section className="login-panel" aria-labelledby="login-title">
          <div className="login-panel-accent" aria-hidden="true"><span /><span /><span /></div>
          <p className="auth-kicker">MEMBER LOGIN</p>
          <h2 id="login-title">로그인</h2>
          <p className="login-panel-copy">Google 계정 하나로 빠르고 간편하게 시작할 수 있습니다.</p>
          {hasError && <p className="form-alert" role="alert">Google 로그인에 실패했습니다. 잠시 후 다시 시도해주세요.</p>}
          {registrationExpired && (
            <p className="form-alert" role="alert">
              가입 가능 시간이 만료되었거나 이미 처리된 요청입니다. Google 로그인을 다시 진행해주세요.
            </p>
          )}
          {loggedOut && <p className="form-notice" role="status">로그아웃되었습니다.</p>}
          <a className="google-login-button" href="/oauth2/authorization/google">
            <span className="google-mark" aria-hidden="true">G</span>
            <span>Google로 계속하기</span>
            <ArrowRight size={19} aria-hidden="true" />
          </a>
          <div className="login-divider"><span>처음 방문하셨나요?</span></div>
          <p className="login-first-visit">
            Google 로그인 후 이름과 배송 정보를 입력하면 회원가입이 바로 완료됩니다.
          </p>
          <p className="login-terms">계속하면 ZIK:00의 이용약관 및 개인정보 처리방침에 동의하게 됩니다.</p>
        </section>
      </main>
    </AuthShell>
  );
}

export default LoginPage;
