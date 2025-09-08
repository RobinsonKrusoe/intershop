package ru.yandex.practicum.intershop;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.yandex.practicum.intershop.dto.InWareDTO;
import ru.yandex.practicum.intershop.dto.ItemDTO;
import ru.yandex.practicum.intershop.dto.OrderDTO;
import ru.yandex.practicum.intershop.model.ItemAction;
import ru.yandex.practicum.intershop.model.OrderStatus;
import ru.yandex.practicum.intershop.model.Ware;
import ru.yandex.practicum.intershop.repository.WareRep;
import ru.yandex.practicum.intershop.service.ShopService;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ShopControllerIntegrationMockTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ShopService shopServ;

	@Autowired
	private WareRep wareRep;

	private final InWareDTO ware1 = InWareDTO.builder()
			.title("Test ware title 1")
			.description("Test ware description 1")
			.image(new MockMultipartFile("image", "ware1".getBytes()))
			.price(1F)
			.build();

	private final InWareDTO ware2 = InWareDTO.builder()
			.title("Test ware title 2")
			.description("Test ware description 2")
			.image(new MockMultipartFile("image", "ware2".getBytes()))
			.price(2F)
			.build();

	/**
	 * Тест вызова корневой страницы
	 */
	@Test
	void testGetRootPath() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/main/items"));
	}

	/**
	 * Тест вызова страницы добавления товара
	 */
	@Test
	void testGetAddWarePage() throws Exception {
		mockMvc.perform(get("/add/ware"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("add-ware"));
	}

	/**
	 * Тест вызова главной страницы
	 */
	@Test
	void testGetMainPage() throws Exception {
		//Создать товары
		shopServ.addWare(ware1);
		shopServ.addWare(ware2);

		//Добавить товары в корзину
  		wareRep.findAll().forEach(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS));

	  	//Получение страницы с данными из базы данных
		mockMvc.perform(get("/main/items"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("main"))
				.andExpect(xpath("//table/tr/td").nodeCount(13))
				.andExpect(xpath("//table/tr[2]/td/table/tr[2]/td/b").string(ware1.getTitle()))
				.andExpect(xpath("//table/tr[2]/td[2]/table/tr[3]/td").string(ware2.getDescription()));
	}

	/**
	 * Тест вызова страницы изменения количества товара в корзине
	 */
	@Test
	void testChangeItemAmount() throws Exception {
		//Создать товар
		shopServ.addWare(ware1);

		//Вызов добавления товара в корзину
		mockMvc.perform(MockMvcRequestBuilders.post("/main/items/1")
						.param("action","PLUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/main/items"));

		//Проверка количества товара в корзине
		ItemDTO item = shopServ.getItem(1L);
		assertEquals(item.getCount(),1);

		//Вызов уменьшения товара в корзине
		mockMvc.perform(MockMvcRequestBuilders.post("/main/items/1")
						.param("action","MINUS"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/main/items"));

		//Проверка, что товар из корзины удалился
		assertNull(shopServ.getItem(1L));
	}

	/**
	 * Тест вызова страницы корзины
	 */
	@Test
	void testGetCart() throws Exception {
		//Создать товары
		shopServ.addWare(ware1);
		shopServ.addWare(ware2);

		//Добавить товары в корзину
		wareRep.findAll().forEach(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS));

		//Получение страницы с данными из базы данных
		mockMvc.perform(get("/cart/items"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("cart"))
				.andExpect(xpath("//table/tr/td").nodeCount(14))
				.andExpect(xpath("//table/tr[1]/td/table/tr[2]/td/b").string(ware1.getTitle()))
				.andExpect(xpath("//table/tr[2]/td/table/tr[3]/td").string(ware2.getDescription()));
	}

	/**
	 * Тест  вызова получения элемента корзины
	 */
	@Test
	void testGetItem() throws Exception {
		//Создать товар
		shopServ.addWare(ware1);

		//Добавить товар в корзину
		shopServ.changeItemAmount(1L, ItemAction.PLUS);

		//Получение страницы с данными из базы
		mockMvc.perform(get("/items/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("item"))
				.andExpect(xpath("//div/p[2]/b").string(ware1.getTitle()));

	}

	/**
	 * Тест совершения покупки
	 */
	@Test
	@Transactional
	void testBuy() throws Exception {
		//Создать товар
		shopServ.addWare(ware1);

		//Добавить товар в корзину
		shopServ.changeItemAmount(1L, ItemAction.PLUS);

		//Вызов добавления товара в корзину
		mockMvc.perform(post("/buy"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders/1?newOrder=true"));

		List<OrderDTO> allOrders = shopServ.getAllOrders();

		//Предполагается, что создался только один заказ и он выполнен
		assertEquals(allOrders.size(),1);
		assertEquals(allOrders.get(0).getStat(), OrderStatus.BUY);
	}

	/**
	 * Тест получения списка заказов
	 */
	@Test
	void testGetOrders() throws Exception {
		//Создать товары
		shopServ.addWare(ware1);
		shopServ.addWare(ware2);

		//Добавить товар в корзину
		shopServ.changeItemAmount(1L, ItemAction.PLUS);

		//Закрыть заказ
		shopServ.buy();

		//Добавить товар в новую корзину
		shopServ.changeItemAmount(2L, ItemAction.PLUS);

		//Получение страницы с данными из базы
		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("orders"))
				.andExpect(xpath("//table/tr/td").nodeCount(4))
				.andExpect(xpath("//table/tr[1]/td/table/tr/td")
						.string(containsString(ware1.getTitle())))
				.andExpect(xpath("//table/tr[2]/td/table/tr/td")
						.string(containsString(ware2.getTitle())));
	}

	/**
	 * Тест получения отдельного заказа
	 */
	@Test
	void testGetOrder() throws Exception {
		//Создать товары
		shopServ.addWare(ware1);
		shopServ.addWare(ware2);

		//Добавить товары в корзину
		wareRep.findAll().forEach(w -> shopServ.changeItemAmount(w.getId(), ItemAction.PLUS));

		//Получение страницы с данными из базы
		mockMvc.perform(get("/orders/1"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("text/html;charset=UTF-8"))
				.andExpect(view().name("order"))
				.andExpect(xpath("//table/tr/td").nodeCount(12))
				.andExpect(xpath("//table/tr[2]/td/table/tr[2]/td/b").string(ware1.getTitle()))
				.andExpect(xpath("//table/tr[3]/td/table/tr[2]/td/b").string(ware2.getTitle()));
	}
}
