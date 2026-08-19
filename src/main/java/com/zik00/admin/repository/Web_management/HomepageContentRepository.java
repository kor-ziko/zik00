package com.zik00.admin.repository.Web_management;

import com.zik00.admin.domain.Web_management.HomepageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomepageContentRepository extends JpaRepository<HomepageContent, Long> {
    List<HomepageContent> findByContentTypeOrderByDisplayOrderAscIdAsc(String contentType);
    List<HomepageContent> findByContentTypeOrderByApplicationTypeAscDisplayOrderAscIdAsc(String contentType);
    List<HomepageContent> findByActiveTrueOrderByContentTypeAscDisplayOrderAscIdAsc();

    @Query("select coalesce(max(content.displayOrder), 0) from HomepageContent content " +
            "where content.contentType = :contentType and content.applicationType = :applicationType")
    int findMaxDisplayOrder(@Param("contentType") String contentType,
                            @Param("applicationType") String applicationType);
}
