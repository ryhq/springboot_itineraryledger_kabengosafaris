package com.itineraryledger.kabengosafaris;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Disabled, honestly rather than skipped silently.
 *
 * @SpringBootTest starts the whole application, which needs a real MySQL — so on a CI runner it
 * cannot pass, and its presence was the reason the pipeline ran `-DskipTests` and therefore ran no
 * tests at all. Disabling this one lets every test that CAN run, run. The fix is a Testcontainers
 * MySQL, which is its own task in REPLICATION-PLAN.md.
 */
@SpringBootTest
@Disabled("needs a real MySQL; see REPLICATION-PLAN.md — Testcontainers boot test")
class KabengosafarisApplicationTests {

	@Test
	void contextLoads() {
	}

}
