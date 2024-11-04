package com.poppy.common.config.external;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "naver")
@Getter
@Setter
public class NaverConfig {
    private Url url;
    private Client client;

    @Getter
    @Setter
    public static class Url {
        private Search search;

        @Getter
        @Setter
        public static class Search {
            private String local;
        }
    }

    @Getter
    @Setter
    public static class Client {
        private String id;
        private String secret;
    }
}
