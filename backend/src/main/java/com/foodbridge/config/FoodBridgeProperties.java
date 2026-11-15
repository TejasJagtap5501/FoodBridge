package com.foodbridge.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Strongly typed binding for the {@code foodbridge.*} configuration tree. Every
 * value can be overridden through an environment variable (see
 * {@code application.yml} and {@code .env.example}) so no secret needs to live
 * in VCS.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "foodbridge")
public class FoodBridgeProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Uploads uploads = new Uploads();
    private final Seed seed = new Seed();
    private final Expiry expiry = new Expiry();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(7);
        private String issuer = "foodbridge";
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
        /**
         * Origin <em>patterns</em> support wildcards and, unlike
         * {@code allowedOrigins}, remain compatible with
         * {@code allowCredentials}. Used for preview/sandbox hostnames such as
         * {@code https://8080-xyz.e2b.app} and the {@code null} origin that a
         * sandboxed iframe sends.
         */
        private String allowedOriginPatterns = "";

        public List<String> origins() {
            return split(allowedOrigins);
        }

        public List<String> originPatterns() {
            return split(allowedOriginPatterns);
        }

        private static List<String> split(String value) {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class Security {
        /** SAMEORIGIN, DENY or DISABLE. DISABLE is required for iframe previews. */
        private String frameOptions = "SAMEORIGIN";
        private boolean cookieSecure = false;
        private String cookieSameSite = "Lax";
        private String refreshCookieName = "fb_refresh";
        private int maxFailedLogins = 10;
        private int lockoutMinutes = 15;
        /**
         * When true the password-reset token is echoed in the API response. This
         * exists purely so the flow is usable without a mail server in local
         * development; {@code application-prod.yml} forces it to false.
         */
        private boolean exposeResetToken = true;
    }

    @Getter
    @Setter
    public static class Uploads {
        private String directory = "./uploads";
        private long maxBytes = 5 * 1024 * 1024;
        private String allowedTypes = "image/jpeg,image/png,image/webp";

        public List<String> allowedTypeList() {
            return Arrays.stream(allowedTypes.split(",")).map(String::trim).toList();
        }
    }

    @Getter
    @Setter
    public static class Seed {
        private boolean demoData = true;
    }

    @Getter
    @Setter
    public static class Expiry {
        private long scanIntervalMs = 60_000;
        private int reminderHoursBefore = 3;
    }
}
