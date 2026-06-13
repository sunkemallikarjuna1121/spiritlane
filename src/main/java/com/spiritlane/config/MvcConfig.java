package com.spiritlane.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    private static final Logger logger =
            LoggerFactory.getLogger(MvcConfig.class);

    @Value("${app.upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        logger.info("[MvcConfig] addResourceHandlers : START");

        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            logger.error("[MvcConfig] addResourceHandlers : uploadDir is null or empty");
            return;
        }

        try {

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + uploadDir);

            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");

            logger.info("[MvcConfig] addResourceHandlers : Resource handlers registered successfully");

        } catch (Exception ex) {

            logger.error("[MvcConfig] addResourceHandlers : Error while registering resource handlers", ex);
            return;
        }

        logger.info("[MvcConfig] addResourceHandlers : END");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        logger.info("[MvcConfig] addViewControllers : START");

        try {

            registry.addRedirectViewController("/", "/home");

            logger.info("[MvcConfig] addViewControllers : View controllers registered successfully");

        } catch (Exception ex) {

            logger.error("[MvcConfig] addViewControllers : Error while registering view controllers", ex);
            return;
        }

        logger.info("[MvcConfig] addViewControllers : END");
    }
}