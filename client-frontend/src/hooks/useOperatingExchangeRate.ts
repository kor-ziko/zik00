import { useEffect, useState } from 'react';
import { getOperatingExchangeRate } from '../api/product';

export function useOperatingExchangeRate() {
  const [rate, setRate] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    getOperatingExchangeRate().then((result) => {
      if (active) setRate(result.rate);
    }).catch(() => {
      if (active) setRate(null);
    });
    return () => { active = false; };
  }, []);

  return {
    rate,
    toJpy(value: number, currency: 'KRW' | 'JPY' | undefined) {
      if (currency === 'JPY') return value;
      return rate === null ? null : Math.ceil(value * rate);
    },
  };
}
