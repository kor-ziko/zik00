package com.zik00.shop.controller.notice;

import com.zik00.shop.dto.notice.NoticeDetailResponse;
import com.zik00.shop.dto.notice.NoticeListResponse;
import com.zik00.shop.service.notice.NoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public NoticeListResponse findNotices(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return noticeService.findNotices(category, page, size);
    }

    @GetMapping("/{noticeId}")
    public NoticeDetailResponse findNotice(@PathVariable long noticeId) {
        return noticeService.findNotice(noticeId);
    }
}
