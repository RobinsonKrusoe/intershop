package ru.yandex.practicum.intershop.service;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.SortKind;
import ru.yandex.practicum.intershop.model.Ware;

public interface WareService {
    Mono<Ware> findById(Long id);

    Mono<Long> wareCount(String search);

    Mono<Void> save(Ware ware);

    Flux<Ware> searchWares(String search, SortKind sortKind, Pageable pageable);
}
