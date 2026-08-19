import { Coins, History, LogOut, PiggyBank, Users } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const pages = [
  ['/admin/members/list', '회원리스트', Users],
  ['/admin/members/withdrawn', '탈퇴회원', LogOut],
  ['/admin/members/reward-points', '적립포인트', Coins],
  ['/admin/members/deposit-requests', '예치금신청', PiggyBank],
  ['/admin/members/deposit-histories', '예치금사용내역', History],
] as const;

export function MemberManagementNav() {
  return <nav className="member-management-tabs" aria-label="회원 관리 메뉴">{pages.map(([to, label, Icon]) =>
    <NavLink key={to} to={to} className={({ isActive }) => `member-management-tab${isActive ? ' active' : ''}`}>
      <Icon size={16} /><span>{label}</span>
    </NavLink>)}</nav>;
}

export function MemberPageHeader({ title, description }: { title: string; description: string }) {
  return <><div className="admin-page-header"><div><p className="admin-eyebrow">회원 관리</p><h1>{title}</h1><p>{description}</p></div></div><MemberManagementNav /></>;
}
