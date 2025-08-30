package ru.yandex.practicum.intershop.dto;

import ru.yandex.practicum.intershop.model.OrderStatus;

import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
public class OrderDTO {
    private long id;                    //Идентификатор заказа
    private OrderStatus stat;
    private List<ItemDTO> items;
    private float totalSum;
}
