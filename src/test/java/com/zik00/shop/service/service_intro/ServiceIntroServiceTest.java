package com.zik00.shop.service.service_intro;

import com.zik00.shop.domain.service_intro.ServiceIntroSection;
import com.zik00.shop.repository.service_intro.ServiceIntroSectionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceIntroServiceTest {
    @Test
    void returnsOnlyRepositorySectionsInDisplayOrder() {
        ServiceIntroSection first = mock(ServiceIntroSection.class);
        ServiceIntroSection second = mock(ServiceIntroSection.class);
        when(first.getId()).thenReturn(1L);
        when(first.getSectionType()).thenReturn("HERO");
        when(first.getTitle()).thenReturn("서비스 소개");
        when(first.getContent()).thenReturn("소개 내용");
        when(first.getDisplayOrder()).thenReturn(1);
        when(second.getId()).thenReturn(2L);
        when(second.getSectionType()).thenReturn("PROCESS");
        when(second.getTitle()).thenReturn("상품 찾기");
        when(second.getContent()).thenReturn("진행 내용");
        when(second.getDisplayOrder()).thenReturn(2);

        ServiceIntroSectionRepository repository = mock(ServiceIntroSectionRepository.class);
        when(repository.findByActiveTrueOrderByDisplayOrderAscIdAsc()).thenReturn(List.of(first, second));

        var response = new ServiceIntroService(repository).findPage();

        assertThat(response.sections()).extracting("title").containsExactly("서비스 소개", "상품 찾기");
    }
}
