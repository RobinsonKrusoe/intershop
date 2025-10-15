package ru.yandex.practicum.intershop.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.Ware;
import ru.yandex.practicum.intershop.repository.WareRep;
import ru.yandex.practicum.intershop.service.WareService;

/**
 * Сервис для работы со справочником товаров
 */

@Slf4j
@Service
public class WareServiceImpl implements WareService {
    private final WareRep wareRep;
    private final ReactiveRedisTemplate<String, Ware> redisTemplate;

    public WareServiceImpl(WareRep wareRep, ReactiveRedisTemplate<String, Ware> redisTemplate) {
        this.wareRep = wareRep;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Получение товара по его идентификатору (с кэшированием в Redis)
     *
     * @param id    Идентификатор товара
     * @return      Товар
     */
    @Override
    public Mono<Ware> findById(Long id) {
        return redisTemplate.opsForValue().get("ware:" + id)    //Попытка взять товар из кэша
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println("Not cashed. " + id);

                    return wareRep.findById(id).map(ware -> {   //Если в кэше нет - взять из базы
                        log.info("Sent to Redis ({}, {}});", "ware:" + ware.getId(), ware.getTitle());
                        redisTemplate.opsForValue().set("ware:" + ware.getId(), ware).subscribe();  //и попутно положить в кэш
                        return ware;
                        }
                    );
                }));
    }

    /**
     * Получение всех товаров с пагинацией
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllBy(Pageable pageable) {
        return wareRep.findAllIdBy(pageable).flatMap(this::findById);
    }

    /**
     * Получение всех товаров отсортированных по цене с пагинацией
     *
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllByOrderByPrice(Pageable pageable) {
        return wareRep.findAllIdByOrderByPrice(pageable).flatMap(this::findById);
    }

    /**
     * Получение всех товаров отсортированных по названию с пагинацией
     *
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllByOrderByTitle(Pageable pageable) {
        return wareRep.findAllIdByOrderByTitle(pageable).flatMap(this::findById);
     }

    /**
     * Получение всех найденных товаров с пагинацией
     *
     * @param search    Строка поиска
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllByTitleLikeIgnoreCase(String search, Pageable pageable) {
        return wareRep.findAllIdByTitleLikeIgnoreCase(search, pageable).flatMap(this::findById);
    }

    /**
     * Получение всех найденных товаров отсортированных по цене с пагинацией
     *
     * @param search    Строка поиска
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByPrice(String search, Pageable pageable) {
        return wareRep.findAllIdByTitleLikeIgnoreCaseOrderByPrice(search, pageable).flatMap(this::findById);
    }

    /**
     * Получение всех найденных товаров отсортированных по названию с пагинацией
     *
     * @param search    Строка поиска
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> findAllByTitleLikeIgnoreCaseOrderByTitle(String search, Pageable pageable) {
        return wareRep.findAllIdByTitleLikeIgnoreCaseOrderByTitle(search, pageable).flatMap(this::findById);
    }

    /**
     * Получение общего количества товаров (для обеспечения пагинации)
     * @return          Количество найденных товаров
     */
    @Override
    public Mono<Long> countAllBy() {
        return wareRep.countAllBy();
    }

    /**
     * Получение количества найденных товаров (для обеспечения пагинации)
     * @param search    Строка поиска
     * @return          Количество найденных товаров
     */
    @Override
    public Mono<Long> countAllByTitleLikeIgnoreCase(String search) {
        return wareRep.countAllByTitleLikeIgnoreCase(search);
    }

    /**
     * Добавление нового товара в базу данных
     *
     * @param ware  Товар
     * @return      Подписка на событие
     */
    @Override
    public Mono<Void> save(Ware ware) {
        return wareRep.save(ware).then();
    }
}
