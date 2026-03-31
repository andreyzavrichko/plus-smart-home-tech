package ru.yandex.practicum.delivery.model;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.commerce.dto.delivery.DeliveryState;
import ru.yandex.practicum.commerce.dto.warehouse.AddressDto;

import java.util.UUID;

@Entity
@Table(name = "deliveries", schema = "delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_state", nullable = false)
    private DeliveryState deliveryState;

    @Column(name = "from_country")
    private String fromCountry;
    @Column(name = "from_city")
    private String fromCity;
    @Column(name = "from_street")
    private String fromStreet;
    @Column(name = "from_house")
    private String fromHouse;
    @Column(name = "from_flat")
    private String fromFlat;

    @Column(name = "to_country")
    private String toCountry;
    @Column(name = "to_city")
    private String toCity;
    @Column(name = "to_street")
    private String toStreet;
    @Column(name = "to_house")
    private String toHouse;
    @Column(name = "to_flat")
    private String toFlat;

    public void setFromAddress(AddressDto a) {
        this.fromCountry = a.getCountry();
        this.fromCity = a.getCity();
        this.fromStreet = a.getStreet();
        this.fromHouse = a.getHouse();
        this.fromFlat = a.getFlat();
    }

    public AddressDto getFromAddress() {
        return AddressDto.builder()
                .country(fromCountry).city(fromCity)
                .street(fromStreet).house(fromHouse).flat(fromFlat)
                .build();
    }

    public void setToAddress(AddressDto a) {
        this.toCountry = a.getCountry();
        this.toCity = a.getCity();
        this.toStreet = a.getStreet();
        this.toHouse = a.getHouse();
        this.toFlat = a.getFlat();
    }

    public AddressDto getToAddress() {
        return AddressDto.builder()
                .country(toCountry).city(toCity)
                .street(toStreet).house(toHouse).flat(toFlat)
                .build();
    }
}