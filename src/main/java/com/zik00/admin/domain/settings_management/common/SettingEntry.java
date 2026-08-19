package com.zik00.admin.domain.settings_management.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "admin_setting_entries")
public class SettingEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id") private Long id;
    @Column(name = "setting_type", nullable = false, length = 40) private String type;
    @Column(nullable = false, length = 100) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Lob @Column(columnDefinition = "LONGTEXT") private String content;
    @Lob @Column(name = "field_data", nullable = false, columnDefinition = "LONGTEXT") private String fieldData;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected SettingEntry() {}
    public SettingEntry(String type, String code, String name, String content, String fieldData, int displayOrder, boolean active) {
        update(type, code, name, content, fieldData, displayOrder, active);
    }
    public void update(String type, String code, String name, String content, String fieldData, int displayOrder, boolean active) {
        this.type=type; this.code=code; this.name=name; this.content=content; this.fieldData=fieldData; this.displayOrder=displayOrder; this.active=active;
    }
    @PrePersist void create(){createdAt=LocalDateTime.now();updatedAt=createdAt;}
    @PreUpdate void updateTime(){updatedAt=LocalDateTime.now();}
}
