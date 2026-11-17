package com.ecommerce.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationScriptTest {

    @Test
    @DisplayName("V1 migration script exists and defines all required relational tables")
    void shouldContainAllV1TablesAndConstraints() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__initial_schema.sql");
        assertThat(resource.exists()).isTrue();

        String sql;
        try (InputStream is = resource.getInputStream()) {
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        List<String> requiredTables = List.of(
                "users",
                "refresh_tokens",
                "categories",
                "products",
                "product_variants",
                "product_images",
                "reviews",
                "carts",
                "cart_items",
                "shipping_addresses",
                "orders",
                "order_items",
                "payments",
                "ledger_entries",
                "webhook_events"
        );

        for (String table : requiredTables) {
            assertThat(sql).as("Table " + table + " must be created in V1").contains("create table " + table);
        }

        // Check UUID primary key pattern
        assertThat(sql).contains("uuid");
        assertThat(sql).contains("primary key default gen_random_uuid()");

        // Check money stored as cents (minor units)
        assertThat(sql).contains("total_amount_minor");
        assertThat(sql).contains("unit_price_minor");
        assertThat(sql).contains("amount_minor");

        // Check Foreign Key constraints
        assertThat(sql).contains("references users(id)");
        assertThat(sql).contains("references categories(id)");
        assertThat(sql).contains("references products(id)");
        assertThat(sql).contains("references orders(id)");
        assertThat(sql).contains("references payments(id)");

        // Check updated_at trigger function
        assertThat(sql).contains("update_updated_at_column");
    }

    @Test
    @DisplayName("V2 migration script exists and defines GIN FTS, composite, and partial indexes")
    void shouldContainV2IndexingAndFtsStrategy() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V2__indexes_and_fts.sql");
        assertThat(resource.exists()).isTrue();

        String sql;
        try (InputStream is = resource.getInputStream()) {
            sql = new String(is.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        // Full-Text Search trigger function and trigger
        assertThat(sql).contains("products_search_vector_update");
        assertThat(sql).contains("to_tsvector('english'");
        assertThat(sql).contains("trg_products_search_vector_update");

        // GIN index for search_vector
        assertThat(sql).contains("idx_products_search");
        assertThat(sql).contains("using gin(search_vector)");

        // Composite index for catalog filtering and sorting
        assertThat(sql).contains("idx_products_category_active_created");
        assertThat(sql).contains("products(category_id, is_active, created_at desc)");

        // Partial index on orders status PENDING
        assertThat(sql).contains("idx_orders_status_pending");
        assertThat(sql).contains("where status = 'pending'");

        // Partial index on stripe payment intent
        assertThat(sql).contains("idx_orders_stripe_pi");
        assertThat(sql).contains("where stripe_payment_intent is not null");

        // Webhook queue partial index
        assertThat(sql).contains("idx_webhook_events_queue");
        assertThat(sql).contains("where status in ('pending', 'processing')");
    }
}
