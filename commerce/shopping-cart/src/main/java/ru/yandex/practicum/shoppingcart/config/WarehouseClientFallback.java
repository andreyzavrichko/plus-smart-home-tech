package ru.yandex.practicum.shoppingcart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.dto.warehouse.AddProductToWarehouseRequest;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;
import ru.yandex.practicum.commerce.dto.warehouse.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.commerce.dto.warehouse.BookedProductsDto;
import ru.yandex.practicum.commerce.dto.warehouse.NewProductInWarehouseRequest;
import ru.yandex.practicum.commerce.dto.warehouse.ShippedToDeliveryRequest;
import ru.yandex.practicum.commerce.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.commerce.feign.WarehouseClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WarehouseClientFallback implements WarehouseClient {

    @Override
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        log.error("Fallback: warehouse недоступен — newProductInWarehouse");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        log.error("Fallback: warehouse недоступен — addProductToWarehouse");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public void checkProductQuantityEnoughForShoppingCart(ShoppingCartDto shoppingCart) {
        log.error("Fallback: warehouse недоступен — checkProductQuantityEnoughForShoppingCart");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Проверка наличия товаров невозможна. Попробуйте позже.");
    }

    @Override
    public AddressDto getWarehouseAddress() {
        log.error("Fallback: warehouse недоступен — getWarehouseAddress");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.error("Fallback: warehouse недоступен — assemblyProductsForOrder");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.error("Fallback: warehouse недоступен — shippedToDelivery");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }

    @Override
    public void acceptReturn(Map<UUID, Long> products) {
        log.error("Fallback: warehouse недоступен — acceptReturn");
        throw new ProductInShoppingCartLowQuantityInWarehouseException(
                "Сервис склада временно недоступен. Попробуйте позже.");
    }
}