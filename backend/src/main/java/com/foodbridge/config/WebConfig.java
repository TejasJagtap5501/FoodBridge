package com.foodbridge.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Static resource wiring.
 *
 * <ol>
 *   <li><b>Uploads</b> — donation photos are served from the configured upload
 *       directory. Only generated storage keys exist on disk, the original
 *       filename is never used as a path, so directory traversal is impossible.</li>
 *   <li><b>SPA fallback</b> — the React build is bundled into
 *       {@code classpath:/static/}. Any GET that does not resolve to a real file
 *       is answered with {@code index.html} so React Router can handle deep links
 *       such as {@code /donor/donations/42}. API, actuator, upload and Swagger
 *       paths are excluded because they are registered as handlers, and a
 *       handler always wins over a resource handler.</li>
 * </ol>
 *
 * <p>Bundling the SPA into the backend keeps one deployable artefact and one
 * origin for UI + API, which removes CORS from the production surface.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    private final FoodBridgeProperties properties;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(properties.getUploads().getDirectory()).toAbsolutePath().normalize();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath.toUri().toString())
                .setCachePeriod(3600);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    /**
     * Returns the requested static file when it exists, otherwise {@code index.html}.
     * Requests for {@code /api/**}, {@code /actuator/**}, {@code /uploads/**} and
     * the Swagger paths resolve to their own controllers and never reach here, but
     * they are also rejected defensively so a missing backend route can never be
     * masked by an HTML page (which would confuse API clients).
     */
    static class SpaResourceResolver extends PathResourceResolver {

        private static final String INDEX = "index.html";

        @Override
        protected Resource getResource(@NonNull String resourcePath, @NonNull Resource location) throws IOException {
            Resource requested = location.createRelative(resourcePath);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")
                    || resourcePath.startsWith("uploads/") || resourcePath.startsWith("swagger-ui/")
                    || resourcePath.startsWith("api-docs")) {
                return null;
            }
            // A missing asset (e.g. /assets/old-chunk.js) must 404 rather than
            // return HTML, otherwise the browser reports a confusing MIME error.
            if (hasFileExtension(resourcePath)) {
                return null;
            }
            Resource index = location.createRelative(INDEX);
            return index.exists() && index.isReadable() ? index : null;
        }

        private boolean hasFileExtension(String path) {
            int lastSlash = path.lastIndexOf('/');
            int lastDot = path.lastIndexOf('.');
            return lastDot > lastSlash;
        }
    }
}
