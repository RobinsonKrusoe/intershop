package ru.yandex.practicum.intershop.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;

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
     */
    @Override
    @Transactional
    public void changeItemAmount(Long id, ItemAction action) {
        Item item = itemRep.getByWare_Id(id);

        if (item == null && ItemAction.PLUS.equals(action)) {   //Если такого товара ещё нет в корзине - создать
            Ware ware = wareRep.getReferenceById(id);
            item = new Item();
            item.setWare(ware);
            item.setOrder(geActiveOrder());
            item.setCount(1);
            itemRep.save(item);
        } else {
            switch (action) {
                case DELETE -> itemRep.delete(item);
                case PLUS -> {
                    item.setCount(item.getCount() + 1);
                    itemRep.save(item);
                }
                case MINUS -> {
                    if (item.getCount() == 1) {
                        itemRep.delete(item);   //Если товар последний - просто удалить его из корзины
                    }

                    if (item.getCount() > 1) {
                        item.setCount(item.getCount() - 1);
                        itemRep.save(item);
                    }
                }
            }
        }
    }

    /**
     * Получение активной корзины
     *
     * @return Заказ
     */
    @Override
    public OrderDTO getOrder() {
        return OrderMapper.toOrderDTO(geActiveOrder());
    }

    /**
     * Получение/создание активной корзины
     *
     * @return Заказ
     */
    @Transactional
    private Order geActiveOrder() {
        Order activeOrder = orderRep.findActiveOrder();
        if (activeOrder == null) {    //Если отсутствует активная корзина - создать
            activeOrder = new Order();
            activeOrder.setStat(OrderStatus.NEW);
            activeOrder = orderRep.save(activeOrder);
        }

        return activeOrder;
    }

    /**
     * Получение элемента корзины
     *
     * @param id Идентификатор товара
     * @return Элемент корзины
     */
    @Override
    public ItemDTO getItem(Long id) {
        return ItemMapper.toItemDTO(itemRep.getByWare_Id(id));
    }

    /**
     * Совершение покупки по активной корзине
     */
    @Override
    @Transactional
    public void buy() {
        Order order = geActiveOrder();
        order.setStat(OrderStatus.BUY);
        orderRep.save(order);
    }

    /**
     * Получение всех заказов (история заказов)
     *
     * @return Список заказов
     */
    @Override
    public List<OrderDTO> getAllOrders() {
        return orderRep.findAll().stream().map(OrderMapper::toOrderDTO).toList();
    }

    /**
     * Получение изображения товара
     *
     * @param id Идентификатор товара
     * @return Картинка
     */
    @Override
    public byte[] getImage(Long id) {
        return wareRep.getReferenceById(id).getImage();
    }

    /**
     * Добавление товара в базу (наполнение справочника товаров)
     * @param ware Товар
     */
    @Override
    @Transactional
    public void addWare(InWareDTO ware) throws IOException {
        Ware newWare = WareMapper.toWare(ware);
        wareRep.save(newWare);
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
    public Page<ItemDTO> findAllItemsPaginated(String search, SortKind sortKind, Pageable pageable) {
        Page<Ware> itemPage = null;
        OrderDTO order = getOrder();

        //Подготовка нужной выборки
        if (search == null || search.isEmpty()) {
            switch (sortKind) {
                case NO -> itemPage = wareRep.findAll(pageable);
                case ALPHA -> itemPage = wareRep.findAllByOrderByTitle(pageable);
                case PRICE -> itemPage = wareRep.findAllByOrderByPrice(pageable);
            }
        } else {
            switch (sortKind) {
                case NO -> itemPage = wareRep.findAllByTitleLikeIgnoreCase(search, pageable);
                case ALPHA -> itemPage = wareRep.findAllByTitleLikeIgnoreCaseOrderByTitle(search, pageable);
                case PRICE -> itemPage = wareRep.findAllByTitleLikeIgnoreCaseOrderByPrice(search, pageable);
            }
        }

        Page<ItemDTO> dtoPage = itemPage.map(ItemMapper::toItemDTO);

        //Актуализация количества для товаров, которые уже находятся в корзине
        for (var orderItem : order.getItems()) {            //Товары заказа
            for (var listItem : dtoPage.getContent()) {     //товары на странице
                if (listItem.getId() == orderItem.getId())  //Если товар уже есть в заказе
                    listItem.setCount(orderItem.getCount());//заполнить количество
            }
        }

        return dtoPage;
    }

    /**
     * Получение заданного заказа
     * @param id Идентификатор заказа
     * @return
     */
    @Override
    public OrderDTO getOrder(Long id) {
        return OrderMapper.toOrderDTO(orderRep.getReferenceById(id));
    }
}
