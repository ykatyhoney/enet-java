package com.enet;

public class ENetPacket {
    private final long handle;
    private final byte[] data;
    private final int flags;

    public ENetPacket(long handle, byte[] data, int flags) {
        this.handle = handle;
        this.data = data;
        this.flags = flags;
    }

    public long getHandle() {
        return handle;
    }

    public byte[] getData() {
        return data;
    }

    public int getFlags() {
        return flags;
    }

    public String getDataAsString() {
        return new String(data);
    }
}

