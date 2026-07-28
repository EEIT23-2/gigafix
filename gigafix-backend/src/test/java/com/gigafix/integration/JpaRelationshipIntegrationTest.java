package com.gigafix.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.context.ActiveProfiles;

import com.gigafix.cart.entity.Cart;
import com.gigafix.cart.entity.CartItem;
import com.gigafix.order.entity.Order;
import com.gigafix.order.entity.OrderItem;
import com.gigafix.user.entity.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.gigafix.cart.repository.CartRepository;

@DataJpaTest
@ActiveProfiles("test")
class JpaRelationshipIntegrationTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void cartMemberCannotBeNull() {
		Cart cart = Cart.builder()
				.status(Cart.CartStatus.ACTIVE)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(cart);
			entityManager.flush();
		});
	}

	@Test
	void orderMemberCannotBeNull() {
		Order order = Order.builder()
				.receiverName("測試")
				.receiverPhone("0900000000")
				.shippingAddress("測試地址")
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(order);
			entityManager.flush();
		});
	}

	@Test
	void cartItemCartCannotBeNull() {
		CartItem item = CartItem.builder()
				.productId(10L)
				.quantity(1)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(item);
			entityManager.flush();
		});
	}

	@Test
	void orderItemOrderCannotBeNull() {
		OrderItem item = OrderItem.builder()
				.productId(10L)
				.quantity(1)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(item);
			entityManager.flush();
		});
	}

	@Test
	void databaseRejectsUnknownMemberForeignKey() {
		Member missingMember = entityManager.getReference(Member.class, 999L);
		Cart cart = Cart.builder()
				.member(missingMember)
				.status(Cart.CartStatus.ACTIVE)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(cart);
			entityManager.flush();
		});
	}

	@Test
	void databaseRejectsUnknownCartForeignKey() {
		Cart missingCart = entityManager.getReference(Cart.class, 999L);
		CartItem item = CartItem.builder()
				.cart(missingCart)
				.productId(10L)
				.quantity(1)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(item);
			entityManager.flush();
		});
	}

	@Test
	void databaseRejectsUnknownOrderForeignKey() {
		Order missingOrder = entityManager.getReference(Order.class, 999L);
		OrderItem item = OrderItem.builder()
				.order(missingOrder)
				.productId(10L)
				.productName("商品")
				.unitPrice(BigDecimal.TEN)
				.quantity(1)
				.subtotal(BigDecimal.TEN)
				.build();

		assertThrows(RuntimeException.class, () -> {
			entityManager.persist(item);
			entityManager.flush();
		});
	}

	@Test
	void checkoutRepositoryUsesPessimisticWriteLock() throws Exception {
		Method method = CartRepository.class.getMethod(
				"findForCheckoutByMemberIdAndStatus",
				Long.class,
				Cart.CartStatus.class
		);

		Lock lock = method.getAnnotation(Lock.class);

		assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
	}

	@Test
	void schemaUsesMemberIdAndForeignKeys() {
		Integer cartMemberColumn = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE TABLE_NAME = 'CARTS'
				  AND COLUMN_NAME = 'MEMBER_ID'
				""", Integer.class);
		Integer orderMemberColumn = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.COLUMNS
				WHERE TABLE_NAME = 'ORDERS'
				  AND COLUMN_NAME = 'MEMBER_ID'
				""", Integer.class);
		Integer memberForeignKeys = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
				WHERE TABLE_NAME IN ('CARTS', 'ORDERS')
				  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
				""", Integer.class);

		assertEquals(1, cartMemberColumn);
		assertEquals(1, orderMemberColumn);
		assertEquals(2, memberForeignKeys);
	}

	@SuppressWarnings("unused")
	private Member persistMember(String email) {
		Member member = Member.builder()
				.password("password")
				.realName("測試會員")
				.nickName("測試")
				.email(email)
				.phone("0900000000")
				.address("測試地址")
				.gender(Member.Gender.MALE)
				.createDateTime(LocalDateTime.now())
				.build();
		entityManager.persist(member);
		return member;
	}
}
