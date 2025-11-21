package com.enet.example;

import com.enet.ENetConnection;
import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import com.enet.ENetPacketFlags;
import com.enet.ENetPeer;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ServerExample {
    private ENetConnection server;
    private final Map<ENetPeer, String> connectedClients = new ConcurrentHashMap<>();
    private final Map<ENetPeer, Boolean> welcomeSent = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ServerExample server = new ServerExample();
        server.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        // Start input thread for server to send messages
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Server ready. Type messages to broadcast to all clients (or 'quit' to exit):");
            
            while (true) {
                String input = scanner.nextLine();
                if ("quit".equalsIgnoreCase(input)) {
                    break;
                }
                if (!input.trim().isEmpty()) {
                    server.broadcastMessage(input);
                }
            }
            
            scanner.close();
            server.stop();
            System.exit(0);
        });
        inputThread.setDaemon(true);
        inputThread.start();
        
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
        ENetPeer peer = event.getPeer();
        String clientInfo = peer.getAddress().toString();
        connectedClients.put(peer, clientInfo);
        System.out.println("Client connected: " + clientInfo + " (Total clients: " + connectedClients.size() + ")");
        
        // Note: Not sending welcome message automatically to avoid Godot _process_sys() errors
        // Welcome message will be sent as echo response when client sends first message
        // If you need a welcome message, send it after client's first packet in onReceive()
    }

    @ENetEventHandler(ENetEventType.DISCONNECT)
    public void onDisconnect(ENetEvent event) {
        ENetPeer peer = event.getPeer();
        String clientInfo = connectedClients.remove(peer);
        welcomeSent.remove(peer);
        System.out.println("Client disconnected: " + clientInfo + " (Total clients: " + connectedClients.size() + ")");
    }

    @ENetEventHandler(ENetEventType.RECEIVE)
    public void onReceive(ENetEvent event) {
        ENetPeer peer = event.getPeer();
        byte channelID = event.getChannelID();
        String clientInfo = connectedClients.getOrDefault(peer, peer.getAddress().toString());
        
        String message = event.getPacket().getDataAsString();
        System.out.println("[" + clientInfo + "] " + message + " (channel: " + channelID + ")");

        // Send welcome message on first received packet (after Godot connection is fully established)
        if (!welcomeSent.getOrDefault(peer, false)) {
            String welcome = "Welcome to the server! You are connected.";
            byte[] welcomeData = welcome.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            // Send plain data on channel 0 - Godot now uses direct ENet connection
            server.send(peer, (byte) 0, welcomeData, ENetPacketFlags.RELIABLE);
            welcomeSent.put(peer, true);
            server.flush();
            System.out.println("Sent welcome message to " + clientInfo + " (channel 0, size: " + welcomeData.length + ")");
        }

        // Send plain responses on channel 0 - no special formatting needed
        String response = "Echo: " + message;
        byte[] data = response.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int result = server.send(peer, (byte) 0, data, ENetPacketFlags.RELIABLE);
        server.flush();
        System.out.println("Echo sent, result: " + result + ", data length: " + data.length + ", channel: 0");
    }
    
    public void broadcastMessage(String message) {
        if (connectedClients.isEmpty()) {
            System.out.println("No clients connected to send message to.");
            return;
        }

        System.out.println("Broadcasting to " + connectedClients.size() + " client(s): " + message);
        // Send plain data on channel 0 - Godot now uses direct ENet connection
        byte[] data = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        server.broadcast((byte) 0, data, ENetPacketFlags.RELIABLE);
        server.flush();
        System.out.println("Broadcast sent, data length: " + data.length);
    }
    
    public void sendToClient(ENetPeer peer, String message) {
        if (connectedClients.containsKey(peer)) {
            // Send plain data on channel 0 - Godot now uses direct ENet connection
            byte[] data = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            server.send(peer, (byte) 0, data, ENetPacketFlags.RELIABLE);
            server.flush();
        } else {
            System.out.println("Client not found or disconnected.");
        }
    }
}

