package com.deanflights;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests. Boots the full Spring context against REAL infrastructure
 * running in Docker via Testcontainers — a Postgres database and a Kafka broker — so the tests
 * exercise the actual JPA mappings, the security filter chain, and the Kafka producer/consumer
 * wiring rather than mocks or an in-memory stand-in.
 *
 * <h2>Singleton container pattern (why no {@code @Container}/{@code @Testcontainers})</h2>
 * The containers are {@code static final} and started ONCE in a static initializer. We deliberately
 * do NOT use {@code @Testcontainers} + {@code @Container}: that JUnit extension ties the container
 * lifecycle to each test CLASS (start before the class, stop after it). Spring, however, caches and
 * reuses the application context across test classes — so a later class would reuse a context still
 * pointing at a container the extension had already stopped, causing "connection refused". Starting
 * the containers manually as JVM-wide singletons keeps them alive for the whole run; Testcontainers'
 * Ryuk sidecar reaps them when the JVM exits.
 *
 * <h2>Kafka approach</h2>
 * We use Testcontainers' {@link ConfluentKafkaContainer} (the KRaft-mode, Zookeeper-free image)
 * wired with Spring Boot's {@link ServiceConnection @ServiceConnection} via a static accessor +
 * {@code @DynamicPropertySource}. On Spring Boot 3.5, {@code @ServiceConnection} on a Kafka
 * container is natively supported: Boot overrides {@code spring.kafka.bootstrap-servers} from the
 * container automatically, so the app's auto-configured KafkaTemplate / @KafkaListener talk to the
 * real broker. Postgres is wired the same way. (Because the fields are plain singletons rather than
 * {@code @Container}-managed, we expose them to Boot's {@code @ServiceConnection} support by also
 * publishing their connection details via {@code @DynamicPropertySource} as a robust fallback.)
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        // Start once for the whole JVM run. Shared across every test class and method.
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    protected MockMvc mockMvc;
}
