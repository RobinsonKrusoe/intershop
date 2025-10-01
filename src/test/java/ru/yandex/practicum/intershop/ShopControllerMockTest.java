package ru.yandex.practicum.intershop;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.OrderStatus;
import ru.yandex.practicum.intershop.service.ShopService;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import org.springframework.http.client.MultipartBodyBuilder;

import java.util.List;

@WebFluxTest
class ShopControllerMockTest {

    @Autowired
    private WebTestClient webTestClient;

	@MockBean
	private ShopService shopServ;

	private final ItemDTO item1 = ItemDTO.builder()
			.title("Test ware title 1")
			.description("Test ware description 1")
			.imageId(1)
			.price(1F)
			.build();

	private final ItemDTO item2 = ItemDTO.builder()
			.title("Test ware title 2")
			.description("Test ware description 2")
			.imageId(2)
			.price(2F)
			.build();

	/**
	 * Тест вызова корневой страницы
	 */
	@Test
	void testGetRootPath() throws Exception {
        webTestClient.get()
                .uri("/")
                .exchange()
                .expectStatus().is3xxRedirection()  //.isFound()
                .expectHeader().valueEquals("Location", "/main/items");
	}

	/**
	 * Тест вызова страницы добавления товара
	 */
	@Test
	void testGetAddWarePage() throws Exception {
        webTestClient.get()
                .uri("/add/ware")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class).consumeWith(response -> {
                    String body = response.getResponseBody();
                    assertNotNull(body);
                    assertTrue(body.contains("<h3>Название</h3>"));
                    assertTrue(body.contains("<h3>Цена</h3>")); // Проверяем, что HTML содержит заданные элементы
                });
	}

	/**
	 * Тест вызова главной страницы
	 */
	@Test
	//@Sql("/test-truncate.sql")
	void testGetMainPage() throws Exception {
		Mockito.when(shopServ.findAllItemsPaginated(any(), any(), any()))
			   .thenReturn(Mono.just(new PageImpl<ItemDTO>(List.of(item1, item2),
					   									   PageRequest.of(0, 10),
					   								  2)));

		//Получение страницы с данными из базы данных
		webTestClient.get()
				.uri("/main/items")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(response -> {
					String body = response.getResponseBody();
					assertNotNull(body);
					assertTrue(body.contains("<title>Витрина товаров</title>"));
					assertTrue(body.contains(item1.getTitle())); // Проверяем, что HTML содержит название товара
					assertTrue(body.contains(item2.getDescription())); // Проверяем, что HTML содержит название товара
				});
	}

	/**
	 * Тест вызова страницы изменения количества товара в корзине
	 */
	@Test
	void testChangeItemAmount() throws Exception {
		Mockito.when(shopServ.changeItemAmount(any(), any()))
				.thenReturn(Mono.empty().then());

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("action", "PLUS", MediaType.TEXT_PLAIN)
			       .header("Content-Disposition", "form-data; name=action")
			       .header("Content-type", "text/plain");

		webTestClient.post()
				.uri("/main/items/1")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(bodyBuilder.build()))
				.exchange()
				.expectStatus().is3xxRedirection()
				.expectHeader().valueEquals("Location", "/main/items");
	}

	/**
	 * Тест вызова страницы корзины
	 */
	@Test
	void testGetCart() throws Exception {
		OrderDTO order = OrderDTO.builder()
				.id(1)
				.totalSum(100)
				.stat(OrderStatus.NEW)
				.items(List.of(item1, item2)).build();

		Mockito.when(shopServ.getOrder())
				.thenReturn(Mono.just(order));

		webTestClient.get()
				.uri("/cart/items")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(response -> {
					String body = response.getResponseBody();
					assertNotNull(body);
					assertTrue(body.contains("<title>Корзина товаров</title>"));
					assertTrue(body.contains(item1.getTitle())); // Проверяем, что HTML содержит название товара
					assertTrue(body.contains(item2.getDescription())); // Проверяем, что HTML содержит название товара
				});
	}

	/**
	 * Тест  вызова получения элемента корзины
	 */
	@Test
	void testGetItem() throws Exception {
		Mockito.when(shopServ.getItem(any()))
				.thenReturn(Mono.just(item1));

		webTestClient.get()
				.uri("/items/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(response -> {
					String body = response.getResponseBody();
					assertNotNull(body);
					assertTrue(body.contains("<title>Витрина товаров</title>"));
					assertTrue(body.contains(item1.getTitle())); // Проверяем, что HTML содержит название товара
					assertTrue(body.contains(item1.getDescription())); // Проверяем, что HTML содержит описание товара
				});
	}

	/**
	 * Тест совершения покупки
	 */
	@Test
	void testBuy() throws Exception {
		OrderDTO order = OrderDTO.builder()
				.id(1)
				.totalSum(100)
				.stat(OrderStatus.NEW)
				.items(List.of(item1, item2)).build();

		Mockito.when(shopServ.getOrder())
				.thenReturn(Mono.just(order));

		Mockito.when(shopServ.buy())
				.thenReturn(Mono.empty().then());

		webTestClient.post()
				.uri("/buy")
				.exchange()
				.expectStatus().is3xxRedirection()
				.expectHeader().valueEquals("Location", "/orders/1?newOrder=true");
	}

	/**
	 * Тест получения списка заказов
	 */
	@Test
	void testGetOrders() throws Exception {
		OrderDTO order1 = OrderDTO.builder()
				.id(1)
				.totalSum(100)
				.stat(OrderStatus.NEW)
				.items(List.of(item1, item2)).build();

		OrderDTO order2 = OrderDTO.builder()
				.id(2)
				.totalSum(100)
				.stat(OrderStatus.NEW)
				.items(List.of(item1, item2)).build();

		Mockito.when(shopServ.getAllOrders())
				.thenReturn(Flux.just(order1, order2));

		webTestClient.get()
				.uri("/orders")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(response -> {
					String body = response.getResponseBody();
					assertNotNull(body);
					assertTrue(body.contains("<title>Заказы</title>"));
					assertTrue(body.contains("Заказ №1")); // Проверяем, что HTML содержит название заказа
					assertTrue(body.contains("Заказ №2")); // Проверяем, что HTML содержит название заказа
					assertTrue(body.contains(item1.getTitle())); // Проверяем, что HTML содержит название товара
					assertTrue(body.contains(item2.getTitle())); // Проверяем, что HTML содержит название товара
				});
	}

	/**
	 * Тест получения отдельного заказа
	 */
	@Test
	void testGetOrder() throws Exception {
		OrderDTO order = OrderDTO.builder()
				.id(1)
				.totalSum(100)
				.stat(OrderStatus.NEW)
				.items(List.of(item1, item2)).build();

		Mockito.when(shopServ.getOrder(any()))
				.thenReturn(Mono.just(order));

		webTestClient.get()
				.uri("/orders/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.TEXT_HTML)
				.expectBody(String.class).consumeWith(response -> {
					String body = response.getResponseBody();
					assertNotNull(body);
					assertTrue(body.contains("<title>Заказ</title>"));
					assertTrue(body.contains("Заказ №1")); // Проверяем, что HTML содержит название заказа
					assertTrue(body.contains(item1.getTitle())); // Проверяем, что HTML содержит название товара
					assertTrue(body.contains(item2.getTitle())); // Проверяем, что HTML содержит название товара
				});
	}
}
