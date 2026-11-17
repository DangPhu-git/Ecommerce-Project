package com.ecommerce.database;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HikariCPReliabilityTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("HikariCP pool should be initialized with reliability configuration")
    void shouldInitializeHikariWithReliabilitySettings() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        assertThat(hikari.getPoolName()).isEqualTo("EcommerceHikariCP");
        assertThat(hikari.getConnectionTimeout()).isEqualTo(30000);
        assertThat(hikari.getIdleTimeout()).isEqualTo(300000);
        assertThat(hikari.getMaxLifetime()).isEqualTo(1800000);
        assertThat(hikari.getKeepaliveTime()).isEqualTo(60000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(60000);
        assertThat(hikari.getMaximumPoolSize()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Should successfully acquire and release connection without leak")
    void shouldAcquireAndReleaseConnectionWithoutLeak() throws Exception {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource hikari = (HikariDataSource) dataSource;

        int initialActive = hikari.getHikariPoolMXBean() != null
                ? hikari.getHikariPoolMXBean().getActiveConnections()
                : 0;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }

        // Connection returned to pool
        if (hikari.getHikariPoolMXBean() != null) {
            assertThat(hikari.getHikariPoolMXBean().getActiveConnections()).isEqualTo(initialActive);
        }
    }

    @Test
    @DisplayName("Reconnection test: multiple connection cycles execute cleanly")
    void shouldHandleReconnectionCycles() throws Exception {
        for (int i = 0; i < 10; i++) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT " + i)) {

                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(i);
            }
        }
    }
}
