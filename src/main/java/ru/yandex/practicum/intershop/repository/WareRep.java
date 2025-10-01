package ru.yandex.practicum.intershop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.Ware;

@Repository
public interface WareRep extends R2dbcRepository<Ware, Long> {
    //Набор выборок товаров с поиском/без и сортировкой/без по названию или цене
    Flux<Ware> findAllBy(Pageable pageable);
    Flux<Ware> findAllByOrderByPrice(Pageable pageable);
    Flux<Ware> findAllByOrderByTitle(Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCase(String search, Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByPrice(String search, Pageable pageable);
    Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByTitle(String search, Pageable pageable);

    Mono<Long> countAllBy();
    Mono<Long> countAllByTitleLikeIgnoreCase(String search);
}
