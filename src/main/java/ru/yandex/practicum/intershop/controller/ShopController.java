package ru.yandex.practicum.intershop.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import ru.yandex.practicum.intershop.model.SortKind;
import ru.yandex.practicum.intershop.model.Paging;
import ru.yandex.practicum.intershop.service.ShopService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.LocalDateTime.now;

@Slf4j
@RestController
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
    @GetMapping("/")
    public ModelAndView items() {
        return new ModelAndView("redirect:/main/items");
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
    @GetMapping("/main/items")
    public ModelAndView getMainPage(@RequestParam(name = "search", required = false) String search,
                                    @RequestParam(name = "sort", required = false, defaultValue = "NO") String sort,
                                    @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                    @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber) {
        log.info("Get getMainPage.");

        Page<ItemDTO> page = serv.findAllItemsPaginated(search,
                                                        SortKind.valueOf(sort),
                                                        PageRequest.of(pageNumber - 1, pageSize));

//        List<List<Employee>> divided = employees.stream().collect(
//                Collectors.teeing(
//                        Collectors.filtering(Employee::isActive, Collectors.toList()),
//                        Collectors.filtering(Predicate.not(Employee::isActive), Collectors.toList()),
//                        List::of
//                ));

//        List<List<Integer>> result = IntStream.range(0, list.size() / 100)
//                .mapToObj(index -> list.subList(index * 100, index * 100 + 100))
//                .collect(Collectors.toList());
//
//        final int n = 3;
//        List<Integer> arr = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 0);
//        int rows = 1 + arr.size() / n;
//        List<List<Integer>> sublists = IntStream.range(0, rows)
//                .mapToObj(i -> arr.subList(n * i, Math.min(n + (n * i), arr.size())))
//                .collect(Collectors.toList());
//        System.out.println(sublists);

        List<List<ItemDTO>> sublists = IntStream.range(0, (1 + page.getContent().size())/2)
                .mapToObj(i -> page.getContent().subList(2*i, Math.min(2 + (2*i), page.getContent().size())))
                .collect(Collectors.toList());

        ModelAndView mv = new ModelAndView("main");
        mv.addObject ("items", sublists);
        mv.addObject ("search", search);
        mv.addObject ("sort", sort);
        mv.addObject ("paging", new Paging(page.getPageable().getPageNumber() + 1,
                page.getPageable().getPageSize(),
                page.hasNext(),
                page.hasPrevious()));
        return mv;
    }

    /**
     * в) POST "/main/items/{id}" - изменить количество товара в корзине
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/main/items"
     */
    @PostMapping(path = "/main/items/{id}")
    public ModelAndView changeItemAmount(@PathVariable("id") Long id,
                                         @RequestParam("action") String action){
        log.info("Get changeItemAmount id - {}, action - {}", id, action);

        serv.changeItemAmount(id, ItemAction.valueOf(action));

        ModelAndView mv = new ModelAndView("redirect:/main/items");

        return mv;
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
    @GetMapping("/cart/items")
    public ModelAndView getCart() {
        log.info("Get getCart.");

        OrderDTO order = serv.getOrder();

        ModelAndView mv = new ModelAndView("cart");
        mv.addObject ("items", order.getItems());
        mv.addObject ("total", order.getTotalSum());
        mv.addObject ("empty", order.getItems().isEmpty());

        return mv;
    }
    /**
     * д) POST "/cart/items/{id}" - изменить количество товара в корзине
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/cart/items"
     */
    @PostMapping(path = "/cart/items/{id}")
    public ModelAndView changeItemAmount1(@PathVariable("id") Long id,
                                         @RequestParam("action") String action){
        log.info("Get changeItemAmount1 id - {}, action - {}", id, action);

        serv.changeItemAmount(id, ItemAction.valueOf(action));

        ModelAndView mv = new ModelAndView("redirect:/cart/items");

        return mv;
    }

    /**
     * е) GET "/items/{id}" - карточка товара
     * 	Возвращает:
     * 		шаблон "item.html"
     * 		используется модель для заполнения шаблона:
     * 			"item" - товаров (id, title, decription, imgPath, count, price)
     */
    @GetMapping(path = "/items/{id}")
    public ModelAndView getItem(@PathVariable("id") Long id){
        log.info("Get getItem id{}", id);

        ItemDTO item = serv.getItem(id);

        ModelAndView mv = new ModelAndView("item");
        mv.addObject ("item", item);

        return mv;
    }

    /**
     * ж) POST "/items/{id}" - изменить количество товара в корзине
     * 	Параматры:
     * 		action - значение из перечисления PLUS|MINUS|DELETE (PLUS - добавить один товар, MINUS - удалить один товар, DELETE - удалить товар из корзины)
     * 	Возвращает:
     * 		редирект на "/items/{id}"
     */
    @PostMapping(path = "/items/{id}")
    public ModelAndView changeItemAmount2(@PathVariable("id") Long id,
                                          @RequestParam("action") String action){
        log.info("Get changeItemAmount2 id - {}, action - {}", id, action);

        serv.changeItemAmount(id, ItemAction.valueOf(action));

        ModelAndView mv = new ModelAndView("redirect:/items/" + id);
        mv.addObject("action", action);
        return mv;
    }

    /**
     *з) POST "/buy" - купить товары в корзине (выполняет покупку товаров в корзине и очищает ее)
     *	Возвращает:
     *		редирект на "/orders/{id}?newOrder=true"
     */
    @PostMapping(path = "/buy")
    public ModelAndView buy(){
        log.info("Get buy ");

        Long orderId = serv.getOrder().getId();

        serv.buy();

        ModelAndView mv = new ModelAndView("redirect:/orders/" + orderId + "?newOrder=true");
        return mv;
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
    @GetMapping(path = "/orders")
    public ModelAndView getOrders(){
        log.info("Get getOrders");

        List<OrderDTO> orders = serv.getAllOrders();

        ModelAndView mv = new ModelAndView("orders");
        mv.addObject("orders", orders);

        return mv;
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
    @GetMapping(path = "/orders/{id}")
    public ModelAndView getOrder(@PathVariable("id") Long id,
                                 @RequestParam(name ="newOrder", required = false, defaultValue = "false") String newOrder){
        log.info("Get getOrder id{}", id);

        OrderDTO order = serv.getOrder(id);

        ModelAndView mv = new ModelAndView("order");
        mv.addObject("order", order);
        mv.addObject("newOrder", newOrder);
        return mv;
    }

    /**
     * е) GET "/images/{id}" -эндпоинт, возвращающий набор байт картинки поста
     * 	Параметры:
     * @param id - идентификатор товара
     * @return Массив байт картинки
     */
    @GetMapping("/images/{id}")
    public void getImage(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
        log.info("Get getImage id={}", id);

        byte[] imgBytes = serv.getImage(id);
        response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
        response.getOutputStream().write(imgBytes);
        response.getOutputStream().close();
    }

    @GetMapping("/add/ware")
    public ModelAndView getAddWare(){
        log.info("Get getAddWare");

        return new ModelAndView("add-ware");
    }

    @PostMapping(path = "/add/ware", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE })
    public ModelAndView addWare(@RequestParam("title") String title,
                                @RequestParam("description") String description,
                                @RequestParam("price") Float price,
                                @RequestParam("image") MultipartFile image) throws IOException {
        log.info("Post addWare title={}, description={}, price={}", title, description, price);

        serv.addWare(InWareDTO.builder()
                        .title(title)
                        .description(description)
                        .price(price)
                        .image(image)
                        .build());

        return new ModelAndView("redirect:/add/ware");
    }
}
