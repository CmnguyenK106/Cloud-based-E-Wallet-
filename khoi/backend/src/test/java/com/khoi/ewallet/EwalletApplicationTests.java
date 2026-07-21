package com.khoi.ewallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=local", "jwt.secret=test-only-secret!that-is-at-least-32-bytes-long"})
class EwalletApplicationTests {

	@Test
	void contextLoads() {
	}

}
