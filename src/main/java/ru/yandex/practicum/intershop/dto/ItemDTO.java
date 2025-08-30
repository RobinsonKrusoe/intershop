package ru.yandex.practicum.intershop.dto;

import lombok.*;

@Getter
@Setter
@Builder
public class ItemDTO {
    private long id;                    //Идентификатор товара
    private String title;
    private String description;
    private long imageId;
    private float price;
    private int count;
}
