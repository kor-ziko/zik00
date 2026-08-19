package com.zik00.shop.service.notice;

import com.zik00.shop.domain.notice.Notice;
import com.zik00.shop.repository.notice.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoticeServiceTest {
    @Test
    void returnsPublishedNoticePageAndCategories() {
        Notice notice = mock(Notice.class);
        when(notice.getId()).thenReturn(1L);
        when(notice.getCategory()).thenReturn("안내");
        when(notice.getTitle()).thenReturn("서비스 안내");
        when(notice.getPublishedAt()).thenReturn(LocalDateTime.of(2026, 8, 1, 9, 0));

        NoticeRepository repository = mock(NoticeRepository.class);
        when(repository.findByPublishedTrueOrderByPinnedDescPublishedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(notice), PageRequest.of(0, 10), 1));
        when(repository.findPublishedCategories()).thenReturn(List.of("배송", "안내"));

        var response = new NoticeService(repository).findNotices("전체", 0, 10);

        assertThat(response.items()).hasSize(1);
        assertThat(response.categories()).containsExactly("배송", "안내");
        assertThat(response.totalCount()).isEqualTo(1);
    }

    @Test
    void returnsNotFoundForUnpublishedOrMissingNotice() {
        NoticeRepository repository = mock(NoticeRepository.class);
        when(repository.findByIdAndPublishedTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new NoticeService(repository).findNotice(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
