package ru.yandex.practicum.intershop.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.model.SortKind;
import ru.yandex.practicum.intershop.model.Ware;
import ru.yandex.practicum.intershop.repository.WareRep;
import ru.yandex.practicum.intershop.service.WareService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для работы со справочником товаров
 */

@Slf4j
@Service
public class WareServiceImpl implements WareService {
    private static final String WARE_CACHE = "WARE";
    private static final String SEARCH_CACHE = "SEARCH";
    private static final String COUNT_CACHE = "COUNT";

    private final WareRep wareRep;
    private final ReactiveRedisTemplate<String, Ware> redisWareTempl;
    private final ReactiveRedisTemplate<String, String> redisStringTempl;

    public WareServiceImpl(WareRep wareRep,
                           ReactiveRedisTemplate<String, Ware> redisWareTempl,
                           ReactiveRedisTemplate<String, String> redisStringTempl) {
        this.wareRep = wareRep;
        this.redisWareTempl = redisWareTempl;
        this.redisStringTempl = redisStringTempl;
    }

    /**
     * Получение товара по его идентификатору (с кэшированием в Redis)
     *
     * @param id    Идентификатор товара
     * @return      Товар
     */
    @Override
    public Mono<Ware> findById(Long id) {
        return redisWareTempl.opsForValue().get(WARE_CACHE + ":" + id)    //Попытка взять товар из кэша
                .switchIfEmpty(Mono.defer(() -> {
                    System.out.println(WARE_CACHE + ":" + id + " was not in cache.");

                    return wareRep.findById(id).map(ware -> {   //Если в кэше нет - взять из базы
                        redisWareTempl.opsForValue()
                                      .set(WARE_CACHE + ":" + ware.getId(), ware)
                                      .subscribe(count -> log.info("Sent to Redis ({}, {});",
                                              WARE_CACHE + ":" + ware.getId(), ware.getTitle()));
                        return ware;
                        }
                    );
                }));
    }

    /**
     * Поиск товаров с сортировкой и пагинацией (с кэшированием)
     *
     * @param search    Строка поиска
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    @Override
    public Flux<Ware> searchWares(String search, SortKind sortKind, Pageable pageable) {
        //Определение ключа кэширования результата поиска
        String searchKey = SEARCH_CACHE + ":" +
                sortKind.name() + "_SORT:" +
                ((search == null || search.isEmpty()) ?
                        "NO_SEARCH" : URLEncoder.encode(search.toUpperCase(), StandardCharsets.UTF_8)) + ":" +
                pageable.getPageNumber() + ":" +
                pageable.getPageSize();

        System.out.println(searchKey);

        return redisStringTempl.opsForList()
                .range(searchKey, 0, -1)                      //Поискать страницу в кэше
                .collectList()
                .flatMapMany(ids -> {
                    if (ids == null || ids.isEmpty()) {                 //Если в кэше нет
                        System.out.println(searchKey + " was not in cache.");
                        return findAllIds(search, sortKind, pageable)   //Взять из базы
                                .map(id -> {                            //Результат дополнительно положить в кэш
                                        redisStringTempl.opsForList()
                                                    .rightPush(searchKey, String.valueOf(id))
                                                    .subscribe(count -> log.info("Sent to Redis {}:{}", searchKey, id));
                                            return id;
                                        }
                                )
                                .flatMap(this::findById);
                    } else {
                        return Flux.fromIterable(ids)
                                   .map(Long::valueOf)
                                   .flatMap(this::findById);
                    }
                });
    }

    /**
     * Поиск товаров с сортировкой и пагинацией (из базы)
     *
     * @param search    Строка поиска
     * @param pageable  Атрибуты страницы
     * @return          Товары страницы
     */
    private Flux<Long> findAllIds(String search, SortKind sortKind, Pageable pageable) {
        Flux<Long> ids = null;
        if (search == null || search.isEmpty()) {
            ids = switch (sortKind) {
                case NO    -> wareRep.findAllIdBy(pageable);
                case ALPHA -> wareRep.findAllIdByOrderByTitle(pageable);
                case PRICE -> wareRep.findAllIdByOrderByPrice(pageable);
            };
        } else {
            ids = switch (sortKind) {
                case NO    -> wareRep.findAllIdByTitleLikeIgnoreCase(search, pageable);
                case ALPHA -> wareRep.findAllIdByTitleLikeIgnoreCaseOrderByTitle(search, pageable);
                case PRICE -> wareRep.findAllIdByTitleLikeIgnoreCaseOrderByPrice(search, pageable);
            };
        }
        return ids;
    }

    /**
     * Получение общего количества товаров (для обеспечения пагинации)
     * @param search    Строка поиска
     * @return          Количество найденных товаров
     */
    @Override
    public Mono<Long> wareCount(String search) {
        String cacheKey = (search == null || search.isEmpty()) ?
                COUNT_CACHE + ":FOR_ALL_WARES" :
                COUNT_CACHE + ":FOR_SEARCH:" + URLEncoder.encode(search.toUpperCase(), StandardCharsets.UTF_8);

        return redisStringTempl.opsForValue().get(cacheKey)     //Попытка взять счётчик из кэша
                .map(Long::valueOf)
                .switchIfEmpty(Mono.defer(() -> {               //Если в кэше нет - взять из базы
                    System.out.println(cacheKey + " was not in cache.");
                    Mono<Long> count = (search == null || search.isEmpty()) ?
                            wareRep.countAllBy() :
                            wareRep.countAllByTitleLikeIgnoreCase(search);

                    return count.map(c -> {                     //и попутно положить в кэш
                                redisStringTempl.opsForValue()
                                        .set(cacheKey, String.valueOf(c))
                                        .subscribe(sub -> log.info("Sent to Redis {}:{}", cacheKey, c));
                                return c;
                            }
                    );
                }));
    }

    /**
     * Добавление нового товара в базу данных
     *
     * @param ware  Товар
     * @return      Подписка на событие
     */
    @Override
    public Mono<Void> save(Ware ware) {
        flushCache();
        return wareRep.save(ware).then();
    }

    /**
     * Сброс кэша поиска
     * @return
     */
    private Mono<Void> flushCache() {
        redisStringTempl.keys(COUNT_CACHE + ":*")
                .flatMap(redisStringTempl::delete)
                .subscribe(count -> log.info("Removed from COUNT_CACHE cache {} key.", count));

        redisStringTempl.keys(SEARCH_CACHE + ":*")
                .flatMap(redisStringTempl::delete)
                .subscribe(count -> log.info("Removed from SEARCH_CACHE cache {} key.", count));

        return Mono.empty();
    }
}
