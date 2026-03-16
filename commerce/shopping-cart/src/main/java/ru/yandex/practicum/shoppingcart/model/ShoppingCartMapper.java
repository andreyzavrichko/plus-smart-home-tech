package ru.yandex.practicum.shoppingcart.model;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.commerce.dto.cart.ShoppingCartDto;

@UtilityClass
public class ShoppingCartMapper {

    public ShoppingCartDto toDto(ShoppingCart cart) {
        return ShoppingCartDto.builder()
                .shoppingCartId(cart.getShoppingCartId())
                .products(cart.getProducts())
                .build();
    }
}