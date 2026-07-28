package com.gigafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class GigafixBackendApplicationTests {

	@Autowired
	private Environment environment;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoadsWithIsolatedH2TestProfile() throws Exception {
		assertTrue(List.of(environment.getActiveProfiles()).contains("test"));
		assertEquals(
				"H2",
				dataSource.getConnection().getMetaData()
						.getDatabaseProductName()
		);
	}

}
