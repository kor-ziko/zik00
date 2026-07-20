import { useEffect, useRef, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { completeOAuthLogin } from '../../api/auth';
import AuthShell from './AuthShell';

function OAuthCallbackPage() {
  const [error, setError] = useState('');
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    const code = new URLSearchParams(window.location.search).get('code');
    if (!code) {
      setError('Google 로그인 완료 코드가 없습니다.');
      return;
    }
    completeOAuthLogin(code)
      .then((result) => {
        window.history.replaceState({}, '', result.destination);
        window.dispatchEvent(new PopStateEvent('popstate'));
      })
      .catch(() => setError('Google 로그인을 완료하지 못했습니다. 다시 시도해주세요.'));
  }, []);

  return <AuthShell><main className="auth-container oauth-callback-page">
    {error ? <><h1>로그인 실패</h1><p className="form-alert">{error}</p><a href="/login">로그인으로 돌아가기</a></> : <><LoaderCircle className="spin" /><h1>Google 로그인 처리 중</h1><p>안전한 로그인 정보를 확인하고 있습니다.</p></>}
  </main></AuthShell>;
}

export default OAuthCallbackPage;
