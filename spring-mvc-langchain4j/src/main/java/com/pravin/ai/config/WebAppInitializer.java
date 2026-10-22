package com.pravin.ai.config;

import java.util.EnumSet;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionTrackingMode;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/**
 * Replaces web.xml. Tomcat finds this class at startup and registers
 * Spring's DispatcherServlet, which sends every request to our controllers.
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);
        // keep the session id in a cookie only, never in the URL (;jsessionid=...)
        servletContext.setSessionTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE));
    }

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return null; // one config class is enough for this small app
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" }; // handle all URLs
    }

    @Override
    protected Filter[] getServletFilters() {
        // read form input and write pages as UTF-8
        return new Filter[] { new CharacterEncodingFilter("UTF-8", true) };
    }
}
