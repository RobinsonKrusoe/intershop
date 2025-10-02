package ru.yandex.practicum.intershop.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InWareDTO {
    private String title;
    private String description;
    private byte[] image;
    private float price;
}
