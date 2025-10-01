package ru.yandex.practicum.intershop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Класс товара
*/

@Table(name = "wares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ware {
    @Id
    private long id;                    //Идентификатор товара

    private String title;
    private String description;
    private byte[] image;
    private float price;
    private LocalDateTime createdAt;
}
