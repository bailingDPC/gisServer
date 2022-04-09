package com.gisserver.filter;

import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import java.io.IOException;

/**
 * @author bailing
 */
@Order(1)
@WebFilter(urlPatterns = "/*", filterName = "authFilter", initParams = {
        @WebInitParam(name = "URL", value = "http://localhost:8080")
})
public class AuthFilter implements Filter
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("======= Filter执行了.....=======");
        filterChain.doFilter(servletRequest, servletResponse);
        System.out.println("======= Filter执行之后.....=======");
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
