package com.mbpal.api.controller;

import com.mbpal.api.dto.request.CreateSupportRequest;
import com.mbpal.api.dto.request.UpdateSupportRequest;
import com.mbpal.api.dto.response.SupportResponse;
import com.mbpal.service.SupportTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/supports")
public class SupportTypeController {

    private final SupportTypeService supportTypeService;

    public SupportTypeController(SupportTypeService supportTypeService) {
        this.supportTypeService = supportTypeService;
    }

    @GetMapping
    public ResponseEntity<List<SupportResponse>> listSupports(
            @RequestParam(required = false) String active) {
        return ResponseEntity.ok(supportTypeService.listSupports(active));
    }

    @GetMapping("/{supportCode}")
    public ResponseEntity<SupportResponse> getSupport(@PathVariable String supportCode) {
        return ResponseEntity.ok(supportTypeService.getSupport(supportCode));
    }

    @PostMapping
    public ResponseEntity<SupportResponse> createSupport(@Valid @RequestBody CreateSupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTypeService.createSupport(request));
    }

    @PutMapping("/{supportCode}")
    public ResponseEntity<SupportResponse> updateSupport(
            @PathVariable String supportCode,
            @Valid @RequestBody UpdateSupportRequest request) {
        return ResponseEntity.ok(supportTypeService.updateSupport(supportCode, request));
    }
}
