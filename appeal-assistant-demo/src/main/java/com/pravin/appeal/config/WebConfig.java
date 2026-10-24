package com.pravin.appeal.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC setup:
 *  - @ComponentScan finds our @Controller, @Service and @Component classes
 *  - view name "worklist" -> /WEB-INF/views/worklist.jsp
 */
@Configuration
@EnableWebMvc
@ComponentScan("com.pravin.appeal")
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }
}
