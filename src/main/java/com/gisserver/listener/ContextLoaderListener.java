package com.gisserver.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author bailing
 */
@WebListener
public class ContextLoaderListener implements ServletContextListener
{
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 加载资源
        System.out.println("contextInitialized.......");
        ServletContextListener.super.contextInitialized(sce);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 释放资源
        System.out.println("contextDestroyed.......");
        ServletContextListener.super.contextDestroyed(sce);
    }
}
