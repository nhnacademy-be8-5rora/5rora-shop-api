package store.aurora.common.db;

import lombok.RequiredArgsConstructor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import store.aurora.key.DatabaseProperties;
import store.aurora.key.KeyConfig;

import javax.sql.DataSource;

@RequiredArgsConstructor
@Configuration
@Profile("prod")
public class DataSourceConfig {

    private final KeyConfig keyConfig;
    private final DatabaseProperties databaseProperties;

    @Bean
    public DataSource dataSource() {

        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setUrl(keyConfig.keyStore(databaseProperties.getUrl()));
        dataSource.setUsername(keyConfig.keyStore(databaseProperties.getUsername()));
        dataSource.setPassword(keyConfig.keyStore(databaseProperties.getPassword()));
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

        dataSource.setInitialSize(20); // 초기 생성되는 연결 수
        dataSource.setMaxTotal(100); // 최대 연결 수 (전체 요청 처리량 기반)
        dataSource.setMaxIdle(50); // 최대 유휴 연결 수 (자주 사용되지 않을 때의 최적 값)
        dataSource.setMinIdle(20); // 최소 유휴 연결 수 (초기 연결 수와 동일하게 유지)
        dataSource.setValidationQuery("SELECT 1"); // 연결 유효성 확인 쿼리
        dataSource.setTestOnBorrow(true); // 연결 풀에서 가져올 때 유효성 검사
        dataSource.setTestWhileIdle(true); // 유휴 상태에서 유효성 검사

        return dataSource;
    }
}
