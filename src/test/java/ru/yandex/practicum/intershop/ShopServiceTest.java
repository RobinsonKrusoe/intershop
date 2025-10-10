package ru.yandex.practicum.intershop;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import reactor.core.publisher.Mono;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import ru.yandex.practicum.intershop.model.OrderStatus;
import ru.yandex.practicum.intershop.model.SortKind;
import ru.yandex.practicum.intershop.model.Ware;
import ru.yandex.practicum.intershop.repository.ItemRep;
import ru.yandex.practicum.intershop.repository.OrderRep;
import ru.yandex.practicum.intershop.repository.WareRep;
import ru.yandex.practicum.intershop.service.ShopService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class ShopServiceTest {
	@Autowired
	private ShopService shopServ;

	@Autowired
	private WareRep wareRep;

	@Autowired
	private ItemRep itemRep;

	@Autowired
	private OrderRep orderRep;

	private final InWareDTO ware1 = InWareDTO.builder()
			.title("Test ware title")
			.description("Test ware description")
			.image("ware".getBytes())
			.price(1F)
			.build();

	@AfterEach
	void afterEach(){
		//Очистка базы
		itemRep.deleteAll().block();
		orderRep.deleteAll().block();
	}

	/**
	 * Тест возвращения активного заказа
	 */
	@Test
	void testGetActiveOrder() {
		//Добавить тестовые товары в активную корзину
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Вернуть данные из базы и проверить их соответствие отправленным
		shopServ.getOrder().flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(OrderStatus.NEW, order.getStat());					//Заказ должен быть в статусе Новый
			assertEquals(3F, order.getTotalSum());					//Корректная общая стоимость всех товаров = 3
			assertEquals(2, order.getItems().size());				//Предполагается, что в корзине 2 товара
			assertEquals(1, order.getItems().get(0).getCount());	//У товара корректно заполнено количество
			assertEquals(1, order.getItems().get(1).getCount());	//У товара корректно заполнено количество
			assertTrue(List.of("Test ware title 1", "Test ware title 2")				//Вернулись оба имени товара
					.containsAll(List.of(order.getItems().get(0).getTitle(),
										 order.getItems().get(1).getTitle())));
			assertTrue(List.of("Test ware description 1", "Test ware description 2")	//Вернулись оба описания товара
					.containsAll(List.of(order.getItems().get(0).getDescription(),
										 order.getItems().get(1).getDescription())));
			return Mono.empty();
		}).block();
	}

	/**
	 * Тест первичного добавления товаров в корзину
	 */
	@Test
	void testCreateOrderByWareIncrement() {
		//Добавление пары товаров в активную корзину увеличением их количества
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Вернуть данные из базы и проверить их соответствие отправленным
		shopServ.getOrder().flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(1, order.getItems().get(0).getCount());	//На данные момент должно быть по 1 товару
			assertEquals(1, order.getItems().get(1).getCount());
			return Mono.empty();
		}).block();
	}

	/**
	 * Тест смены количества товара в корзине
	 */
	@Test
	void testChangeItemAmount() {
		//Добавление пары товаров в активную корзину увеличением их количества
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Увеличение количества первого товара
		shopServ.changeItemAmount(1000L, ItemAction.PLUS).block();

		//Уменьшение количества товара до 0 (эквивалентно его удалению из заказа)
		shopServ.changeItemAmount(2000L, ItemAction.MINUS).block();

		//Вернуть данные из базы и проверить их соответствие предполагаемым
		shopServ.getOrder().flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(1, order.getItems().size());				//Должен остаться только один товар
			assertEquals(2, order.getItems().get(0).getCount());	//Количество оставшегося товара должно быть равно 2
			assertEquals(2F, order.getTotalSum());					//Корректная сумма заказа = 2
			return Mono.empty();
		}).block();
	}

	/**
	 * Тест удаления товаров из корзины
	 */
	@Test
	void testDeleteItem() {
		//Добавление пары товаров в активную корзину увеличением их количества
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Проверить, что товары добавились
		shopServ.getOrder().flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(1, order.getItems().get(0).getCount());	//На данные момент должно быть по 1 товару
			assertEquals(1, order.getItems().get(1).getCount());
			return Mono.empty();
		}).block();

		//Удаление одного из товаров
		shopServ.changeItemAmount(2000L, ItemAction.DELETE).block();

		//Проверить, что товар удалился
		shopServ.getOrder().flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(1, order.getItems().size());				//Должен остаться только один товар
			assertEquals(1F, order.getTotalSum());					//Корректная сумма заказа = 1
			return Mono.empty();
		}).block();
	}

	/**
	 * Тест завершения заказа/покупки
	 */
	@Test
	void testBuyOrder() {
		//Добавление пары товаров в активную корзину увеличением их количества
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Произвести покупку
		shopServ.buy().block();

		//Вернуть данные из базы и проверить их соответствие
		shopServ.getOrder(1L).flatMap(order -> {
			assertNotNull(order);											//Заказ не должен быть пустым
			assertNotNull(order.getItems());								//В нём должны быть элементы
			assertEquals(OrderStatus.BUY, order.getStat());					//Заказ должен быть в статусе Buy
			assertEquals(3F, order.getTotalSum());					//Корректная общая стоимость всех товаров = 3
			assertEquals(2, order.getItems().size());				//Предполагается, что в корзине 2 товара
			assertEquals(1, order.getItems().get(0).getCount());	//У товара корректно заполнено количество
			assertEquals(1, order.getItems().get(1).getCount());	//У товара корректно заполнено количество
			assertTrue(List.of("Test ware title 1", "Test ware title 2")				//Вернулись оба имени товара
					.containsAll(List.of(order.getItems().get(0).getTitle(),
							order.getItems().get(1).getTitle())));
			assertTrue(List.of("Test ware description 1", "Test ware description 2")	//Вернулись оба описания товара
					.containsAll(List.of(order.getItems().get(0).getDescription(),
							order.getItems().get(1).getDescription())));
			return Mono.empty();
		}).block();
	}

	/**
	 * Тест добавления товара в базу
	 */
	@Test
	void testAddNewWare() {
		//Добавление тестового товара в базу
		shopServ.addWare(ware1).block();

		//Возврат товара из базы
		Ware ware = wareRep.findAll()
				.filter(w -> ware1.getTitle().equals(w.getTitle()))
				.blockFirst();

		assertNotNull(ware);	//Товар должен быть в базе
		//Атрибуты товара должны соответствовать отправленным в базу
		assertEquals(ware1.getTitle(), ware.getTitle());
		assertEquals(ware1.getDescription(), ware.getDescription());
		assertEquals(ware1.getPrice(), ware.getPrice());
		assertEquals(ware1.getImage().length, ware.getImage().length);

		//Подчистить товар для устранения конфликтов в других тестах
		wareRep.deleteById(ware.getId()).block();
	}

	/**
	 * Тест получения всех товаров
	 */
	@Test
	void testFindAllItemsPaginated(){
		shopServ.findAllItemsPaginated(null,
									   SortKind.NO,
									   PageRequest.of(0, 5))
				.flatMap(page -> {
					assertEquals(1, page.getTotalPages());		//Должна быть 1 страница
					assertEquals(2, page.getContent().size());	//На странице должно быть 2 товара

					return Mono.empty();
				}).block();

	}

	/**
	 * Тест возврата элемента корзины
	 */
	@Test
	void testGetItem(){
		shopServ.getItem(1000L)
				.flatMap(item -> {
					assertNotNull(item);		//Должен вернуться не пустой объект
					assertEquals("Test ware title 1", item.getTitle());
					assertEquals("Test ware description 1", item.getDescription());
					assertEquals(0, item.getCount());
					assertEquals(1, item.getPrice());

					return Mono.empty();
				})
				.block();
	}

	/**
	 * Тест получения списка всех заказов
	 */
	@Test
	void getAllOrders(){
		//Формирование первого заказа
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		//Произвести покупку (закрыть первый заказ)
		shopServ.buy().block();

		//Формирование второго заказа
		wareRep.findAll().flatMap(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS)).blockLast();

		shopServ.getAllOrders()
				.collectList()
				.flatMap(orders -> {
					assertEquals(2, orders.size());	//Ожидается два заказа
					for (var order: orders) {
						assertEquals(2, order.getItems().size());	//Ожидается по 2 товара в заказе
						assertEquals(3, order.getTotalSum());		//Корректная общая сумма = 3
					}

					assertTrue(List.of(OrderStatus.NEW, OrderStatus.BUY)	//Один заказ завершённый, второй - новый
							.containsAll(List.of(orders.get(0).getStat(),
												 orders.get(1).getStat())));

					return Mono.empty();
				}).block();
	}
}
