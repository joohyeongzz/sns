package com.joohyeong.sns.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class DataSourceConfig {

    private final DBProperty dbProperty;

    @Bean
    public DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        DataSource masterDataSource = createDataSource(
                dbProperty.getWriteurl(),
                dbProperty.getUsername(),
                dbProperty.getPassword(),
                dbProperty.getDriver()
        );
        targetDataSources.put("master", masterDataSource);

        List<String> slaveUrls = dbProperty.getReadurls();
        for (int i = 0; i < slaveUrls.size(); i++) {
            DataSource slaveDataSource = createDataSource(
                    slaveUrls.get(i),
                    dbProperty.getUsername(),
                    dbProperty.getPassword(),
                    dbProperty.getDriver()
            );
            targetDataSources.put("slave" + i, slaveDataSource);
        }

        AbstractRoutingDataSource routingDataSource = new ReplicationRoutingDataSource(slaveUrls.size());
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);

        return routingDataSource;
    }

    private DataSource createDataSource(String url, String username, String password, String driverClassName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        return new HikariDataSource(config);
    }

    @Bean
    public DataSource dataSource() {
        DataSource routingDataSource = routingDataSource();
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}

