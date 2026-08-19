export type ServiceIntroSection = {
  id: number;
  sectionType: 'HERO' | 'PROCESS' | 'VALUE' | string;
  eyebrow: string | null;
  title: string;
  content: string;
  detail: string | null;
  imageUrl: string | null;
  displayOrder: number;
};

export type ServiceIntroResponse = {
  sections: ServiceIntroSection[];
  updatedAt: string | null;
};

export async function getServiceIntro(signal?: AbortSignal): Promise<ServiceIntroResponse> {
  const response = await fetch('/api/service-intro', { signal, credentials: 'include' });
  if (!response.ok) throw new Error('서비스 소개를 불러오지 못했습니다.');
  return response.json() as Promise<ServiceIntroResponse>;
}
