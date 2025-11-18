package com.enet;

public enum ENetEventType {
    NONE(0),
    CONNECT(1),
    DISCONNECT(2),
    RECEIVE(3);

    private final int value;

    ENetEventType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ENetEventType fromValue(int value) {
        for (ENetEventType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return NONE;
    }
}

