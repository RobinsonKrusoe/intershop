package ru.yandex.practicum.intershop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Класс заказа
 */
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private long id;                    //Идентификатор заказа
    private OrderStatus stat;
    private LocalDateTime createdAt;
}
