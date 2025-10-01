package ru.yandex.practicum.intershop.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.mapper.ItemMapper;
import ru.yandex.practicum.intershop.mapper.OrderMapper;
import ru.yandex.practicum.intershop.mapper.WareMapper;
import ru.yandex.practicum.intershop.model.*;
import ru.yandex.practicum.intershop.repository.ItemRep;
import ru.yandex.practicum.intershop.repository.OrderRep;
import ru.yandex.practicum.intershop.repository.WareRep;
import ru.yandex.practicum.intershop.service.ShopService;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Сервис для работы с корзиной покупок
 */

@Slf4j
@Service
public class ShopServiceImpl implements ShopService {
    private final ItemRep itemRep;
    private final OrderRep orderRep;
    private final WareRep wareRep;

    public ShopServiceImpl(ItemRep itemRep, OrderRep orderRep, WareRep wareRep) {
        this.itemRep = itemRep;
        this.orderRep = orderRep;
        this.wareRep = wareRep;
    }

    /**
     * Изменение количества товара в корзине
     *
     * @param id     Идентификатор товара
     * @param action Действие
     * @return
     */
    @Override
    @Transactional
    public Mono<Void> changeItemAmount(Long id, ItemAction action) {
        return switch (action) {
            case DELETE -> delItem(id);
            case PLUS -> incItem(id);
            case MINUS -> decItem(id);
        };
    }

    private Mono<Void> delItem(Long id) {
        return geActiveOrder()
                .flatMap(order -> itemRep.deleteByOrderIdAndWareId(order.getId(), id));
    }

    private Mono<Void> decItem(Long id) {
        Mono<Order> order = geActiveOrder().cache();

        return order.flatMap(o -> itemRep.findByOrderIdAndWareId(o.getId(), id))
                    .flatMap(it -> {
                                        if (it.getCount() > 1) {
                                            it.setCount(it.getCount() - 1);
                                            return itemRep.save(it);
                                        } else {
                                            return itemRep.deleteByOrderIdAndWareId(it.getOrderId(), id);
                                        }
                                    }
                        )
                .then();
    }

    @Transactional
    private Mono<Void> incItem(Long id) {
        Mono<Order> order = geActiveOrder().cache();

        return order.flatMap(o -> itemRep.findByOrderIdAndWareId(o.getId(), id))
                    .switchIfEmpty(order.map(o -> new Item(o.getId(), id, 0)))
                    .flatMap(it -> {it.setCount(it.getCount() + 1);
                                    return itemRep.save(it);
                                   }
                            )
                    .then();
    }

    /**
     * Получение активной корзины
     *
     * @return Заказ
     */
    @Override
    public Mono<OrderDTO> getOrder() {
        return geActiveOrder().map(OrderMapper::toOrderDTO)
                              .flatMap(order -> getOrderItems(order.getId())
                                      .collectList()
                                      .map(items -> {
                                          order.setItems(items);
                                          order.setTotalSum((float)items.stream()
                                                                 .mapToDouble(i -> i.getCount() * i.getPrice())
                                                                 .sum());
                                          return order;
                                      })
                              );
    }

    /**
     * Получение/создание активной корзины
     *
     * @return Заказ
     */
    @Transactional
    private Mono<Order> geActiveOrder() {
        return orderRep.findActiveOrder()
                       .switchIfEmpty(Mono.defer(() -> {
                                               Order activeOrder = new Order();
                                               activeOrder.setStat(OrderStatus.NEW);
                                               return orderRep.save(activeOrder);
                                           })
                       );
    }

    /**
     * Получение элемента активной корзины
     *
     * @param id Идентификатор товара
     * @return Элемент корзины
     */
    @Override
    public Mono<ItemDTO> getItem(Long id) {
        return geActiveOrder()
                .flatMap(order -> itemRep.findByOrderIdAndWareId(order.getId(), id))
                .switchIfEmpty(Mono.just(new Item()))
                .zipWith(wareRep.findById(id)
                                .map(ItemMapper::toItemDTO)
                )
                .map(itemAndWare -> {
                    itemAndWare.getT2().setCount(itemAndWare.getT1().getCount());
                    return itemAndWare.getT2();
                });
    }

    /**
     * Совершение покупки по активной корзине
     */
    @Override
    @Transactional
    public Mono<Void> buy() {
        return geActiveOrder().flatMap(o -> {
                                                o.setStat(OrderStatus.BUY);
                                                return orderRep.save(o);
                                             }
                                       )
                              .then();
    }

    /**
     * Получение всех заказов (история заказов)
     *
     * @return Список заказов
     */
    @Override
    public Flux<OrderDTO> getAllOrders() {
        return orderRep.findAllByOrderByIdDesc()
                       .map(OrderMapper::toOrderDTO)
                       .flatMap(order -> getOrderItems(order.getId())
                                            .collectList()
                                            .map(items -> {
                                                order.setItems(items);
                                                order.setTotalSum((float)items.stream()
                                                     .mapToDouble(i -> i.getCount() * i.getPrice())
                                                     .sum()
                                                );
                                                return order;
                                            })
                       );
    }

    /**
     * Получение изображения товара
     *
     * @param id Идентификатор товара
     * @return Картинка
     */
    @Override
    public Mono<byte[]> getImage(Long id) {
        return wareRep.findById(id).map(Ware::getImage);
    }

    /**
     * Добавление товара в базу (наполнение справочника товаров)
     * @param ware Товар
     */
    @Override
    @Transactional
    public Mono<Void> addWare(InWareDTO ware) throws IOException {
        Ware newWare = WareMapper.toWare(ware);
        return wareRep.save(newWare).then();
    }

    /**
     * Получение товаров с фильтрацией и пагинацией
     *
     * @param search   Строка поиска
     * @param sortKind Тип сортировки
     * @param pageable Пагинация
     * @return Страница товаров/элементов корзины
     */
    @Override
    public Mono<Page<ItemDTO>> findAllItemsPaginated(String search, SortKind sortKind, Pageable pageable) {
        Flux<Ware> wares = null;
        Mono<Long> totalCount = null;

        //Подготовка нужной выборки
        if (search == null || search.isEmpty()) {
            totalCount = wareRep.countAllBy();

            wares = switch (sortKind) {
                case NO    -> wareRep.findAllBy(pageable);
                case ALPHA -> wareRep.findAllByOrderByTitle(pageable);
                case PRICE -> wareRep.findAllByOrderByPrice(pageable);
            };
        } else {
            totalCount = wareRep.countAllByTitleLikeIgnoreCase(search);

            wares = switch (sortKind) {
                case NO    -> wareRep.findAllByTitleLikeIgnoreCase(search, pageable);
                case ALPHA -> wareRep.findAllByTitleLikeIgnoreCaseOrderByTitle(search, pageable);
                case PRICE -> wareRep.findAllByTitleLikeIgnoreCaseOrderByPrice(search, pageable);
            };
        }

        return wares.map(ItemMapper::toItemDTO)
                    .collectList()
                    .zipWith(getOrder())
                    .map(itemsAndOrder -> {
                                //Актуализация количества для товаров, которые уже находятся в корзине
                                for (var orderItem : itemsAndOrder.getT2().getItems()) {    //Товары заказа
                                    for (var ware : itemsAndOrder.getT1()) {                //товары на странице
                                        if (ware.getId() == orderItem.getId())
                                            ware.setCount(orderItem.getCount());
                                    }
                                }
                        return itemsAndOrder.getT1();}
                    )
                    .zipWith(totalCount)
                    .map(ItemsAndCount -> new PageImpl<ItemDTO>(ItemsAndCount.getT1(),
                                                                pageable,
                                                                ItemsAndCount.getT2()));
    }

    /**
     * Получение заданного заказа
     * @param id Идентификатор заказа
     * @return
     */
    @Override
    public Mono<OrderDTO> getOrder(Long id) {
        return orderRep.findById(id)
                       .map(OrderMapper::toOrderDTO)
                       .flatMap(order -> getOrderItems(order.getId())
                       .collectList()
                       .map(items -> {
                           order.setItems(items);
                           order.setTotalSum((float)items.stream()
                                .mapToDouble(i -> i.getCount() * i.getPrice())
                                .sum());
                            return order;
                       })
               );
    }

    /**
     * Получение только элементов заказа
     * @param id    Идентификатор Заказа
     * @return      Список элементов
     */
    private Flux<ItemDTO> getOrderItems(Long id) {
        return itemRep.findAllByOrderIdOrderByIdDesc(id)
                      .flatMap(item -> wareRep.findById(item.getWareId())
                                              .map(ItemMapper::toItemDTO)
                                              .map(dto -> {
                                                  dto.setCount(item.getCount());
                                                  return dto;
                                              })
                      );
    }
}
