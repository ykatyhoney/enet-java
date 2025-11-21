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
        // Handle null-terminated strings (C-style)
        int length = data.length;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0) {
                length = i;
                break;
            }
        }
        return new String(data, 0, length, java.nio.charset.StandardCharsets.UTF_8);
    }
}

