import termsJson from './registrationTerms.json';

export type RegistrationTermId = string;

export type RegistrationTerm = {
  id: RegistrationTermId;
  stitle: string;
  title: string;
  content: string;
  button: string;
  required: boolean;
};

function requireText(value: unknown, field: string): string {
  if (typeof value !== 'string' || value.trim() === '') {
    throw new Error(`registrationTerms.json의 ${field} 값이 비어 있습니다.`);
  }
  return value.trim();
}

const entries = Object.entries(termsJson as Record<string, unknown>);
if (entries.length === 0) {
  throw new Error('registrationTerms.json에 약관이 하나 이상 필요합니다.');
}

export const registrationTerms: RegistrationTerm[] = entries.map(([id, value]) => {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`registrationTerms.json의 ${id} 항목 형식이 올바르지 않습니다.`);
  }
  const rawTerm = value as Record<string, unknown>;
  return {
    id,
    stitle: requireText(rawTerm.stitle, `${id}.stitle`),
    title: requireText(rawTerm.title, `${id}.title`),
    content: requireText(rawTerm.content, `${id}.content`),
    button: requireText(rawTerm.button, `${id}.button`),
    required: rawTerm.required !== false,
  };
});
