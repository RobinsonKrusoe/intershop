package ru.yandex.practicum.intershop.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Класс товара
*/

@Entity
@Table(name = "wares")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;                    //Идентификатор товара

    @NotBlank
    private String title;
    private String description;
    private byte[] image;
    private float price;
    @CreationTimestamp
    private LocalDateTime createdAt;

}
