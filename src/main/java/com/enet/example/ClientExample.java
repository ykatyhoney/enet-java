package com.enet.example;

import com.enet.ENetConnection;
import com.enet.ENetEvent;
import com.enet.ENetEventType;
import com.enet.ENetEventHandler;
import com.enet.ENetPeer;

import java.util.Scanner;

public class ClientExample {
    private ENetConnection client;
    private ENetPeer serverPeer;
    private boolean connected = false;

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7777;
        
        ClientExample client = new ClientExample();
        client.start(host, port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(client::stop));
        
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type messages to send (or 'quit' to exit):");
        
        while (true) {
            String input = scanner.nextLine();
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            if (client.connected) {
                client.sendMessage(input);
            } else {
                System.out.println("Not connected yet...");
            }
        }
        
        scanner.close();
        client.stop();
    }

    public void start(String host, int port) {
        client = ENetConnection.createHostBound(null, 0, 1, 2);
        client.addEventHandler(this);
        client.startEventLoop();
        
        try {
            serverPeer = client.connect(host, port, 2);
            System.out.println("Connecting to " + host + ":" + port + "...");
        } catch (Exception e) {
            System.err.println("Failed to connect: " + e.getMessage());
            System.exit(1);
        }
    }

    public void stop() {
        if (client != null) {
            if (connected && serverPeer != null) {
                client.disconnectPeer(serverPeer);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            client.close();
            System.out.println("Client disconnected");
        }
    }

    public void sendMessage(String message) {
        if (connected && serverPeer != null) {
            client.send(serverPeer, (byte) 0, message.getBytes());
            client.flush();
        }
    }

    @ENetEventHandler(ENetEventType.CONNECT)
    public void onConnect(ENetEvent event) {
        connected = true;
        System.out.println("Connected to server!");
    }

    @ENetEventHandler(ENetEventType.DISCONNECT)
    public void onDisconnect(ENetEvent event) {
        connected = false;
        System.out.println("Disconnected from server");
    }

    @ENetEventHandler(ENetEventType.RECEIVE)
    public void onReceive(ENetEvent event) {
        String message = event.getPacket().getDataAsString();
        System.out.println("Server: " + message);
    }
}

