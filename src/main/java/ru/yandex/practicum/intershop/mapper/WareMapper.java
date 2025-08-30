package ru.yandex.practicum.intershop.mapper;

import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.model.Ware;

import java.io.IOException;
import java.time.LocalDateTime;

public class WareMapper {
    public static Ware toWare(InWareDTO ware) throws IOException {
        if (ware != null) {
            Ware ret = new Ware();
            ret.setTitle(ware.getTitle());
            ret.setDescription(ware.getDescription());
            ret.setPrice(ware.getPrice());
            ret.setImage(ware.getImage().getBytes());

            return ret;
        }
        return null;
    }
}
