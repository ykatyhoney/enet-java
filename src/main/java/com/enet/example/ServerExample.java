package com.enet.example;

import com.enet.ENetConnection;
import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import com.enet.ENetPeer;

public class ServerExample {
    private ENetConnection server;

    public static void main(String[] args) {
        ServerExample server = new ServerExample();
        server.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void start() {
        server = ENetConnection.createHost("0.0.0.0", 7777, 32, 2);
        server.addEventHandler(this);
        server.startEventLoop();
        System.out.println("Server started on port 7777");
    }

    public void stop() {
        if (server != null) {
            server.close();
            System.out.println("Server stopped");
        }
    }

    @ENetEventHandler(ENetEventType.CONNECT)
    public void onConnect(ENetEvent event) {
        System.out.println("Client connected: " + event.getPeer().getAddress());
    }

    @ENetEventHandler(ENetEventType.DISCONNECT)
    public void onDisconnect(ENetEvent event) {
        System.out.println("Client disconnected: " + event.getPeer().getAddress());
    }

    @ENetEventHandler(ENetEventType.RECEIVE)
    public void onReceive(ENetEvent event) {
        String message = event.getPacket().getDataAsString();
        System.out.println("Received from " + event.getPeer().getAddress() + ": " + message);
        
        String response = "Echo: " + message;
        server.send(event.getPeer(), event.getChannelID(), response.getBytes());
    }
}

