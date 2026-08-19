import { FileWarning, GalleryHorizontal, Image, LayoutTemplate, PanelBottom, PackageSearch, PanelsTopLeft } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const pages = [
  ['/admin/homepage/main-banners', '메인배너관리', GalleryHorizontal],
  ['/admin/homepage/other-banners', '기타배너관리', PanelsTopLeft],
  ['/admin/homepage/popups', '팝업관리', LayoutTemplate],
  ['/admin/homepage/footer-copyright', '하단카피라이트', PanelBottom],
  ['/admin/homepage/precautions', '유의사항관리', FileWarning],
  ['/admin/homepage/recommended-sites', '추천사이트', Image],
  ['/admin/homepage/recommended-products', '추천상품', PackageSearch],
] as const;

export function WebManagementNav() {
  return <nav className="web-management-tabs" aria-label="홈페이지 관리 메뉴">{pages.map(([to,label,Icon]) => <NavLink key={to} to={to} className={({isActive}) => `web-management-tab${isActive?' active':''}`}><Icon size={16}/><span>{label}</span></NavLink>)}</nav>;
}

export function WebPageHeader({title,description}:{title:string;description:string}) {
  return <><div className="admin-page-header"><div><p className="admin-eyebrow">홈페이지 관리</p><h1>{title}</h1><p>{description}</p></div></div><WebManagementNav/></>;
}
