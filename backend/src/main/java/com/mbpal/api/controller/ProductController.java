package com.mbpal.api.controller;

import com.mbpal.api.dto.request.CreateProductRequest;
import com.mbpal.api.dto.request.UpdateProductRequest;
import com.mbpal.api.dto.response.PageResponse;
import com.mbpal.api.dto.response.ProductResponse;
import com.mbpal.domain.enums.TemperatureType;
import com.mbpal.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> listProducts(
            @RequestParam(required = false) TemperatureType temperatureType,
            @RequestParam(required = false) String active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listProducts(temperatureType, active, PageRequest.of(page, size)));
    }

    @GetMapping("/{productCode}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String productCode) {
        return ResponseEntity.ok(productService.getProduct(productCode));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{productCode}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String productCode,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(productCode, request));
    }
}
