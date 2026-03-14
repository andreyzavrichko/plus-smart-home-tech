package ru.yandex.practicum.shoppingcart.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.commerce.dto.cart.ChangeProductQuantityRequest;
import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;
import ru.yandex.practicum.commerce.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.commerce.exception.NotAuthorizedUserException;
import ru.yandex.practicum.commerce.exception.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.commerce.feign.WarehouseClient;
import ru.yandex.practicum.shoppingcart.model.ShoppingCart;
import ru.yandex.practicum.shoppingcart.model.ShoppingCartMapper;
import ru.yandex.practicum.shoppingcart.repository.ShoppingCartRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final WarehouseClient warehouseClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public ShoppingCartDto getShoppingCart(String username) {
        validateUsername(username);
        ShoppingCart cart = getOrCreateActiveCart(username);
        return ShoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProducts(String username, Map<UUID, Long> products) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        products.forEach((productId, qty) ->
                cart.getProducts().merge(productId, qty, Long::sum));

        ShoppingCartDto cartDto = ShoppingCartMapper.toDto(cart);
        checkWarehouseAvailability(cartDto);

        cartRepository.save(cart);
        return ShoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto removeProducts(String username, List<UUID> productIds) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        boolean anyFound = productIds.stream()
                .anyMatch(id -> cart.getProducts().containsKey(id));

        if (!anyFound) {
            throw new NoProductsInShoppingCartException(
                    "Ни один из указанных товаров не найден в корзине");
        }

        productIds.forEach(cart.getProducts()::remove);
        cartRepository.save(cart);
        return ShoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeQuantity(String username, ChangeProductQuantityRequest request) {
        validateUsername(username);

        ShoppingCart cart = getOrCreateActiveCart(username);

        if (!cart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException(
                    "Товар не найден в корзине: " + request.getProductId());
        }

        cart.getProducts().put(request.getProductId(), request.getNewQuantity());
        cartRepository.save(cart);
        return ShoppingCartMapper.toDto(cart);
    }

    @Transactional
    public void deactivateCart(String username) {
        validateUsername(username);

        cartRepository.findByUsernameAndActiveTrue(username)
                .ifPresent(cart -> {
                    cart.setActive(false);
                    cartRepository.save(cart);
                    log.info("Корзина деактивирована для пользователя: {}", username);
                });
    }


    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Имя пользователя не должно быть пустым");
        }
    }

    private ShoppingCart getOrCreateActiveCart(String username) {
        return cartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> {
                    log.info("Создаём новую корзину для пользователя: {}", username);
                    return cartRepository.save(ShoppingCart.builder()
                            .username(username)
                            .active(true)
                            .build());
                });
    }

    private void checkWarehouseAvailability(ShoppingCartDto cartDto) {
        circuitBreakerFactory.create("warehouse").run(
                () -> {
                    warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);
                    return null;
                },
                throwable -> {
                    log.error("Warehouse недоступен или вернул ошибку: {}", throwable.getMessage());
                    throw new ProductInShoppingCartLowQuantityInWarehouseException(
                            "Сервис склада временно недоступен. Попробуйте позже.");
                }
        );
    }
}