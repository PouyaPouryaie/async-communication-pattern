package ir.bigz.polling.orderPollingApi;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the Angular development client.
 *
 * <p>The browser sends a cross-origin request when the Angular dev server and
 * this API run on different ports. Spring must answer both the actual request
 * and the browser's preflight {@code OPTIONS} request with the appropriate
 * CORS headers.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Allows the local Angular development server to call the REST API.
     *
     * @param registry Spring MVC's CORS mapping registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:4200")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}

