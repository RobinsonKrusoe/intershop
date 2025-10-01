package ru.yandex.practicum.intershop.repository;

import reactor.core.publisher.Flux;
import ru.yandex.practicum.intershop.model.Order;
import ru.yandex.practicum.intershop.model.OrderStatus;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface OrderRep extends R2dbcRepository<Order, Long>{
    Mono<Order> findTopByStatIsOrderByIdDesc(OrderStatus stat);

    default  Mono<Order> findActiveOrder() {
        return findTopByStatIsOrderByIdDesc(OrderStatus.NEW);
    }

    Flux<Order> findAllByOrderByIdDesc();
}
