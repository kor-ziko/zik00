import Calculator from 'lucide-react/dist/esm/icons/calculator.js';
import Clock3 from 'lucide-react/dist/esm/icons/clock-3.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import { useLocale } from '../../locale';

function ServiceStrip() {
  const { copy } = useLocale();
  return (
    <section className="service-strip" id="service" aria-label={copy.header.navigation[1]}>
      <div><PackageCheck size={22} /><span><strong>{copy.service[0].title}</strong>{copy.service[0].body}</span></div>
      <div><Calculator size={22} /><span><strong>{copy.service[1].title}</strong>{copy.service[1].body}</span></div>
      <div><Clock3 size={22} /><span><strong>{copy.service[2].title}</strong>{copy.service[2].body}</span></div>
    </section>
  );
}

export default ServiceStrip;
