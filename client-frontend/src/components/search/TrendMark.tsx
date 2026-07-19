import type { Trend } from '../../data';

type TrendMarkProps = {
  trend: Trend;
};

function TrendMark({ trend }: TrendMarkProps) {
  if (trend === 'new') {
    return <span className="trend trend-new">NEW</span>;
  }

  const label = trend === 'up' ? '상승' : trend === 'down' ? '하락' : '변동 없음';

  return (
    <span className={`trend trend-${trend}`} aria-label={label}>
      {trend === 'up' ? '▲' : trend === 'down' ? '▼' : '−'}
    </span>
  );
}

export default TrendMark;
