package com.enet;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ENetConnection implements AutoCloseable {
    private static boolean initialized = false;
    
    private long hostHandle;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService eventLoop;
    private final List<Object> handlers = new ArrayList<>();
    private final Map<ENetEventType, List<Method>> handlerMethods = new HashMap<>();

    static {
        try {
            // Try loading from native folder first
            String userDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();
            String libName;
            
            if (osName.contains("win")) {
                libName = "enet_jni.dll";
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                libName = "libenet_jni.dylib";
            } else {
                libName = "libenet_jni.so";
            }
            
            String nativePath = userDir + File.separator + "native" + File.separator + libName;
            File libFile = new File(nativePath);
            
            if (libFile.exists()) {
                System.load(nativePath);
            } else {
                // Fallback to system library search
                System.loadLibrary("enet_jni");
            }
        } catch (UnsatisfiedLinkError e) {
            throw new UnsatisfiedLinkError(
                "Failed to load enet_jni library. Make sure the native library is in the native folder or in your PATH. " +
                "Error: " + e.getMessage());
        }
    }

    public static synchronized void initialize() {
        if (!initialized) {
            int result = nativeInitialize();
            if (result != 0) {
                throw new RuntimeException("Failed to initialize ENet: error code " + result);
            }
            initialized = true;
        }
    }

    public static synchronized void deinitialize() {
        if (initialized) {
            nativeDeinitialize();
            initialized = false;
        }
    }

    public static ENetConnection createHost(String host, int port, int maxClients, int maxChannels) {
        return createHost(host, port, maxClients, maxChannels, 0, 0);
    }

    public static ENetConnection createHost(String host, int port, int maxClients, int maxChannels,
                                           int incomingBandwidth, int outgoingBandwidth) {
        initialize();
        ENetConnection connection = new ENetConnection();
        connection.hostHandle = nativeCreateHost(host, port, maxClients, maxChannels,
                incomingBandwidth, outgoingBandwidth);
        if (connection.hostHandle == 0) {
            throw new RuntimeException("Failed to create ENet host on " + 
                (host != null ? host : "0.0.0.0") + ":" + port + 
                ". Check if the port is available and ENet is properly initialized.");
        }
        return connection;
    }

    public static ENetConnection createHostBound(String host, int port, int maxPeers, int maxChannels) {
        return createHostBound(host, port, maxPeers, maxChannels, 0, 0);
    }

    public static ENetConnection createHostBound(String host, int port, int maxPeers, int maxChannels,
                                                int incomingBandwidth, int outgoingBandwidth) {
        initialize();
        ENetConnection connection = new ENetConnection();
        connection.hostHandle = nativeCreateHostBound(host, port, maxPeers, maxChannels,
                incomingBandwidth, outgoingBandwidth);
        if (connection.hostHandle == 0) {
            throw new RuntimeException("Failed to create ENet host bound");
        }
        return connection;
    }

    public void addEventHandler(Object handler) {
        handlers.add(handler);
        scanHandlerMethods(handler);
    }

    private void scanHandlerMethods(Object handler) {
        for (Method method : handler.getClass().getDeclaredMethods()) {
            ENetEventHandler annotation = method.getAnnotation(ENetEventHandler.class);
            if (annotation != null) {
                if (method.getParameterCount() != 1 || 
                    !method.getParameterTypes()[0].equals(ENetEvent.class)) {
                    throw new IllegalArgumentException(
                        "Handler method must accept exactly one ENetEvent parameter: " + method.getName());
                }
                method.setAccessible(true);
                handlerMethods.computeIfAbsent(annotation.value(), k -> new ArrayList<>()).add(method);
            }
        }
    }

    public void startEventLoop() {
        if (running.compareAndSet(false, true)) {
            eventLoop = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ENet-EventLoop");
                t.setDaemon(true);
                return t;
            });
            eventLoop.execute(this::eventLoop);
        }
    }

    public void stopEventLoop() {
        if (running.compareAndSet(true, false)) {
            if (eventLoop != null) {
                eventLoop.shutdown();
            }
        }
    }

    private void eventLoop() {
        while (running.get()) {
            ENetEvent event = service(10);
            if (event != null) {
                handleEvent(event);
            }
        }
    }

    private void handleEvent(ENetEvent event) {
        ENetEventType eventType = event.getEventType();
        List<Method> methods = handlerMethods.get(eventType);
        if (methods != null) {
            for (Method method : methods) {
                for (Object handler : handlers) {
                    try {
                        method.invoke(handler, event);
                    } catch (Exception e) {
                        System.err.println("Error handling event: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public ENetEvent service(int timeoutMillis) {
        return nativeService(hostHandle, timeoutMillis);
    }

    public ENetPeer connect(String host, int port, int channelCount) {
        return connect(host, port, channelCount, 0);
    }

    public ENetPeer connect(String host, int port, int channelCount, int data) {
        long peerHandle = nativeConnect(hostHandle, host, port, channelCount, data);
        if (peerHandle == 0) {
            throw new RuntimeException("Failed to connect to " + host + ":" + port);
        }
        return new ENetPeer(peerHandle, new ENetAddress(host, port));
    }

    public int send(ENetPeer peer, byte channelID, byte[] data) {
        return send(peer, channelID, data, ENetPacketFlags.RELIABLE);
    }

    public int send(ENetPeer peer, byte channelID, byte[] data, int flags) {
        return nativeSend(hostHandle, peer.getHandle(), channelID, data, flags);
    }

    public void broadcast(byte channelID, byte[] data) {
        broadcast(channelID, data, ENetPacketFlags.RELIABLE);
    }

    public void broadcast(byte channelID, byte[] data, int flags) {
        nativeBroadcast(hostHandle, channelID, data, flags);
    }

    public void flush() {
        nativeFlush(hostHandle);
    }

    public void disconnectPeer(ENetPeer peer) {
        disconnectPeer(peer, 0);
    }

    public void disconnectPeer(ENetPeer peer, int data) {
        nativeDisconnectPeer(hostHandle, peer.getHandle(), data);
    }

    @Override
    public void close() {
        stopEventLoop();
        if (hostHandle != 0) {
            nativeDestroy(hostHandle);
            hostHandle = 0;
        }
    }

    // Native methods
    private static native int nativeInitialize();
    private static native void nativeDeinitialize();
    private static native long nativeCreateHost(String host, int port, int maxClients, int maxChannels,
                                               int incomingBandwidth, int outgoingBandwidth);
    private static native long nativeCreateHostBound(String host, int port, int maxPeers, int maxChannels,
                                                    int incomingBandwidth, int outgoingBandwidth);
    private native long nativeConnect(long hostHandle, String host, int port, int channelCount, int data);
    private native ENetEvent nativeService(long hostHandle, int timeoutMillis);
    private native int nativeSend(long hostHandle, long peerHandle, byte channelID, byte[] data, int flags);
    private native void nativeBroadcast(long hostHandle, byte channelID, byte[] data, int flags);
    private native void nativeFlush(long hostHandle);
    private native void nativeDisconnectPeer(long hostHandle, long peerHandle, int data);
    private native void nativeDestroy(long hostHandle);
}

