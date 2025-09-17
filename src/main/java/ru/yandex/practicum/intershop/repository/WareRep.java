package ru.yandex.practicum.intershop.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.intershop.model.Ware;

public interface WareRep extends JpaRepository<Ware, Long> {
    //Набор выборок товаров с поиском/без и сортировкой/без по названию или цене
    Page<Ware> findAllByOrderByPrice(Pageable pageable);
    Page<Ware> findAllByOrderByTitle(Pageable pageable);
    Page<Ware> findAllByTitleLikeIgnoreCase(String search, Pageable pageable);
    Page<Ware> findAllByTitleLikeIgnoreCaseOrderByPrice(String search, Pageable pageable);
    Page<Ware> findAllByTitleLikeIgnoreCaseOrderByTitle(String search, Pageable pageable);
}
