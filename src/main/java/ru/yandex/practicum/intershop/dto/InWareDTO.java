package ru.yandex.practicum.intershop.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class InWareDTO {
    private String title;
    private String description;
    private MultipartFile image;
    private float price;
}
