package ru.yandex.practicum.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.*;
import ru.yandex.practicum.commerce.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.warehouse.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.warehouse.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.warehouse.model.OrderBooking;
import ru.yandex.practicum.warehouse.model.WarehouseProduct;
import ru.yandex.practicum.warehouse.repository.OrderBookingRepository;
import ru.yandex.practicum.warehouse.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private static final String[] ADDRESSES = {"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS =
            ADDRESSES[Random.from(new SecureRandom()).nextInt(0, ADDRESSES.length)];

    private final WarehouseProductRepository repository;
    private final OrderBookingRepository bookingRepository;

    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        if (repository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар уже зарегистрирован на складе: " + request.getProductId());
        }
        WarehouseProduct product = WarehouseProduct.builder()
                .productId(request.getProductId())
                .fragile(request.isFragile())
                .weight(request.getWeight())
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .quantity(0L)
                .build();
        repository.save(product);
    }

    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        WarehouseProduct product = findProductOrThrow(request.getProductId());
        product.setQuantity(product.getQuantity() + request.getQuantity());
        repository.save(product);
    }

    public void checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        Map<UUID, Long> requested = cart.getProducts();
        List<WarehouseProduct> warehouseProducts = repository.findAllById(requested.keySet());

        Set<UUID> foundIds = new HashSet<>();
        warehouseProducts.forEach(p -> foundIds.add(p.getProductId()));

        for (UUID productId : requested.keySet()) {
            if (!foundIds.contains(productId)) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Товар отсутствует на складе: " + productId);
            }
        }

        for (WarehouseProduct wp : warehouseProducts) {
            long requestedQty = requested.get(wp.getProductId());
            if (wp.getQuantity() < requestedQty) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Недостаточно товара: " + wp.getProductId()
                                + " (запрошено: " + requestedQty
                                + ", доступно: " + wp.getQuantity() + ")");
            }
        }
    }

    public AddressDto getWarehouseAddress() {
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }

    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        Map<UUID, Long> requested = request.getProducts();
        List<WarehouseProduct> warehouseProducts = repository.findAllById(requested.keySet());

        Set<UUID> foundIds = new HashSet<>();
        warehouseProducts.forEach(p -> foundIds.add(p.getProductId()));
        for (UUID id : requested.keySet()) {
            if (!foundIds.contains(id)) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Товар отсутствует на складе: " + id);
            }
        }

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (WarehouseProduct wp : warehouseProducts) {
            long qty = requested.get(wp.getProductId());
            if (wp.getQuantity() < qty) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Недостаточно товара: " + wp.getProductId()
                                + " (запрошено: " + qty
                                + ", доступно: " + wp.getQuantity() + ")");
            }
            wp.setQuantity(wp.getQuantity() - qty);
            totalWeight += wp.getWeight() * qty;
            totalVolume += wp.getWidth() * wp.getHeight() * wp.getDepth() * qty;
            if (wp.isFragile()) hasFragile = true;
        }

        repository.saveAll(warehouseProducts);

        OrderBooking booking = OrderBooking.builder()
                .orderId(request.getOrderId())
                .products(new HashMap<>(requested))
                .build();
        bookingRepository.save(booking);

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    @Transactional
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        OrderBooking booking = bookingRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Бронирование не найдено для заказа: " + request.getOrderId()));
        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);
    }

    @Transactional
    public void acceptReturn(Map<UUID, Long> returning) {
        List<WarehouseProduct> warehouseProducts = repository.findAllById(returning.keySet());
        for (WarehouseProduct wp : warehouseProducts) {
            wp.setQuantity(wp.getQuantity() + returning.get(wp.getProductId()));
        }
        repository.saveAll(warehouseProducts);
    }

    private WarehouseProduct findProductOrThrow(UUID productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Товар не найден на складе: " + productId));
    }
}