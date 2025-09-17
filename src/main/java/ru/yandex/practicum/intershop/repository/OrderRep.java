package ru.yandex.practicum.intershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.intershop.model.Order;
import ru.yandex.practicum.intershop.model.OrderStatus;

public interface OrderRep extends JpaRepository<Order, Long>{
    Order findTopByStatIsOrderByIdDesc(OrderStatus stat);

    default Order findActiveOrder() {
        return findTopByStatIsOrderByIdDesc(OrderStatus.NEW);
    }
}
