import ExternalLink from 'lucide-react/dist/esm/icons/external-link.js';
import { useHomepageContent } from '../../homepage/HomepageContentContext';
import { isExternalHref, managedHref } from '../../homepage/managedLink';

function RecommendedSites(){
  const sites=useHomepageContent('RECOMMENDED_SITE');
  if(!sites.length)return null;
  return <section className="recommended-sites"><div className="section-heading"><div><span>ZIK:00 PICKS</span><h2>추천 쇼핑 사이트</h2></div></div><div className="recommended-site-grid">{sites.map(site=>{const href=managedHref(site.linkUrl);return <a key={site.id} href={href} target={isExternalHref(href)?'_blank':undefined} rel={isExternalHref(href)?'noreferrer':undefined}><div>{site.imageUrl?<img src={site.imageUrl} alt=""/>:<span>{site.title.slice(0,1)}</span>}</div><strong>{site.title}</strong><p>{site.subtitle}</p><ExternalLink size={16}/></a>})}</div></section>;
}
export default RecommendedSites;
