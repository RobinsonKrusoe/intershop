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
            if (order.getItems() != null) {
                ret.setItems(order.getItems().stream().map(ItemMapper::toItemDTO).toList());
                ret.setTotalSum((float)order.getItems().stream().mapToDouble(i -> i.getCount() * i.getWare().getPrice()).sum());
            } else {
                ret.setItems(List.of());
                ret.setTotalSum(0);
            }

            return ret;
        }
        return null;
    }
}
