package com.restaurant.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Maps /uploads/** to the runtime uploads directory on disk so admins can
 * preview images they uploaded, and the staff app can fetch food images via
 * the same base URL.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final RestaurantProperties props;

    public StaticResourceConfig(RestaurantProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String uploadsAbs = "file:" + Paths.get(props.getUploadsDir()).toAbsolutePath() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsAbs)
                .setCachePeriod(3600);
    }
}