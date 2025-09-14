package ru.yandex.practicum.intershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.intershop.model.Ware;

import java.util.List;

public interface WareRep extends JpaRepository<Ware, Long> {
    //Набор выборок товаров с поиском/без и сортировкой/без по названию или цене
    List<Ware> findAllByOrderByPrice();
    List<Ware> findAllByOrderByTitle();
    List<Ware> findAllByTitleLikeIgnoreCase(String search);
    List<Ware> findAllByTitleLikeIgnoreCaseOrderByPrice(String search);
    List<Ware> findAllByTitleLikeIgnoreCaseOrderByTitle(String search);
}
