package com.manhnv.capstonebakerai.identity.config;

import com.manhnv.capstonebakerai.common.exception.SecurityExceptionHandler;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductWarehouseAuthorizationTest {

    private record Endpoint(HttpMethod method, String path) { }

    private AnnotationConfigWebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "app.cors.allowed-origin=http://localhost:5173");
        context.register(MvcConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void closeContext() {
        context.close();
    }

    @Test
    void catalogWritesRequireManagerButStaffCanRead() throws Exception {
        for (Endpoint endpoint : List.of(
                new Endpoint(HttpMethod.POST, "/api/products"),
                new Endpoint(HttpMethod.PUT, "/api/products/id"),
                new Endpoint(HttpMethod.PUT, "/api/products/id/combo-items"),
                new Endpoint(HttpMethod.DELETE, "/api/products/id"),
                new Endpoint(HttpMethod.POST, "/api/product-categories"),
                new Endpoint(HttpMethod.PUT, "/api/product-categories/id"),
                new Endpoint(HttpMethod.DELETE, "/api/product-categories/id"))) {
            assertStatus(endpoint, "MANAGER", 200);
            assertStatus(endpoint, "STAFF", 403);
            assertStatus(endpoint, "CUSTOMER", 403);
            assertStatus(endpoint, null, 401);
        }
        assertStatus(new Endpoint(HttpMethod.GET, "/api/products"), "STAFF", 200);
        assertStatus(new Endpoint(HttpMethod.GET, "/api/products/id/combo-items"), "STAFF", 200);
        assertStatus(new Endpoint(HttpMethod.GET, "/api/product-categories"), "STAFF", 200);
        assertStatus(new Endpoint(HttpMethod.POST, "/api/products/low-stock/alerts"), "STAFF", 200);
    }

    @Test
    void stockReceiptWritesRequireStaffOrManager() throws Exception {
        for (Endpoint endpoint : List.of(
                new Endpoint(HttpMethod.POST, "/api/warehouse/stock-in"),
                new Endpoint(HttpMethod.PUT, "/api/warehouse/stock-in/id"),
                new Endpoint(HttpMethod.POST, "/api/warehouse/stock-in/id/complete"),
                new Endpoint(HttpMethod.POST, "/api/warehouse/stock-in/id/cancel"))) {
            assertStatus(endpoint, "MANAGER", 200);
            assertStatus(endpoint, "STAFF", 200);
            assertStatus(endpoint, "CUSTOMER", 403);
            assertStatus(endpoint, "BAKER_STAFF", 403);
            assertStatus(endpoint, null, 401);
        }
    }

    private void assertStatus(Endpoint endpoint, String role, int expectedStatus) throws Exception {
        var call = request(endpoint.method(), endpoint.path());
        if (role != null) call.header(HttpHeaders.AUTHORIZATION, "Bearer " + role);
        mvc.perform(call).andExpect(status().is(expectedStatus));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, TestEndpoints.class})
    static class MvcConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token).header("alg", "RS256")
                    .subject("test-account").claim("role", token).build();
        }

        @Bean
        SecurityExceptionHandler securityExceptionHandler() {
            return new SecurityExceptionHandler(JsonMapper.builder().build());
        }
    }

    @RestController
    static class TestEndpoints {
        @RequestMapping({"/api/products/**", "/api/product-categories/**", "/api/warehouse/stock-in/**"})
        String reachedController() {
            return "authorized";
        }
    }
}
