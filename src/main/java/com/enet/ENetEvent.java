package com.enet;

public class ENetEvent {
    private final int type;
    private final ENetPeer peer;
    private final byte channelID;
    private final int data;
    private final ENetPacket packet;

    public ENetEvent(int type, ENetPeer peer, byte channelID, int data, ENetPacket packet) {
        this.type = type;
        this.peer = peer;
        this.channelID = channelID;
        this.data = data;
        this.packet = packet;
    }

    public ENetEventType getEventType() {
        return ENetEventType.fromValue(type);
    }

    public int getType() {
        return type;
    }

    public ENetPeer getPeer() {
        return peer;
    }

    public byte getChannelID() {
        return channelID;
    }

    public int getData() {
        return data;
    }

    public ENetPacket getPacket() {
        return packet;
    }
}

