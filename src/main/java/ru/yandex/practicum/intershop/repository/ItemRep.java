package ru.yandex.practicum.intershop.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.Item;

@Repository
public interface ItemRep extends R2dbcRepository<Item, Long> {
    Mono<Item> findByOrderIdAndWareId(Long orderId, Long wareId);

    /**
     * Удаление элемента корзины
     * @param orderId   Идентификатор заказа
     * @param wareId    Идентификатор товара
     * @return
     */
    Mono<Void> deleteByOrderIdAndWareId(Long orderId, Long wareId);

    /**
     * Получение всех элементов заказа
     * @param orderId   Идентификатор заказа
     * @return
     */
    Flux<Item> findAllByOrderIdOrderByIdDesc(Long orderId);
}
