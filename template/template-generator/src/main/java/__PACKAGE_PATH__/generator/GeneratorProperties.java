package __PACKAGE_NAME__.generator;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "generator")
public class GeneratorProperties {

    private String basePackage = "__PACKAGE_NAME__";
    private String author = "generator";
    private boolean fileOverride = false;
    private String[] tablePrefixes = {"t_", "tbl_"};
}
