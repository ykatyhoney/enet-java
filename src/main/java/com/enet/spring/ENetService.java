package com.enet.spring;

import com.enet.ENetConnection;
import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class ENetService {
    private ENetConnection connection;
    private final ENetEventHandlerRegistry handlerRegistry;

    public ENetService(ENetEventHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @PostConstruct
    public void init() {
        connection = ENetConnection.createHost("0.0.0.0", 7777, 32, 2);
        connection.addEventHandler(this);
        connection.startEventLoop();
    }

    @PreDestroy
    public void cleanup() {
        if (connection != null) {
            connection.close();
        }
    }

    public ENetConnection getConnection() {
        return connection;
    }

    @ENetEventHandler(ENetEventType.CONNECT)
    public void onConnect(ENetEvent event) {
        handlerRegistry.handleConnect(event);
    }

    @ENetEventHandler(ENetEventType.DISCONNECT)
    public void onDisconnect(ENetEvent event) {
        handlerRegistry.handleDisconnect(event);
    }

    @ENetEventHandler(ENetEventType.RECEIVE)
    public void onReceive(ENetEvent event) {
        handlerRegistry.handleReceive(event);
    }
}

