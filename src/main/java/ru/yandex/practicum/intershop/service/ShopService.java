package ru.yandex.practicum.intershop.service;

import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.intershop.model.SortKind;

import java.io.IOException;
import java.util.List;

public interface ShopService {
    /**
     * Изменение количества товара в корзине
     * @param id        Идентификатор элемента корзины
     * @param action    Действие
     */
    void changeItemAmount(Long id, ItemAction action);

    /**
     * Получение активной корзины
     * @return Заказ
     */
    OrderDTO getOrder();

    /**
     * Получение элемента корзны
     * @param id Идентификатор элемента корзины
     * @return  Элемент корзины
     */
    ItemDTO getItem(Long id);

    /**
     * Совершение покупки по активной корзине
     */
    void buy();

    /**
     * Получение всех заказов
     * @return  Список заказов
     */
    List<OrderDTO> getAllOrders();

    /**
     * Получение картинки товара
     *
     * @param id  Идентификатор товара
     * @return  Картинка
     */
    byte[] getImage(Long id);

    /**
     * Добавление товара в базу
     * @param ware Товар
     */
    void addWare(InWareDTO ware) throws IOException;

    /**
     * Получение товаров с фильтрацией и пагинацией
     *
     * @param search    Строка поиска
     * @param sortKind  Тип сортировки
     * @param pageable  Пагинация
     * @return Страница товаров/элементов корзины
     */
    Page<ItemDTO> findAllItemsPaginated(String search, SortKind sortKind, Pageable pageable);

    /**
     * Получение заданного заказа
     * @param id Идентификатор заказа
     * @return Заказ
     */
    OrderDTO getOrder(Long id);
}
