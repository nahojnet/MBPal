package com.mbpal.service;

import com.mbpal.api.dto.request.CreateProductRequest;
import com.mbpal.api.dto.request.UpdateProductRequest;
import com.mbpal.api.dto.response.PageResponse;
import com.mbpal.api.dto.response.ProductResponse;
import com.mbpal.api.exception.ConflictException;
import com.mbpal.api.exception.ResourceNotFoundException;
import com.mbpal.domain.entity.Product;
import com.mbpal.domain.enums.TemperatureType;
import com.mbpal.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public PageResponse<ProductResponse> listProducts(TemperatureType temperatureType, String active, Pageable pageable) {
        Page<Product> page;
        if (temperatureType != null) {
            page = productRepository.findByTemperatureType(temperatureType, pageable);
        } else if (active != null) {
            page = productRepository.findByActiveFlag(active, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(this::toResponse));
    }

    public ProductResponse getProduct(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        productRepository.findByProductCode(request.productCode())
                .ifPresent(p -> {
                    throw new ConflictException("Product", "productCode", request.productCode());
                });

        Product product = Product.builder()
                .productCode(request.productCode())
                .label(request.label())
                .temperatureType(request.temperatureType())
                .lengthMm(request.lengthMm())
                .widthMm(request.widthMm())
                .heightMm(request.heightMm())
                .weightKg(request.weightKg())
                .fragilityLevel(request.fragilityLevel())
                .stackableFlag(request.stackableFlag())
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(String productCode, UpdateProductRequest request) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productCode", productCode));

        product.setLabel(request.label());
        product.setTemperatureType(request.temperatureType());
        product.setLengthMm(request.lengthMm());
        product.setWidthMm(request.widthMm());
        product.setHeightMm(request.heightMm());
        product.setWeightKg(request.weightKg());
        product.setFragilityLevel(request.fragilityLevel());
        product.setStackableFlag(request.stackableFlag());

        return toResponse(productRepository.save(product));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getProductId(),
                product.getProductCode(),
                product.getLabel(),
                product.getTemperatureType(),
                product.getLengthMm(),
                product.getWidthMm(),
                product.getHeightMm(),
                product.getWeightKg(),
                product.getFragilityLevel(),
                product.getStackableFlag(),
                product.isActive()
        );
    }
}
