import { SettingsEntryPage } from '../SettingsEntryPage';

export function RegistrationTermsPage() {
  return (
    <SettingsEntryPage
      config={{
        endpoint: 'registration-terms',
        title: '회원가입 약관 관리',
        description: '회원가입 단계에 표시할 약관과 필수 동의 여부를 관리합니다.',
        itemName: '회원가입 약관',
        codeLabel: '약관 코드',
        nameLabel: '약관 제목',
        contentLabel: '약관 본문',
        contentRequired: true,
        fields: [
          { key: 'shortTitle', label: '약관 구분명', placeholder: '예: 이용약관', required: true },
          { key: 'consentLabel', label: '동의 문구', placeholder: '예: 서비스 이용약관에 동의함', required: true },
          {
            key: 'requirement',
            label: '필수 여부',
            type: 'select',
            placeholder: '필수 여부를 선택하세요',
            required: true,
            options: [['REQUIRED', '필수'], ['OPTIONAL', '선택']],
          },
        ],
      }}
    />
  );
}
