package com.insureflow.policyservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Explicitly builds the application DataSource and runs Flyway migrations
 * against it BEFORE the bean is handed off to JPA/Hibernate.
 *
 * This bypasses any ordering/compatibility issues between Spring Boot's
 * FlywayAutoConfiguration and the Hibernate JPA auto-configuration by
 * guaranteeing migration happens synchronously during DataSource bean
 * creation - which Spring resolves as a dependency BEFORE
 * entityManagerFactory is created (since JPA auto-configuration injects
 * the DataSource bean).
 */
@Configuration
public class FlywayMigrationConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String flywayLocations;

    @Value("${spring.flyway.baselineOnMigrate:true}")
    private boolean baselineOnMigrate;

    @Value("${spring.flyway.baseline-version:1}")
    private String baselineVersion;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayLocations)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();

        flyway.migrate();

        return dataSource;
    }
}
