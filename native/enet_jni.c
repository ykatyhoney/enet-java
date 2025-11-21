// Fix for MinGW/GCC: define __int64 before including jni.h
// JNI headers expect __int64 which is MSVC-specific, but GCC/MinGW uses long long
#if (defined(__GNUC__) || defined(__MINGW32__)) && !defined(_MSC_VER)
#ifndef __int64
#define __int64 long long
#endif
#endif

#include <jni.h>
#include <string.h>
#include <stdlib.h>

#define ENET_IMPLEMENTATION
#include "enet/include/enet.h"

#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#else
#include <arpa/inet.h>
#include <netinet/in.h>
#endif

// Global reference to ENetEvent class
static jclass eventClass = NULL;
static jclass peerClass = NULL;
static jclass addressClass = NULL;
static jclass packetClass = NULL;
static jmethodID eventConstructor = NULL;
static jmethodID peerConstructor = NULL;
static jmethodID addressConstructor = NULL;
static jmethodID packetConstructor = NULL;

// Helper function to convert ENetAddress to Java ENetAddress
jobject createJavaAddress(JNIEnv *env, ENetAddress *address) {
#ifndef INET6_ADDRSTRLEN
#define INET6_ADDRSTRLEN 46
#endif
    char hostStr[INET6_ADDRSTRLEN];
    hostStr[0] = '\0';
    
    // Try IPv6 first, then IPv4
    if (inet_ntop(AF_INET6, &address->host, hostStr, INET6_ADDRSTRLEN) == NULL) {
        // Fallback: try to extract IPv4 from IPv6-mapped address
        struct in_addr ipv4;
        memcpy(&ipv4, ((char*)&address->host) + 12, 4);
        if (inet_ntop(AF_INET, &ipv4, hostStr, INET6_ADDRSTRLEN) == NULL) {
            strcpy(hostStr, "0.0.0.0");
        }
    }
    
    // If still empty, use default
    if (hostStr[0] == '\0') {
        strcpy(hostStr, "0.0.0.0");
    }
    
    jstring host = (*env)->NewStringUTF(env, hostStr);
    jint port = address->port;
    
    return (*env)->NewObject(env, addressClass, addressConstructor, host, port);
}

// Helper function to create Java ENetPeer
jobject createJavaPeer(JNIEnv *env, ENetPeer *peer) {
    jobject address = createJavaAddress(env, &peer->address);
    jlong handle = (jlong)(intptr_t)peer;
    
    return (*env)->NewObject(env, peerClass, peerConstructor, handle, address);
}

// Helper function to create Java ENetPacket
jobject createJavaPacket(JNIEnv *env, ENetPacket *packet) {
    jbyteArray data = (*env)->NewByteArray(env, packet->dataLength);
    (*env)->SetByteArrayRegion(env, data, 0, packet->dataLength, (jbyte*)packet->data);
    
    jlong handle = (jlong)(intptr_t)packet;
    jint flags = packet->flags;
    
    return (*env)->NewObject(env, packetClass, packetConstructor, handle, data, flags);
}

// Helper function to create Java ENetEvent
jobject createJavaEvent(JNIEnv *env, ENetEvent *event) {
    jobject peer = NULL;
    jobject packet = NULL;
    
    if (event->peer) {
        peer = createJavaPeer(env, event->peer);
    }
    
    if (event->packet) {
        packet = createJavaPacket(env, event->packet);
    }
    
    return (*env)->NewObject(env, eventClass, eventConstructor,
        (jint)event->type,
        peer,
        (jbyte)event->channelID,
        (jint)event->data,
        packet
    );
}

JNIEXPORT jint JNICALL
Java_com_enet_ENetConnection_nativeInitialize(JNIEnv *env, jclass clazz) {
    return enet_initialize();
}

JNIEXPORT void JNICALL
Java_com_enet_ENetConnection_nativeDeinitialize(JNIEnv *env, jclass clazz) {
    enet_deinitialize();
}

JNIEXPORT jlong JNICALL
Java_com_enet_ENetConnection_nativeCreateHost(JNIEnv *env, jclass clazz,
    jstring host, jint port, jint maxClients, jint maxChannels,
    jint incomingBandwidth, jint outgoingBandwidth) {
    
    ENetAddress address = {0};
    
    if (host != NULL) {
        const char *hostStr = (*env)->GetStringUTFChars(env, host, NULL);
        if (hostStr != NULL) {
            if (strcmp(hostStr, "0.0.0.0") == 0 || strcmp(hostStr, "::") == 0 || strcmp(hostStr, "") == 0) {
                address.host = in6addr_any;
            } else {
                if (enet_address_set_host(&address, hostStr) < 0) {
                    (*env)->ReleaseStringUTFChars(env, host, hostStr);
                    return 0; // Failed to set host
                }
            }
            (*env)->ReleaseStringUTFChars(env, host, hostStr);
        }
    } else {
        address.host = in6addr_any;
    }
    
    address.port = port;
    
    ENetHost *hostPtr = enet_host_create(&address, maxClients, maxChannels,
        incomingBandwidth, outgoingBandwidth);
    
    return (jlong)(intptr_t)hostPtr;
}

JNIEXPORT jlong JNICALL
Java_com_enet_ENetConnection_nativeCreateHostBound(JNIEnv *env, jclass clazz,
    jstring host, jint port, jint maxPeers, jint maxChannels,
    jint incomingBandwidth, jint outgoingBandwidth) {
    
    ENetAddress address = {0};
    
    if (host != NULL && port != 0) {
        const char *hostStr = (*env)->GetStringUTFChars(env, host, NULL);
        if (strcmp(hostStr, "0.0.0.0") == 0 || strcmp(hostStr, "::") == 0 || strcmp(hostStr, "") == 0) {
            address.host = in6addr_any;
        } else {
            enet_address_set_host(&address, hostStr);
        }
        (*env)->ReleaseStringUTFChars(env, host, hostStr);
        address.port = port;
    } else {
        address.host = in6addr_any;
        address.port = 0;
    }
    
    ENetHost *hostPtr = enet_host_create(&address, maxPeers, maxChannels,
        incomingBandwidth, outgoingBandwidth);
    
    return (jlong)(intptr_t)hostPtr;
}

JNIEXPORT jlong JNICALL
Java_com_enet_ENetConnection_nativeConnect(JNIEnv *env, jobject thiz,
    jlong hostHandle, jstring host, jint port, jint channelCount, jint data) {
    
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    ENetAddress address = {0};
    
    const char *hostStr = (*env)->GetStringUTFChars(env, host, NULL);
    enet_address_set_host(&address, hostStr);
    (*env)->ReleaseStringUTFChars(env, host, hostStr);
    
    address.port = port;
    
    ENetPeer *peer = enet_host_connect(hostPtr, &address, channelCount, data);
    
    return (jlong)(intptr_t)peer;
}

JNIEXPORT jobject JNICALL
Java_com_enet_ENetConnection_nativeService(JNIEnv *env, jobject thiz,
    jlong hostHandle, jint timeoutMillis) {
    
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    ENetEvent event;
    
    int result = enet_host_service(hostPtr, &event, timeoutMillis);
    
    if (result > 0) {
        return createJavaEvent(env, &event);
    }
    
    return NULL;
}

JNIEXPORT jint JNICALL
Java_com_enet_ENetConnection_nativeSend(JNIEnv *env, jobject thiz,
    jlong hostHandle, jlong peerHandle, jbyte channelID, jbyteArray data, jint flags) {
    
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    ENetPeer *peer = (ENetPeer*)(intptr_t)peerHandle;
    
    jsize len = (*env)->GetArrayLength(env, data);
    jbyte *dataPtr = (*env)->GetByteArrayElements(env, data, NULL);
    
    ENetPacket *packet = enet_packet_create(dataPtr, len, flags);
    int result = enet_peer_send(peer, channelID, packet);
    
    (*env)->ReleaseByteArrayElements(env, data, dataPtr, JNI_ABORT);
    
    if (result < 0) {
        enet_packet_destroy(packet);
        return -1;
    }
    
    return 0;
}

JNIEXPORT void JNICALL
Java_com_enet_ENetConnection_nativeBroadcast(JNIEnv *env, jobject thiz,
    jlong hostHandle, jbyte channelID, jbyteArray data, jint flags) {
    
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    
    jsize len = (*env)->GetArrayLength(env, data);
    jbyte *dataPtr = (*env)->GetByteArrayElements(env, data, NULL);
    
    ENetPacket *packet = enet_packet_create(dataPtr, len, flags);
    enet_host_broadcast(hostPtr, channelID, packet);
    
    (*env)->ReleaseByteArrayElements(env, data, dataPtr, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_enet_ENetConnection_nativeFlush(JNIEnv *env, jobject thiz, jlong hostHandle) {
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    enet_host_flush(hostPtr);
}

JNIEXPORT void JNICALL
Java_com_enet_ENetConnection_nativeDisconnectPeer(JNIEnv *env, jobject thiz,
    jlong hostHandle, jlong peerHandle, jint data) {
    
    ENetPeer *peer = (ENetPeer*)(intptr_t)peerHandle;
    enet_peer_disconnect(peer, data);
}

JNIEXPORT void JNICALL
Java_com_enet_ENetConnection_nativeDestroy(JNIEnv *env, jobject thiz, jlong hostHandle) {
    ENetHost *hostPtr = (ENetHost*)(intptr_t)hostHandle;
    if (hostPtr) {
        enet_host_destroy(hostPtr);
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    
    // Cache class references
    eventClass = (*env)->FindClass(env, "com/enet/ENetEvent");
    peerClass = (*env)->FindClass(env, "com/enet/ENetPeer");
    addressClass = (*env)->FindClass(env, "com/enet/ENetAddress");
    packetClass = (*env)->FindClass(env, "com/enet/ENetPacket");
    
    if (!eventClass || !peerClass || !addressClass || !packetClass) {
        return JNI_ERR;
    }
    
    eventClass = (jclass)(*env)->NewGlobalRef(env, eventClass);
    peerClass = (jclass)(*env)->NewGlobalRef(env, peerClass);
    addressClass = (jclass)(*env)->NewGlobalRef(env, addressClass);
    packetClass = (jclass)(*env)->NewGlobalRef(env, packetClass);
    
    // Cache constructor method IDs
    jclass eventClassLocal = (*env)->FindClass(env, "com/enet/ENetEvent");
    eventConstructor = (*env)->GetMethodID(env, eventClassLocal, "<init>", "(ILcom/enet/ENetPeer;BILcom/enet/ENetPacket;)V");
    
    jclass peerClassLocal = (*env)->FindClass(env, "com/enet/ENetPeer");
    peerConstructor = (*env)->GetMethodID(env, peerClassLocal, "<init>", "(JLcom/enet/ENetAddress;)V");
    
    jclass addressClassLocal = (*env)->FindClass(env, "com/enet/ENetAddress");
    addressConstructor = (*env)->GetMethodID(env, addressClassLocal, "<init>", "(Ljava/lang/String;I)V");
    
    jclass packetClassLocal = (*env)->FindClass(env, "com/enet/ENetPacket");
    packetConstructor = (*env)->GetMethodID(env, packetClassLocal, "<init>", "(J[BI)V");
    
    if (!eventConstructor || !peerConstructor || !addressConstructor || !packetConstructor) {
        return JNI_ERR;
    }
    
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    
    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {
        return;
    }
    
    if (eventClass) (*env)->DeleteGlobalRef(env, eventClass);
    if (peerClass) (*env)->DeleteGlobalRef(env, peerClass);
    if (addressClass) (*env)->DeleteGlobalRef(env, addressClass);
    if (packetClass) (*env)->DeleteGlobalRef(env, packetClass);
}

