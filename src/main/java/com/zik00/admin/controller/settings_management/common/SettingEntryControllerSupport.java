package com.zik00.admin.controller.settings_management.common;

import com.zik00.admin.dto.settings_management.common.SettingEntryRequest;
import com.zik00.admin.dto.settings_management.common.SettingEntryResponse;
import com.zik00.admin.service.settings_management.common.SettingEntryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

public abstract class SettingEntryControllerSupport {
    private final SettingEntryService service; private final String type; private final boolean singleton;
    protected SettingEntryControllerSupport(SettingEntryService service,String type,boolean singleton){this.service=service;this.type=type;this.singleton=singleton;}
    @GetMapping public List<SettingEntryResponse> findAll(){return service.findAll(type);}
    @PostMapping public SettingEntryResponse create(@Valid @RequestBody SettingEntryRequest request){return service.create(type,request,singleton);}
    @PutMapping("/{id}") public SettingEntryResponse update(@PathVariable long id,@Valid @RequestBody SettingEntryRequest request){return service.update(type,id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable long id){service.delete(type,id);return ResponseEntity.noContent().build();}
}
