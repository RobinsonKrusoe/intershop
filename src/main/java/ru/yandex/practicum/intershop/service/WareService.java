package ru.yandex.practicum.intershop.service;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.Ware;

public interface WareService {
    Mono<Ware> findById(Long id);

    //Набор выборок товаров с поиском/без и сортировкой/без по названию или цене
    Flux<Ware> findAllBy(Pageable pageable);
    Flux<Ware> findAllByOrderByPrice(Pageable pageable);
    Flux<Ware> findAllByOrderByTitle(Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCase(String search, Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByPrice(String search, Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByTitle(String search, Pageable pageable);

    Mono<Long> countAllBy();
    Mono<Long> countAllByTitleLikeIgnoreCase(String search);

    Mono<Void> save(Ware ware);
}
