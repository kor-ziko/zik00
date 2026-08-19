const POSTCODE_SCRIPT_ID = 'kakao-postcode-script';
const POSTCODE_SCRIPT_URL = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';

export type KoreanAddressResult = { postalCode: string; baseAddress: string };

type KakaoPostcodeData = {
  zonecode: string;
  address: string;
  roadAddress: string;
  jibunAddress: string;
  bname: string;
  buildingName: string;
  apartment: 'Y' | 'N';
};

type KakaoPostcodeConstructor = new (options: {
  oncomplete: (data: KakaoPostcodeData) => void;
}) => { open: () => void };

declare global {
  interface Window {
    kakao?: { Postcode?: KakaoPostcodeConstructor };
  }
}

let loadingScript: Promise<void> | null = null;

export function loadKoreanPostcode(): Promise<void> {
  if (window.kakao?.Postcode) return Promise.resolve();
  if (loadingScript) return loadingScript;

  document.getElementById(POSTCODE_SCRIPT_ID)?.remove();
  const promise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    const timeout = window.setTimeout(
      () => reject(new Error('주소 검색 서비스 연결 시간이 초과되었습니다.')),
      10_000,
    );
    const complete = () => {
      window.clearTimeout(timeout);
      if (window.kakao?.Postcode) resolve();
      else reject(new Error('주소 검색 서비스를 불러오지 못했습니다.'));
    };

    script.addEventListener('load', complete, { once: true });
    script.addEventListener('error', () => {
      window.clearTimeout(timeout);
      reject(new Error('주소 검색 서비스를 불러오지 못했습니다.'));
    }, { once: true });
    script.id = POSTCODE_SCRIPT_ID;
    script.src = POSTCODE_SCRIPT_URL;
    script.async = true;
    document.head.appendChild(script);
  }).catch((error): never => {
    document.getElementById(POSTCODE_SCRIPT_ID)?.remove();
    loadingScript = null;
    throw error;
  });

  loadingScript = promise;
  return promise;
}

export async function openKoreanPostcode(onComplete: (result: KoreanAddressResult) => void) {
  await loadKoreanPostcode();
  const Postcode = window.kakao?.Postcode;
  if (!Postcode) throw new Error('주소 검색 서비스를 사용할 수 없습니다.');

  new Postcode({
    oncomplete: (data) => {
      const baseAddress = data.roadAddress || data.address || data.jibunAddress;
      const extras: string[] = [];
      if (data.bname && /[동로가]$/.test(data.bname)) extras.push(data.bname);
      if (data.buildingName && data.apartment === 'Y') extras.push(data.buildingName);
      onComplete({
        postalCode: data.zonecode,
        baseAddress: extras.length ? `${baseAddress} (${extras.join(', ')})` : baseAddress,
      });
    },
  }).open();
}
