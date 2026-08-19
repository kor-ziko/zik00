package com.zik00.shop.service.service_intro;

import com.zik00.shop.domain.service_intro.ServiceIntroSection;
import com.zik00.shop.dto.service_intro.ServiceIntroResponse;
import com.zik00.shop.dto.service_intro.ServiceIntroSectionResponse;
import com.zik00.shop.repository.service_intro.ServiceIntroSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ServiceIntroService {
    private final ServiceIntroSectionRepository repository;

    public ServiceIntroService(ServiceIntroSectionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ServiceIntroResponse findPage() {
        List<ServiceIntroSection> sections = repository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
        LocalDateTime updatedAt = sections.stream()
                .map(ServiceIntroSection::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new ServiceIntroResponse(
                sections.stream().map(ServiceIntroSectionResponse::from).toList(),
                updatedAt
        );
    }
}
