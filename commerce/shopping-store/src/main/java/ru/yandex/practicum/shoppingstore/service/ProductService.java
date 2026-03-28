package ru.yandex.practicum.shoppingstore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.product.ProductCategory;
import ru.yandex.practicum.commerce.dto.product.ProductDto;
import ru.yandex.practicum.commerce.dto.product.ProductState;
import ru.yandex.practicum.commerce.dto.product.QuantityState;
import ru.yandex.practicum.commerce.exception.ProductNotFoundException;
import ru.yandex.practicum.shoppingstore.model.Product;
import ru.yandex.practicum.shoppingstore.model.ProductMapper;
import ru.yandex.practicum.shoppingstore.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        return productRepository
                .findByProductCategory(category, pageable)
                .map(ProductMapper::toDto);
    }

    public ProductDto getProduct(UUID productId) {
        return productRepository.findById(productId)
                .map(ProductMapper::toDto)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Товар не найден: " + productId));
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        if (dto.getProductId() == null) {
            dto = ProductDto.builder()
                    .productId(UUID.randomUUID())
                    .productName(dto.getProductName())
                    .description(dto.getDescription())
                    .imageSrc(dto.getImageSrc())
                    .quantityState(dto.getQuantityState())
                    .productState(dto.getProductState())
                    .productCategory(dto.getProductCategory())
                    .price(dto.getPrice())
                    .build();
        }
        Product saved = productRepository.save(ProductMapper.toEntity(dto));
        return ProductMapper.toDto(saved);
    }

    @Transactional
    public ProductDto updateProduct(ProductDto dto) {
        productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Товар не найден: " + dto.getProductId()));
        Product saved = productRepository.save(ProductMapper.toEntity(dto));
        return ProductMapper.toDto(saved);
    }

    @Transactional
    public boolean removeProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Товар не найден: " + productId));
        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        return true;
    }

    @Transactional
    public boolean setQuantityState(UUID productId, QuantityState quantityState) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Товар не найден: " + productId));
        product.setQuantityState(quantityState);
        productRepository.save(product);
        return true;
    }
}