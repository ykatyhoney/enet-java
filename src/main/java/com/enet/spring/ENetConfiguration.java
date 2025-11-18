package com.enet.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ENetConfiguration {
    
    @Bean
    public ENetEventHandlerRegistry enetEventHandlerRegistry() {
        return new ENetEventHandlerRegistry();
    }
}

