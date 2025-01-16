package store.aurora.key;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "database.mysql")
public class DatabaseProperties {
    private String url;
    private String username;
    private String password;
}
