import Calculator from 'lucide-react/dist/esm/icons/calculator.js';
import Clock3 from 'lucide-react/dist/esm/icons/clock-3.js';
import PackageCheck from 'lucide-react/dist/esm/icons/package-check.js';
import { useLocale } from '../../locale';
import { useHomepageContent } from '../../homepage/HomepageContentContext';
import { managedHref } from '../../homepage/managedLink';

function ServiceStrip() {
  const { copy } = useLocale();
  const managed = useHomepageContent('OTHER_BANNER');
  const icons = [PackageCheck, Calculator, Clock3];
  const items = managed.length ? managed : copy.service.map((item,index)=>({id:index,title:item.title,subtitle:item.body,linkUrl:null,imageUrl:null}));
  return (
    <section className="service-strip" id="service" aria-label={copy.header.navigation[1]}>
      {items.map((item,index)=>{const Icon=icons[index%icons.length];const body=<><Icon size={22}/><span><strong>{item.title}</strong>{item.subtitle}</span></>;return item.linkUrl?<a key={item.id} href={managedHref(item.linkUrl)}>{body}</a>:<div key={item.id}>{body}</div>})}
    </section>
  );
}

export default ServiceStrip;
