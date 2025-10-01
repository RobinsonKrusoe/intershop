package ru.yandex.practicum.intershop.mapper;

import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.Order;

import java.util.List;

public class OrderMapper {
    public static OrderDTO toOrderDTO(Order order){
        if (order != null){
            OrderDTO ret = OrderDTO.builder()
                    .id(order.getId())
                    .stat(order.getStat())
                    .build();
            return ret;
        }
        return null;
    }
}
