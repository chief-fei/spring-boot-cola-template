package __PACKAGE_NAME__.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.custom")
public class AppProperties {

    private String name = "__PROJECT_NAME__";

    private String version = "1.0.0";

    private boolean debugEnabled = false;

    private int maxRetryCount = 3;

    private UploadConfig upload = new UploadConfig();

    @Data
    public static class UploadConfig {
        private String path = "/tmp/uploads";
        private long maxSize = 10485760L;
        private String[] allowedTypes = {"jpg", "png", "pdf", "xlsx"};
    }
}
