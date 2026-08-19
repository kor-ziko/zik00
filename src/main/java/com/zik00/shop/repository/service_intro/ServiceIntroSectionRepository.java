package com.zik00.shop.repository.service_intro;

import com.zik00.shop.domain.service_intro.ServiceIntroSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceIntroSectionRepository extends JpaRepository<ServiceIntroSection, Long> {
    List<ServiceIntroSection> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
