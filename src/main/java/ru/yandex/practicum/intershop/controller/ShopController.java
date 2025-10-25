package ru.yandex.practicum.intershop.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import ru.yandex.practicum.intershop.model.SortKind;
import ru.yandex.practicum.intershop.model.Paging;
import ru.yandex.practicum.intershop.service.ShopService;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Контроллер для работы с корзиной покупок в интернет магазине
 */
@Tag(name = "ShopController", description = "Контроллер для работы с корзиной покупок в интернет магазине")
@Slf4j
@Controller
@RequestMapping("/")
public class ShopController {
    private final ShopService serv;

    public ShopController(ShopService serv) {
        this.serv = serv;
    }

    /**
     * а) GET "/" - редирект на "/main/items"
     * @return - Шаблон items
     */
    @Operation(summary = "Вывод главной формы")
    @GetMapping("/")
    public Mono<Rendering> items() {
        return Mono.just(Rendering.redirectTo("/main/items").build());
    }

    /**
     * б) GET "/main/items" - список всех товаров плиткой на главной странице
     * 		Параметры:
     * 			search - строка с поисков по названию/описанию товара (по умолчанию, пустая строка - все товары)
     *     		sort - сортировка перечисление NO, ALPHA, PRICE (по умолчанию, NO - не использовать сортировку)
     *     		pageSize - максимальное число товаров на странице (по умолчанию, 10)
     *     		pageNumber - номер текущей страницы (по умолчанию, 1)
     *     	Возвращает:
     *     		шаблон "main.html"
     *     		используется модель для заполнения шаблона:
     *     			"items" - List<List<Item>> - список товаров по N в ряд (id, title, decription, imgPath, count, price)
     *     			"search" - строка поиска (по умолчанию, пустая строка - все товары)
     *     			"sort" - сортировка перечисление NO, ALPHA, PRICE (по умолчанию, NO - не использовать сортировку)
     *     			"paging":
     *     				"pageNumber" - номер текущей страницы (по умолчанию, 1)
     *     				"pageSize" - максимальное число товаров на странице (по умолчанию, 10)
     *     				"hasNext" - можно ли пролистнуть вперед
     *     				"hasPrevious" - можно ли пролистнуть назад
     */
    @Operation(summary = "Вывод списка товаров на главной странице")
    @GetMapping("/main/items")
    public Mono<Rendering> getMainPage(@Parameter(description = "Строка поиска", required = false)
                                       @RequestParam(name = "search", required = false) String search,
                                       @Parameter(description = "Вид сортировки", required = false)
                                       @RequestParam(name = "sort", required = false, defaultValue = "NO") String sort,
                                       @Parameter(description = "Размер страницы", required = false)
                                       @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                       @Parameter(description = "Номер страницы", required = false)
                                       @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber) {
        log.info("Get getMainPage.");

        Mono<Page<ItemDTO>> page = serv.findAllItemsPaginated(search,
                                                        SortKind.valueOf(sort),
                                                        PageRequest.of(pageNumber - 1, pageSize));

        return page.map(p -> Rendering
                        .view("main")
                        .modelAttribute("items", IntStream.range(0, (1 + p.getContent().size()) / 2)
                                                                .mapToObj(i -> p.getContent()
                                                                .subList(2 * i, Math.min(2 + (2 * i), p.getContent().size())))
                                                                .collect(Collectors.toList()))
                        .modelAttribute("search", search)
                        .modelAttribute("sort", sort)
                        .modelAttribute("paging", new Paging(p.getPageable().getPageNumber() + 1,
                                p.getPageable().getPageSize(),
                                p.hasNext(),
                                p.hasPrevious()))
                        .build())
                .flatMap(Mono::just);
    }

    /**
     * в) POST "/main/items/{id}" - добавить товар (изменить количество) на главной странице
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/main/items"
     */
    @Operation(summary = "Изменение количества товара для главной страницы")
    @PostMapping(path = "/main/items/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE })
    public Mono<Rendering> mainPageAmountChange(@Parameter(description = "ID товара", required = true)
                                                @PathVariable("id") Long id,
                                                @Parameter(description = "Действие над количеством товара в корзине", required = true)
                                                @RequestPart("action") String action){

        log.info("Post mainPageAmountChange id - {}, action - {}", id, action);

        return serv.changeItemAmount(id, ItemAction.valueOf(action))
                   .thenReturn(Rendering.redirectTo("/main/items").build());
    }

    /**
     * г) GET "/cart/items" - список товаров в корзине
     * 	Возвращает:
     * 		шаблон "cart.html"
     * 		используется модель для заполнения шаблона:
     * 			"items" - List<Item> - список товаров в корзине (id, title, decription, imgPath, count, price)
     * 			"total" - суммарная стоимость заказа
     * 			"empty" - true, если в корзину не добавлен ни один товар
     */
    @Operation(summary = "Вывод товаров корзины")
    @GetMapping("/cart/items")
    public Mono<Rendering> getCart() {
        log.info("Get getCart.");


        return serv.getOrder()
                        .map(o -> Rendering
                                .view("cart")
                                .modelAttribute("items", o.getItems())
                                .modelAttribute("total", o.getTotalSum())
                                .modelAttribute("empty", o.getItems().isEmpty())
                                .build())
                        .flatMap(Mono::just);
    }

    /**
     * д) POST "/cart/items/{id}" - изменить количество товара на странице корзины
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/cart/items"
     */
    @Operation(summary = "Изменение количества товара для страницы корзины")
    @PostMapping(path = "/cart/items/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE })
    public Mono<Rendering> cartPageAmountChange(@Parameter(description = "ID товара", required = true)
                                                @PathVariable("id") Long id,
                                                @Parameter(description = "Действие над количеством товара в корзине", required = true)
                                                @RequestPart("action") String action){
        log.info("Post cartPageAmountChange id - {}, action - {}", id, action);

        return serv.changeItemAmount(id, ItemAction.valueOf(action))
                   .thenReturn(Rendering.redirectTo("/cart/items").build());
    }

    /**
     * е) GET "/items/{id}" - карточка товара
     * 	Возвращает:
     * 		шаблон "item.html"
     * 		используется модель для заполнения шаблона:
     * 			"item" - товаров (id, title, decription, imgPath, count, price)
     */
    @Operation(summary = "Вывод карточки товара")
    @GetMapping("/items/{id}")
    public Mono<Rendering> getItem(@Parameter(description = "ID товара", required = true)
                                   @PathVariable(name = "id") Long id){
        log.info("Get getItem id - {}", id);

        return serv.getItem(id)
                   .map(i -> Rendering.view("item")
                                      .modelAttribute("item", i)
                                      .build()
                       );
    }

    /**
     * ж) POST "/items/{id}" - изменить количество товара на странице карточки товара
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/items/{id}"
     */
    @Operation(summary = "Изменение количества товара для карточки товара")
    @PostMapping(path = "/items/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE })
    public Mono<Rendering> itemPageAmountChange(@Parameter(description = "ID товара", required = true)
                                                @PathVariable("id") Long id,
                                                @Parameter(description = "Действие над количеством товара в корзине", required = true)
                                                @RequestPart("action") String action){
        log.info("Post itemPageAmountChange id - {}, action - {}", id, action);

        return serv.changeItemAmount(id, ItemAction.valueOf(action))
                   .thenReturn(Rendering.redirectTo("/items/" + id).build());
    }

    /**
     *з) POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *	Возвращает:
     *		редирект на "/orders/{id}?newOrder=true"
     */
    @Operation(summary = "Совершение покупки")
    @PostMapping("/buy")
    public Mono<Rendering> buy(){
        log.info("Get buy ");

        return serv.getOrder()
                   .flatMap(o -> serv.buy()
                                     .thenReturn(Rendering.redirectTo("/orders/" + o.getId() + "?newOrder=true")
                                                          .build()
                                      )
                    );
    }

    /**
     * и) GET "/orders" - список заказов
     *	Возвращает:
     * 		шаблон "orders.html"
     * 		используется модель для заполнения шаблона:
     * 			"orders" - List<Order> - список заказов
     * 				"id" - идентификатор заказа
     *      	 "items" - List<Item> - список товаров в заказе (id, title, decription, imgPath, count, price)
     */
    @Operation(summary = "Получение списка заказов")
    @GetMapping("/orders")
    public Mono<Rendering> getOrders(){
        log.info("Get getOrders");

        return serv.getAllOrders()
                   .collectList()
                   .map(lo -> Rendering.view("orders")
                                       .modelAttribute("orders", lo)
                                       .build()
                   );
    }

    /**
     * к) GET "/orders/{id}" - карточка заказа
     * 	Параматры:
     * 		newOrder - true, если переход со страницы оформления заказа (по умолчанию, false)
     * 	Возвращает:
     * 		шаблон "order.html"
     * 		используется модель для заполнения шаблона:
     * 			"order" - заказ Order
     * 			"id" - идентификатор заказа
     * 			"items" - List<Item> - список товаров в заказе (id, title, decription, imgPath, count, price)
     * 			"newOrder" - true, если переход со страницы оформления заказа (по умолчанию, false)
     */
    @Operation(summary = "Получение заказа")
    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrder(@Parameter(description = "ID заказа", required = true)
                                    @PathVariable(name = "id") Long id,
                                    @Parameter(description = "Признак нового заказа", required = false)
                                    @RequestParam(name ="newOrder", required = false, defaultValue = "false") String newOrder){
        log.info("Get getOrder id {}", id);

        return serv.getOrder(id)
                   .map(o -> Rendering.view("order")
                                      .modelAttribute("order", o)
                                      .modelAttribute("newOrder", newOrder)
                                      .build()
                );
    }

    /**
     * е) GET "/images/{id}" -эндпоинт, возвращающий набор байт картинки поста
     * 	Параметры:
     * @param id - идентификатор товара
     * @return Массив байт картинки
     */
    @Operation(summary = "Получение изображения товара в виде набора байт.")
    @GetMapping("/images/{id}")
    public @ResponseBody Mono<byte[]> getImage(@Parameter(description = "ID товара", required = true)
                                               @PathVariable(name = "id") Long id) throws IOException {
        log.info("Get getImage id={}", id);

        Mono<byte[]> imgBytes = serv.getImage(id);
        return imgBytes;
    }

    @Operation(summary = "Возвращает форму добавления товара")
    @GetMapping("/add/ware")
    public Mono<Rendering> getAddWare(){
        log.info("Get getAddWare");

        return Mono.just(Rendering.view("add-ware").build());
    }

    @Operation(summary = "Добавление нового товара в базу")
    @PostMapping(path = "/add/ware", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE })
    public Mono<Rendering> addWare(@Parameter(description = "Название товара", required = true)
                                   @RequestPart("title") String title,
                                   @Parameter(description = "Описание товара", required = true)
                                   @RequestPart("description") String description,
                                   @Parameter(description = "Цена товара", required = true)
                                   @RequestPart("price") String price,
                                   @Parameter(description = "Файл изображения товара", required = true)
                                   @RequestPart("image") FilePart image) throws IOException {
        log.info("Post addWare title={}, description={}, price={}, image={}", title, description, price, image);

        return DataBufferUtils.join(image.content())
                .map(dataBuffer -> dataBuffer.asByteBuffer().array())
                .flatMap(img -> serv.addWare(InWareDTO.builder()
                                                        .title(title)
                                                        .description(description)
                                                        .price(Float.parseFloat(price))
                                                        .image(img)
                                                        .build()))
                .thenReturn(Rendering.redirectTo("/add/ware").build());
    }
}
