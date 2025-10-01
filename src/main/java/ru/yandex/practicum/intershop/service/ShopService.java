package ru.yandex.practicum.intershop.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.intershop.model.SortKind;

import java.io.IOException;

/**
 * Класс сервиса для работы с магазином
 */
public interface ShopService {
    /**
     * Изменение количества товара в корзине
     * @param id        Идентификатор элемента корзины
     * @param action    Действие
     */
    Mono<Void> changeItemAmount(Long id, ItemAction action);

    /**
     * Получение активной корзины
     * @return Заказ
     */
    Mono<OrderDTO> getOrder();

    /**
     * Получение элемента корзны
     * @param id Идентификатор элемента корзины
     * @return  Элемент корзины
     */
    Mono<ItemDTO> getItem(Long id);

    /**
     * Совершение покупки по активной корзине
     */
    Mono<Void> buy();

    /**
     * Получение всех заказов
     * @return  Список заказов
     */
    Flux<OrderDTO> getAllOrders();

    /**
     * Получение картинки товара
     *
     * @param id  Идентификатор товара
     * @return  Картинка
     */
    Mono<byte[]> getImage(Long id);

    /**
     * Добавление товара в базу
     * @param ware Товар
     */
    Mono<Void> addWare(InWareDTO ware) throws IOException;

    /**
     * Получение товаров с фильтрацией и пагинацией
     *
     * @param search    Строка поиска
     * @param sortKind  Тип сортировки
     * @param pageable  Пагинация
     * @return Страница товаров/элементов корзины
     */
    Mono<Page<ItemDTO>> findAllItemsPaginated(String search, SortKind sortKind, Pageable pageable);

    /**
     * Получение заданного заказа
     * @param id Идентификатор заказа
     * @return Заказ
     */
    Mono<OrderDTO> getOrder(Long id);
}
