package com.zik00.admin.service.Web_management.site_page;

import com.zik00.admin.dto.Web_management.site_page.SitePageResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SitePageCatalogService {
    private static final List<SitePageResponse> PAGES = List.of(
            new SitePageResponse("홈", "/", "기본 페이지"),
            new SitePageResponse("서비스 소개", "/service-intro", "기본 페이지"),
            new SitePageResponse("리뷰", "/reviews", "기본 페이지"),
            new SitePageResponse("공지사항", "/notices", "기본 페이지"),
            new SitePageResponse("찜", "/wishlist", "쇼핑"),
            new SitePageResponse("장바구니", "/cart", "쇼핑"),
            new SitePageResponse("마이페이지", "/mypage", "회원"),
            new SitePageResponse("구매대행 유의사항", "/precautions?type=PURCHASE_AGENCY", "신청 안내"),
            new SitePageResponse("배송대행 유의사항", "/precautions?type=DELIVERY_AGENCY", "신청 안내")
    );

    public List<SitePageResponse> findAll() {
        return PAGES;
    }
}
