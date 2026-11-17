package com.ecommerce.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA & Database Reliability Configuration.
 * <p>
 * Configures HikariCP connection pooling optimized for PostgreSQL Serverless (Neon)
 * with keep-alive, connection lifetime management, leak detection, and transaction management.
 * </p>
 */
@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
public class JpaConfig {

    private static final Logger log = LoggerFactory.getLogger(JpaConfig.class);

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        log.info("Configuring HikariDataSource with Neon-optimized reliability settings...");
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();

        // Enforce fallback pool-name if not bound
        if (dataSource.getPoolName() == null || dataSource.getPoolName().startsWith("HikariPool-")) {
            dataSource.setPoolName("EcommerceHikariCP");
        }

        log.info("HikariCP initialized: poolName={}, maximumPoolSize={}, minimumIdle={}, leakDetectionThreshold={}ms",
                dataSource.getPoolName(),
                dataSource.getMaximumPoolSize(),
                dataSource.getMinimumIdle(),
                dataSource.getLeakDetectionThreshold());

        return dataSource;
    }
}
