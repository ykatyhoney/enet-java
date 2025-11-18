package com.enet;

public class ENetPeer {
    private final long handle;
    private final ENetAddress address;

    public ENetPeer(long handle, ENetAddress address) {
        this.handle = handle;
        this.address = address;
    }

    public long getHandle() {
        return handle;
    }

    public ENetAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ENetPeer{address=" + address + "}";
    }
}

