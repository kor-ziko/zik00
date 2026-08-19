import { PackageSearch } from 'lucide-react';
import { WebPageHeader } from '../WebManagementNav';
export function RecommendedProductPage(){return <section className="admin-page"><WebPageHeader title="추천상품" description="홈페이지에 노출할 추천상품을 관리하는 화면입니다."/><div className="web-product-placeholder"><PackageSearch size={34}/><h2>추천상품 관리</h2><p>상품 연결 기능은 다음 작업에서 추가할 예정입니다.</p></div></section>}
