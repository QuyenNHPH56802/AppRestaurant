package com.restaurant.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Resolves the locale for each request:
 *   1. ?lang= query parameter
 *   2. Accept-Language header
 *   3. Default: vi
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    public static final List<String> SUPPORTED = Arrays.asList("vi", "ko");

    @Bean
    public LocaleResolver localeResolver() {
        return new HeaderOrQueryLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor i = new LocaleChangeInterceptor();
        i.setParamName("lang");
        return i;
    }

    static class HeaderOrQueryLocaleResolver implements LocaleResolver {
        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            String q = request.getParameter("lang");
            if (q != null) {
                String tag = q.trim().toLowerCase();
                if (SUPPORTED.contains(tag)) return Locale.forLanguageTag(tag);
            }
            String accept = request.getHeader("Accept-Language");
            if (accept != null && !accept.isBlank()) {
                for (String part : accept.split(",")) {
                    String tag = part.split(";", 2)[0].trim().toLowerCase();
                    if (tag.isEmpty()) continue;
                    String base = tag.split("-", 2)[0];
                    if (SUPPORTED.contains(base)) return Locale.forLanguageTag(base);
                }
            }
            return Locale.forLanguageTag("vi");
        }

        @Override
        public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
            // no-op: per-request locale is derived, not stored
        }
    }
}