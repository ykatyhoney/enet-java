package com.enet.spring;

import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ENetEventHandlerRegistry implements ApplicationContextAware {
    private final Map<ENetEventType, List<HandlerMethod>> handlers = new HashMap<>();
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            if (bean != this && hasHandlerMethods(bean)) {
                registerHandler(bean);
            }
        }
    }

    private boolean hasHandlerMethods(Object bean) {
        for (Method method : bean.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(ENetEventHandler.class) != null) {
                return true;
            }
        }
        return false;
    }

    public void registerHandler(Object handler) {
        for (Method method : handler.getClass().getDeclaredMethods()) {
            ENetEventHandler annotation = method.getAnnotation(ENetEventHandler.class);
            if (annotation != null) {
                if (method.getParameterCount() != 1 || 
                    !method.getParameterTypes()[0].equals(ENetEvent.class)) {
                    throw new IllegalArgumentException(
                        "Handler method must accept exactly one ENetEvent parameter: " + method.getName());
                }
                method.setAccessible(true);
                handlers.computeIfAbsent(annotation.value(), k -> new ArrayList<>())
                        .add(new HandlerMethod(handler, method));
            }
        }
    }

    public void handleConnect(ENetEvent event) {
        invokeHandlers(ENetEventType.CONNECT, event);
    }

    public void handleDisconnect(ENetEvent event) {
        invokeHandlers(ENetEventType.DISCONNECT, event);
    }

    public void handleReceive(ENetEvent event) {
        invokeHandlers(ENetEventType.RECEIVE, event);
    }

    private void invokeHandlers(ENetEventType eventType, ENetEvent event) {
        List<HandlerMethod> methods = handlers.get(eventType);
        if (methods != null) {
            for (HandlerMethod handlerMethod : methods) {
                try {
                    handlerMethod.method.invoke(handlerMethod.handler, event);
                } catch (Exception e) {
                    System.err.println("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static class HandlerMethod {
        final Object handler;
        final Method method;

        HandlerMethod(Object handler, Method method) {
            this.handler = handler;
            this.method = method;
        }
    }
}

