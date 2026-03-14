package ru.yandex.practicum.shoppingcart.model;

import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;

public final class ShoppingCartMapper {

    private ShoppingCartMapper() {
    }

    public static ShoppingCartDto toDto(ShoppingCart cart) {
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .build();
    }
}