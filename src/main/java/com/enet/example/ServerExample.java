package com.enet.example;

import com.enet.ENetConnection;
import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import com.enet.ENetPeer;

import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class ServerExample {
    private ENetConnection server;
    private final Map<ENetPeer, String> connectedClients = new ConcurrentHashMap<>();

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
        
        // Send welcome message
        String welcome = "Welcome to the server! You are connected.";
        server.send(peer, (byte) 0, welcome.getBytes());
        server.flush();
    }

    @ENetEventHandler(ENetEventType.DISCONNECT)
    public void onDisconnect(ENetEvent event) {
        ENetPeer peer = event.getPeer();
        String clientInfo = connectedClients.remove(peer);
        System.out.println("Client disconnected: " + clientInfo + " (Total clients: " + connectedClients.size() + ")");
    }

    @ENetEventHandler(ENetEventType.RECEIVE)
    public void onReceive(ENetEvent event) {
        ENetPeer peer = event.getPeer();
        String message = event.getPacket().getDataAsString();
        String clientInfo = connectedClients.getOrDefault(peer, peer.getAddress().toString());
        System.out.println("[" + clientInfo + "] " + message);
        
        // Echo back to the sender
        String response = "Echo: " + message;
        server.send(peer, event.getChannelID(), response.getBytes());
        server.flush();
    }
    
    public void broadcastMessage(String message) {
        if (connectedClients.isEmpty()) {
            System.out.println("No clients connected to send message to.");
            return;
        }
        
        System.out.println("Broadcasting to " + connectedClients.size() + " client(s): " + message);
        server.broadcast((byte) 0, message.getBytes());
        server.flush();
    }
    
    public void sendToClient(ENetPeer peer, String message) {
        if (connectedClients.containsKey(peer)) {
            server.send(peer, (byte) 0, message.getBytes());
            server.flush();
        } else {
            System.out.println("Client not found or disconnected.");
        }
    }
}

