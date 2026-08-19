import { HomepageContentManagementPage } from '../HomepageContentManagementPage';
export function PopupManagementPage(){return <HomepageContentManagementPage config={{endpoint:'popups',title:'팝업관리',description:'홈페이지에 노출할 팝업을 만들고 기간과 공개 상태를 관리합니다.',itemName:'팝업',subtitleLabel:'짧은 안내',contentLabel:'팝업 내용',image:true,link:true,linkLabel:false,schedule:true,previewPath:()=>`/?previewPopup=1`}}/>}
