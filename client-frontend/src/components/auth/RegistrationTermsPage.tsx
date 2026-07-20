import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, Check, ChevronRight, LoaderCircle, ShieldCheck } from 'lucide-react';
import { acceptRegistrationTerms, ApiError, getRegistrationTerms } from '../../api/auth';
import { registrationTerms, type RegistrationTermId } from '../../content/registrationTerms';
import AuthShell from './AuthShell';

const initialAgreements = Object.fromEntries(
  registrationTerms.map((term) => [term.id, false]),
) as Record<RegistrationTermId, boolean>;

function RegistrationTermsPage() {
  const [agreements, setAgreements] = useState(initialAgreements);
  const [checkingSession, setCheckingSession] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const submittingRef = useRef(false);
  const allRequired = registrationTerms
    .filter((term) => term.required)
    .every((term) => agreements[term.id]);
  const allAgreed = registrationTerms.every((term) => agreements[term.id]);

  useEffect(() => {
    getRegistrationTerms()
      .then(({ accepted, alarmConsent }) => {
        if (accepted) {
          setAgreements(Object.fromEntries(
            registrationTerms.map((term) => [
              term.id,
              term.required ? true : term.id === 'alarmConsent' && alarmConsent,
            ]),
          ) as Record<RegistrationTermId, boolean>);
        }
      })
      .catch((requestError) => {
        if (requestError instanceof ApiError && requestError.status === 401) {
          window.location.replace('/login?reason=registration-expired');
          return;
        }
        setError('가입 정보를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.');
      })
      .finally(() => setCheckingSession(false));
  }, []);

  const toggleAll = (checked: boolean) => {
    setAgreements(Object.fromEntries(
      registrationTerms.map((term) => [term.id, checked]),
    ) as Record<RegistrationTermId, boolean>);
  };

  const toggleTerm = (id: RegistrationTermId, checked: boolean) => {
    setAgreements((current) => ({ ...current, [id]: checked }));
  };

  const handleContinue = async () => {
    if (!allRequired || submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    setError('');
    try {
      await acceptRegistrationTerms({
        accepted: allRequired,
        alarmConsent: agreements.alarmConsent ?? false,
      });
      window.location.assign('/login/detail');
    } catch (requestError) {
      if (requestError instanceof ApiError) {
        if (requestError.status === 401) {
          window.location.replace('/login?reason=registration-expired');
          return;
        }
        setError(requestError.messages.join(' '));
      } else {
        setError('약관 동의를 저장하지 못했습니다. 다시 시도해주세요.');
      }
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  if (checkingSession) {
    return <div className="auth-loading"><LoaderCircle className="spin" aria-hidden="true" /><span>가입 정보를 확인하고 있습니다.</span></div>;
  }

  return (
    <AuthShell step="회원가입 2 / 4">
      <main className="auth-container terms-layout">
        <header className="terms-heading">
          <p className="auth-kicker">TERMS &amp; PRIVACY</p>
          <h1>서비스 이용을 위해<br />약관에 동의해주세요.</h1>
          <p>필수 항목에 모두 동의한 후 회원정보 입력을 계속할 수 있습니다.</p>
        </header>

        <div className="signup-progress" aria-label="가입 진행 단계">
          <span className="done"><Check size={14} /> Google 인증</span>
          <i />
          <span className="active">약관동의</span>
          <i />
          <span>추가정보 입력</span>
          <i />
          <span>가입 완료</span>
        </div>

        <section className="terms-card" aria-labelledby="terms-title">
          {error && <p className="form-alert" role="alert">{error}</p>}

          <label className="terms-all-check">
            <input type="checkbox" checked={allAgreed} onChange={(event) => toggleAll(event.target.checked)} />
            <span><strong id="terms-title">전체 약관 동의</strong><small>선택 항목을 포함한 아래 약관 전체에 동의합니다.</small></span>
          </label>

          <div className="terms-documents">
            {registrationTerms.map((term) => (
              <article className="terms-document" key={term.id}>
                <header>
                  <div>
                    <span>{term.stitle}</span>
                    <h2>{term.title}</h2>
                  </div>
                </header>

                <div className="terms-document-content" tabIndex={0} aria-label={`${term.title} 내용`}>
                  <p>{term.content}</p>
                </div>

                <label className="terms-document-consent">
                  <input
                    type="checkbox"
                    checked={agreements[term.id]}
                    onChange={(event) => toggleTerm(term.id, event.target.checked)}
                  />
                  <span><b>{term.required ? '[필수]' : '[선택]'}</b> {term.button}</span>
                </label>
              </article>
            ))}
          </div>

          <div className="terms-security-note">
            <ShieldCheck size={18} />
            <span>동의 여부는 가입 완료 전까지 임시 가입정보와 함께 안전하게 보관됩니다.</span>
          </div>

          <div className="terms-actions">
            <a href="/login"><ArrowLeft size={18} /> 로그인으로 돌아가기</a>
            <button type="button" onClick={handleContinue} disabled={!allRequired || submitting}>
              {submitting ? <><LoaderCircle className="spin" size={18} /> 처리 중</> : <>동의하고 계속하기 <ChevronRight size={19} /></>}
            </button>
          </div>
        </section>
      </main>
    </AuthShell>
  );
}

export default RegistrationTermsPage;
