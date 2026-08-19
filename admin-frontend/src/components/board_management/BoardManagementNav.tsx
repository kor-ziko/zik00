import { FilePenLine, FilePlus2, MessageSquareText, Star } from 'lucide-react';
import { NavLink } from 'react-router-dom';

const pages = [
  { to: '/admin/boards/notices/new', label: '공지사항 등록', icon: FilePlus2 },
  { to: '/admin/boards/notices', label: '공지사항 관리', icon: FilePenLine },
  { to: '/admin/boards/inquiries', label: '1:1문의 관리', icon: MessageSquareText },
  { to: '/admin/boards/reviews', label: '리뷰 관리', icon: Star },
];

export function BoardManagementNav() {
  return (
    <nav className="board-tabs" aria-label="게시판 관리 메뉴">
      {pages.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/admin/boards/notices'}
          className={({ isActive }) => `board-tab${isActive ? ' active' : ''}`}
        >
          <Icon size={17} />
          <span>{label}</span>
        </NavLink>
      ))}
    </nav>
  );
}

export function BoardPageHeader({ title, description }: { title: string; description: string }) {
  return (
    <>
      <div className="admin-page-header">
        <div><p className="admin-eyebrow">게시판 관리</p><h1>{title}</h1><p>{description}</p></div>
      </div>
      <BoardManagementNav />
    </>
  );
}
