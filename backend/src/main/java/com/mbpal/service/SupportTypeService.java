package com.mbpal.service;

import com.mbpal.api.dto.request.CreateSupportRequest;
import com.mbpal.api.dto.request.UpdateSupportRequest;
import com.mbpal.api.dto.response.SupportResponse;
import com.mbpal.api.exception.ConflictException;
import com.mbpal.api.exception.ResourceNotFoundException;
import com.mbpal.domain.entity.SupportType;
import com.mbpal.repository.SupportTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupportTypeService {

    private final SupportTypeRepository supportTypeRepository;

    public SupportTypeService(SupportTypeRepository supportTypeRepository) {
        this.supportTypeRepository = supportTypeRepository;
    }

    public List<SupportResponse> listSupports(String active) {
        List<SupportType> supports;
        if (active != null) {
            supports = supportTypeRepository.findByActiveFlag(active);
        } else {
            supports = supportTypeRepository.findAll();
        }
        return supports.stream().map(this::toResponse).toList();
    }

    public SupportResponse getSupport(String supportCode) {
        SupportType support = supportTypeRepository.findBySupportCode(supportCode)
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", "supportCode", supportCode));
        return toResponse(support);
    }

    @Transactional
    public SupportResponse createSupport(CreateSupportRequest request) {
        supportTypeRepository.findBySupportCode(request.supportCode())
                .ifPresent(s -> {
                    throw new ConflictException("SupportType", "supportCode", request.supportCode());
                });

        SupportType support = SupportType.builder()
                .supportCode(request.supportCode())
                .label(request.label())
                .lengthMm(request.lengthMm())
                .widthMm(request.widthMm())
                .heightMm(request.heightMm())
                .maxLoadKg(request.maxLoadKg())
                .maxTotalHeightMm(request.maxTotalHeightMm())
                .mergeableFlag(request.mergeableFlag() != null ? request.mergeableFlag() : "N")
                .mergeTargetCode(request.mergeTargetCode())
                .mergeQuantity(request.mergeQuantity() != null ? request.mergeQuantity() : 2)
                .build();

        return toResponse(supportTypeRepository.save(support));
    }

    @Transactional
    public SupportResponse updateSupport(String supportCode, UpdateSupportRequest request) {
        SupportType support = supportTypeRepository.findBySupportCode(supportCode)
                .orElseThrow(() -> new ResourceNotFoundException("SupportType", "supportCode", supportCode));

        support.setLabel(request.label());
        support.setLengthMm(request.lengthMm());
        support.setWidthMm(request.widthMm());
        support.setHeightMm(request.heightMm());
        support.setMaxLoadKg(request.maxLoadKg());
        support.setMaxTotalHeightMm(request.maxTotalHeightMm());
        if (request.mergeableFlag() != null) {
            support.setMergeableFlag(request.mergeableFlag());
        }
        support.setMergeTargetCode(request.mergeTargetCode());
        if (request.mergeQuantity() != null) {
            support.setMergeQuantity(request.mergeQuantity());
        }

        return toResponse(supportTypeRepository.save(support));
    }

    private SupportResponse toResponse(SupportType support) {
        return new SupportResponse(
                support.getSupportTypeId(),
                support.getSupportCode(),
                support.getLabel(),
                support.getLengthMm(),
                support.getWidthMm(),
                support.getHeightMm(),
                support.getMaxLoadKg(),
                support.getMaxTotalHeightMm(),
                support.getMergeableFlag(),
                support.getMergeTargetCode(),
                "Y".equals(support.getActiveFlag())
        );
    }
}
