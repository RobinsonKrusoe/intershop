package ru.yandex.practicum.intershop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.Ware;

@Repository
public interface WareRep extends R2dbcRepository<Ware, Long> {
    Mono<Long> countAllBy();
    Mono<Long> countAllByTitleLikeIgnoreCase(String search);

    //Набор выборок товаров с поиском/без и сортировкой/без по названию или цене
    Flux<Long> findAllIdBy(Pageable pageable);
    Flux<Long> findAllIdByOrderByPrice(Pageable pageable);
    Flux<Long> findAllIdByOrderByTitle(Pageable pageable);
    Flux<Long> findAllIdByTitleLikeIgnoreCase(String search, Pageable pageable);
    Flux<Long> findAllIdByTitleLikeIgnoreCaseOrderByPrice(String search, Pageable pageable);
    Flux<Long> findAllIdByTitleLikeIgnoreCaseOrderByTitle(String search, Pageable pageable);
}
