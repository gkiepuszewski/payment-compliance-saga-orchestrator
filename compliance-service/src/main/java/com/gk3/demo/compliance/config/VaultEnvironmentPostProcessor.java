package com.gk3.demo.compliance.config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Fetches this service's DB password from a local Vault dev server at startup and injects it as
 * {@code spring.datasource.password}, so the password never has to be hard-coded in
 * {@code application.yml}, docker-compose, or Terraform (see README &gt; "Secrets management").
 *
 * <p>This runs before the Spring {@code ApplicationContext} exists, so a plain JDK
 * {@link HttpClient} call is used instead of a Spring-managed bean/{@code RestClient}.
 *
 * <p>If Vault is unreachable (e.g. during {@code mvn test}, where Testcontainers'
 * {@code @ServiceConnection} overrides the datasource entirely and never needs this), this logs a
 * warning and leaves the property unset rather than failing application startup.
 */
public class VaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(VaultEnvironmentPostProcessor.class);

    private static final String SECRET_PATH = "compliance-service";
    private static final String DEFAULT_VAULT_ADDR = "http://localhost:8200";
    // NOSONAR: fixed root token for the ephemeral, in-memory `vault -dev` server started by
    // docker-compose for local development only; wiped on every restart, never reachable outside
    // localhost. See README > "Secrets management" for the full rationale.
    private static final String DEFAULT_VAULT_TOKEN = "dev-only-root-token";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        String vaultAddr = environment.getProperty("VAULT_ADDR", DEFAULT_VAULT_ADDR);
        String vaultToken = environment.getProperty("VAULT_TOKEN", DEFAULT_VAULT_TOKEN);

        try {
            String password = fetchPassword(vaultAddr, vaultToken);
            if (password == null) {
                return;
            }
            environment.getPropertySources().addFirst(
                    new MapPropertySource("vaultSecrets", Map.of("spring.datasource.password", password)));
            log.info("Fetched DB password for secret/{} from Vault at {}.", SECRET_PATH, vaultAddr);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Could not fetch DB password from Vault at {} ({}); spring.datasource.password will be unset. "
                    + "This is expected when running tests, but not when running the service for real - make sure "
                    + "`docker compose up -d vault vault-seed` has been run first.", vaultAddr, ex.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchPassword(String vaultAddr, String vaultToken) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(vaultAddr + "/v1/secret/data/" + SECRET_PATH))
                    .header("X-Vault-Token", vaultToken)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Vault returned HTTP {} for secret/{}; spring.datasource.password will be unset.",
                        response.statusCode(), SECRET_PATH);
                return null;
            }

            ObjectMapper objectMapper = JsonMapper.builder().build();
            Map<String, Object> root = objectMapper.readValue(response.body(), Map.class);
            Map<String, Object> outer = (Map<String, Object>) root.get("data");
            Map<String, Object> inner = outer == null ? null : (Map<String, Object>) outer.get("data");
            Object password = inner == null ? null : inner.get("db_password");
            if (password == null) {
                log.warn("Vault response for secret/{} had no 'db_password' field; spring.datasource.password will be unset.",
                        SECRET_PATH);
                return null;
            }
            return password.toString();
        }
    }
}
