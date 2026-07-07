package com.duoc.gestionguias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class OracleJdbcConfig {

    @Value("${oracle.datasource.url}")
    private String oracleUrl;

    @Value("${oracle.datasource.username}")
    private String oracleUsername;

    @Value("${oracle.datasource.password}")
    private String oraclePassword;

    /*
     * JdbcTemplate independiente para Oracle Cloud.
     */
    @Bean(name = "oracleJdbcTemplate")
    public JdbcTemplate oracleJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUrl(oracleUrl);
        dataSource.setUsername(oracleUsername);
        dataSource.setPassword(oraclePassword);

        return new JdbcTemplate(dataSource);
    }
}