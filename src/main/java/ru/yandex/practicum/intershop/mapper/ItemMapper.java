package ru.yandex.practicum.intershop.mapper;

import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.model.Item;
import ru.yandex.practicum.intershop.model.Ware;

public class ItemMapper {
    public static ItemDTO toItemDTO(Item item){
        if (item != null){
            return ItemDTO.builder()
                    .id(item.getWare().getId())
                    .title(item.getWare().getTitle())
                    .description(item.getWare().getDescription())
                    .imageId(item.getWare().getId())
                    .price(item.getWare().getPrice())
                    .count(item.getCount())
                    .build();
        }
        return null;
    }

    public static ItemDTO toItemDTO(Ware ware){
        if (ware != null){
            return ItemDTO.builder()
                    .id(ware.getId())
                    .title(ware.getTitle())
                    .description(ware.getDescription())
                    .imageId(ware.getId())
                    .price(ware.getPrice())
                    .count(0)
                    .build();
        }
        return null;
    }
}
