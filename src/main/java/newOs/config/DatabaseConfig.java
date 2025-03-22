package newOs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.relational.core.dialect.Dialect;

@Configuration
public class DatabaseConfig {
    @Bean
    public Dialect jdbcDialect() {
        return new SQLiteDialect();
    }
}