package __PACKAGE_NAME__.infrastructure.config;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@Configuration
public class CustomBeanConfig {

    @Bean("traceIdGenerator")
    public Supplier<String> traceIdGenerator() {
        return () -> IdUtil.fastSimpleUUID();
    }

    @Bean("dateTimeFormatter")
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    @Bean("currentDateTimeSupplier")
    public Supplier<LocalDateTime> currentDateTimeSupplier() {
        return LocalDateTime::now;
    }
}
