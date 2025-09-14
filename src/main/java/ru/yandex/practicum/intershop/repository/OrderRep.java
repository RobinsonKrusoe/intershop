package ru.yandex.practicum.intershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.intershop.model.Order;

public interface OrderRep extends JpaRepository<Order, Long>{
    /**
     * Выборка активной корзины
     * @return Заказ
     */
    @Query(value = """
                   select o.* from orders o
                    where o.stat = 'NEW'
                    order by created_at desc
                    limit 1
                   """,
            nativeQuery = true)
    Order findActiveOrder();
}
