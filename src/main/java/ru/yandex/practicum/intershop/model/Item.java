package ru.yandex.practicum.intershop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Класс элемента корзины
*/

@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @Id
    private long id;                    //Идентификатор элемента корзины
    private long orderId;
    private long wareId;
    private int count;
    private LocalDateTime createdAt;

    public Item (long orderId, long wareId, int count) {
        this.orderId = orderId;
        this.wareId = wareId;
        this.count = count;
    }
}
