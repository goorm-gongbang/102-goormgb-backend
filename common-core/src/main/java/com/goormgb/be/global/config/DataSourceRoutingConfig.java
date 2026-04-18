package com.goormgb.be.global.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Read Replica 라우팅 설정.
 * <p>
 * {@code datasource.routing.enabled=true} 일 때만 활성화된다.
 * {@code @Transactional(readOnly = true)} 가 붙은 메서드는 replica로,
 * 나머지는 primary로 커넥션을 라우팅한다.
 * <p>
 * replica URL 을 primary 와 동일하게 설정하면 단일 DB 에서도 안전하게 동작한다.
 * <p>
 * Spring Boot DataSourceAutoConfiguration 은 {@code @ConditionalOnMissingBean(DataSource.class)}
 * 이므로, 이 config 의 {@code @Primary} DataSource 빈이 먼저 등록되어 auto-config 은 자동 스킵된다.
 */
@Configuration
@ConditionalOnProperty(name = "datasource.routing.enabled", havingValue = "true")
public class DataSourceRoutingConfig {

    @Bean
    @ConfigurationProperties(prefix = "datasource.routing.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @ConfigurationProperties(prefix = "datasource.routing.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean
    @DependsOn({"primaryDataSource", "replicaDataSource"})
    public DataSource routingDataSource() {
        ReplicaRoutingDataSource routing = new ReplicaRoutingDataSource();

        Map<Object, Object> targets = new HashMap<>();
        targets.put("primary", primaryDataSource());
        targets.put("replica", replicaDataSource());

        routing.setTargetDataSources(targets);
        routing.setDefaultTargetDataSource(primaryDataSource());

        return routing;
    }

    @Bean
    @Primary
    @DependsOn("routingDataSource")
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }

    static class ReplicaRoutingDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                    ? "replica"
                    : "primary";
        }
    }
}
