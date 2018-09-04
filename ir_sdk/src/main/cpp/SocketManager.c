//
// Created by Kevin on 16/12/2.
//

#include "SocketManager.h"
#include "ErrorTable.h"
#include <unistd.h>
#include <sys/time.h>

NET_CONFIG mDevs[MAX_MCARD_NUM_HANDLE];
int mDevsNum = 0;
int isSearching = 0;

void initMsg(PMESSAGE_STRUCT pMsg) {
    if (pMsg != NULL) {
        pMsg->type = MESSAGE_TYPE_NONE;
        pMsg->dir = MESSAGE_DIRECTION_TO_TARGET;
        pMsg->datLen = 0;
        memset(pMsg->resv, 0, MAX_RESERVED_LEN);
    }
}

void callback(JNIEnv *env, jclass obj, char *className, char *methodName, char *methodSign,
              char *string) {
    //在c代码里面调用java代码里面的方法
    // java 反射
    //1 . 找到java代码的 class文件
    //    jclass      (*FindClass)(JNIEnv*, const char*);
    jclass dpclazz = (*env)->FindClass(env, className);
    if (dpclazz == 0) {
        LOGI("find class error");
        return;
    }
    LOGI("find class ");

    //2 寻找class里面的方法
    //   jmethodID   (*GetMethodID)(JNIEnv*, jclass, const char*, const char*);
    jmethodID method1 = (*env)->GetMethodID(env, dpclazz, methodName, methodSign);
    if (method1 == 0) {
        LOGI("find method1 error");
        return;
    }
    LOGI("find method1 ");
    //3 .调用这个方法
    //    void        (*CallVoidMethod)(JNIEnv*, jobject, jmethodID, ...);
    (*env)->CallObjectMethod(env, obj, method1, string);
}

void processSearchMessage(P_CALLBACK p_callback, char *buf, SOCKADDR_IN addr, int datalen) {
    char *pData = buf + sizeof(MESSAGE_STRUCT);
    PNET_CONFIG pNetCfg = (PNET_CONFIG) pData;
    int bExistAlready = -1;

    //Check exist already
    for (int i = 0; i < mDevsNum; i++) {
        if (memcmp(mDevs[i].mac, pNetCfg->mac, MAX_MAC_STR_LEN) == 0) {
            bExistAlready = i;
            break;
        }
    }

    //If exist, update config
    if (bExistAlready != -1)
        memcpy(&mDevs[bExistAlready], pNetCfg, sizeof(NET_CONFIG));
    else //If not, Add the record entry
    {
        //Find index ava
        if (mDevsNum < MAX_MCARD_NUM_HANDLE) {
            memcpy(&mDevs[mDevsNum], pNetCfg, sizeof(NET_CONFIG));
            jbyte *by = (jbyte *) pNetCfg->ip;
            jbyteArray jarray = (*p_callback->env)->NewByteArray(p_callback->env,
                                                                 sizeof(pNetCfg->ip));
            (*p_callback->env)->SetByteArrayRegion(p_callback->env, jarray, 0, sizeof(pNetCfg->ip),
                                                   by);
            (*p_callback->env)->CallVoidMethod(p_callback->jobject1, p_callback->callback, jarray);
            mDevsNum++;
        }
    }
}

void openSearchReceiver(P_CALLBACK p_callback) {
    isSearching = 1;
    SOCKADDR_IN sin;
    int receiverLen = sizeof(SOCKADDR);
    int lenght = sizeof(MESSAGE_STRUCT) + sizeof(NET_CONFIG);
    while (isSearching) {
        unsigned char receiverBuf[lenght];
        int ret = (int) recvfrom(p_callback->client, receiverBuf, (size_t) lenght, 0,
                                 (SOCKADDR *) &sin, &receiverLen);
        if (ret < 0) {
            usleep(10);
        } else {
            LOGD("searc", "search receive");
            PMESSAGE_STRUCT pmessage_struct = (PMESSAGE_STRUCT) receiverBuf;
            switch (pmessage_struct->type) {
                case MESSAGE_TYPE_SEARCH:
                    processSearchMessage(p_callback, (char *) receiverBuf, sin, ret);
                    break;
                case MESSAGE_TYPE_VIDEO_OPEN:
                case MESSAGE_TYPE_VIDEO_CLOSE:
                case MESSAGE_TYPE_SERIAL_CMD:
                    break;
                default:
                    break;
            }
        }
    }
    LOGE("search socket receive is closed");
}

void countFps(P_HANDLE handle) {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    long time = tv.tv_sec * 1000 + tv.tv_usec / 1000;
    handle->tempFrame++;
    if (handle->lastTime > 0) {
        long off = time - handle->lastTime;
        if (off >= 1000) {
            handle->fps = (float) handle->tempFrame / off * 1000;
            handle->fps = (float) (((int) (handle->fps * 100 + 0.5)) / 100.0);
            handle->tempFrame = 0;
        } else {
            return;
        }
    }
    handle->lastTime = time;
}

int flag = 1;

void openReceiver(P_HANDLE handle) {
    SOCKADDR_IN sin;
    unsigned char rawData[handle->p_device->img_raw_size];
    int receiverLen = sizeof(SOCKADDR);
    if (handle->status != STATUS_OPENED) {
        handle->status = STATUS_OPENED;
    }
    int f = flag++;
    int count = 0;
    while (handle->isRun) {
        unsigned char receiverBuf[handle->p_device->package_size];
        int ret = (int) recvfrom(handle->client, receiverBuf,
                                 (size_t) handle->p_device->package_size,
                                 0, (SOCKADDR *) &sin, &receiverLen);
        if (ret < 0) {
            usleep(10);
        } else {
//            LOGE("socket %d", f);
            if (handle->status != STATUS_RUNNING) {
                handle->status = STATUS_RUNNING;
            }
            IR_PACKET packet;
            memset(&packet, 0x00, sizeof(IR_PACKET));
            int size = 0;
            if (handle->device_type == DEVICE_80_60) {
                PIR_PACKET_80 packet1 = (PIR_PACKET_80) receiverBuf;
                packet.ver = packet1->ver;
                packet.temp1 = packet1->temp1;
                packet.temp2 = packet1->temp2;
                packet.res = packet1->res;
                packet.frameID = packet1->frameID;
                packet.dataLen = packet1->dataLen;
                packet.dataFol = packet1->dataFol;
                packet.packetID = packet1->packetID;
                packet.res1 = NULL;
                size = sizeof(IR_PACKET_80);
            } else {
                PIR_PACKET_ELSE packet2 = (PIR_PACKET_ELSE) receiverBuf;
                packet.ver = packet2->ver;
                packet.temp1 = packet2->temp1;
                packet.temp2 = packet2->temp2;
                packet.res = packet2->res;
                packet.frameID = packet2->frameID;
                packet.dataLen = packet2->dataLen;
                packet.dataFol = packet2->dataFol;
                packet.packetID = packet2->packetID;
                packet.res1 = packet2->res1;
                size = sizeof(IR_PACKET_ELSE);
            }
            int id = packet.packetID;
            int maxId = handle->p_device->max_packetId;
            if (id == 0) {
                count = 0;
            } else {
                count++;
            }
            handle->sutTemp = (float) atof((const char *) packet.temp1);
            handle->envTemp = (float) atof((const char *) packet.temp2);
            handle->frameId = packet.frameID;
            memcpy(rawData + packet.dataLen * id, receiverBuf + size, packet.dataLen);
            if (id == maxId && count == maxId) {
                saveFrame(handle, rawData);
                countFps(handle);
                continue;
            }
        }
    }
    LOGE("socket receive is closed");
}

int sendOpenCmd(P_HANDLE handle) {
    int result;
    if (handle->device_type == DEVICE_80_60) {
        LOGE("sendOpenCmd DEVICE_80_60");
        result = sendMessage(handle, OPEN, strlen(OPEN));
    } else {
        LOGE("sendOpenCmd DEVICE_else");
        unsigned char sendBuf[sizeof(MESSAGE_STRUCT) + sizeof(VIDEO_TRANS_DEF)] = {0};
        MESSAGE_STRUCT msg;
        VIDEO_TRANS_DEF vCfg;
        //Message Construct
        initMsg(&msg);
        msg.type = MESSAGE_TYPE_VIDEO_OPEN;
        msg.datLen = sizeof(VIDEO_TRANS_DEF);
        SOCKADDR_IN sockAddr;
        int iLen = sizeof(SOCKADDR_IN);
        getsockname(handle->client, (struct sockaddr *) &sockAddr, &iLen);

        vCfg.port = ntohs(sockAddr.sin_port);
        LOGE("vc port %d", vCfg.port);
        vCfg.mode = 0;
        if (&sendBuf == NULL) {
            return -1;
        }
        memcpy(&sendBuf[0], &msg, sizeof(MESSAGE_STRUCT));
        memcpy(&sendBuf[sizeof(MESSAGE_STRUCT)], &vCfg, sizeof(VIDEO_TRANS_DEF));
        result = sendMessage(handle, (char *) sendBuf, sizeof(sendBuf));
    }
    if (result >= 0 && handle->status < STATUS_STARTING) {
        handle->status = STATUS_STARTING;
    }
    return result;
}

int sendCloseCmd(P_HANDLE handle) {
//    LOGE("sendCloseCmd");
    int result;
    if (handle->device_type == DEVICE_80_60) {
        result = sendMessage(handle, CLOSE, strlen(CLOSE));
    } else {
        unsigned char sendBuf[sizeof(MESSAGE_STRUCT)] = {0};
        MESSAGE_STRUCT msg;
        initMsg(&msg);
        msg.type = MESSAGE_TYPE_VIDEO_CLOSE;
        msg.datLen = sizeof(MESSAGE_STRUCT);
        if (&sendBuf == NULL) {
            return -1;
        }
        memcpy(&sendBuf[0], &msg, sizeof(MESSAGE_STRUCT));
        result = sendMessage(handle, (char *) sendBuf, sizeof(sendBuf));
    }
    if (result >= 0) {
        if (handle->status > STATUS_STOP) {
            handle->status = STATUS_STOP;
        }
    }
    return result;
}

int sendSearchCmd(SOCKET client) {
    int result;
    unsigned char sendBuf[sizeof(MESSAGE_STRUCT)] = {0};
    MESSAGE_STRUCT msg;
    initMsg(&msg);
    msg.type = MESSAGE_TYPE_SEARCH;
    msg.datLen = sizeof(MESSAGE_STRUCT);
    if (&sendBuf == NULL) {
        return -1;
    }
    memcpy(&sendBuf[0], &msg, sizeof(MESSAGE_STRUCT));

    SOCKADDR_IN sin;
    sin.sin_family = AF_INET;
    sin.sin_port = htons(50001);
//    sin.sin_addr.s_addr = INADDR_BROADCAST;
    sin.sin_addr.s_addr = inet_addr("192.168.255.255");
    result = (int) sendto(client, (const void *) sendBuf, (size_t) sizeof(sendBuf), 0,
                          (const struct sockaddr *) &sin, sizeof(SOCKADDR));
    if (result < 0) {
        LOGE("send sendSearchCmd broadcast fail", sendBuf);
        return -1;
    } else {
        LOGD("send sendSearchCmd broadcast successful ,length = %d", result);
    }
    return result;
}

int sendCorrect(P_HANDLE handle) {
    int result;
    if (handle->device_type == DEVICE_80_60) {
        result = sendMessage(handle, CORRECT, strlen(CORRECT));
    } else {
        unsigned char sendBuf[sizeof(MESSAGE_STRUCT) + sizeof(SERIAL_DATA)] = {0};
        MESSAGE_STRUCT msg;
        SERIAL_DATA serData;

        unsigned char cmd[16] = {0x0C, 0x00, 0x00, 0x00, 0x00,
                                 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                 0x00, 0x00, 0x00, 0x00, 0x00};

        initMsg(&msg);
        msg.type = MESSAGE_TYPE_SERIAL_CMD;
        msg.datLen = sizeof(SERIAL_DATA);
        memcpy(serData.serDat, (cmd), msg.datLen);
        if (&sendBuf == NULL) {
            return -1;
        }
        memcpy(&sendBuf[0], &msg, sizeof(MESSAGE_STRUCT));
        memcpy(&sendBuf[sizeof(MESSAGE_STRUCT)], &serData, sizeof(SERIAL_DATA));
        result = sendMessage(handle, (char *) sendBuf, sizeof(sendBuf));
    }
    return result;
}

int sendMessage(P_HANDLE handle, char *message, int length) {
    LOGE("sendMessage %x  %d", message, length);
    for (int i = 0; i < length; ++i) {
        LOGE("i = %d   %x", i, message[i]);
    }
    SOCKADDR_IN sin;
    sin.sin_family = AF_INET;
    sin.sin_port = htons(handle->port);
    sin.sin_addr.s_addr = inet_addr(handle->ip);
    int result = (int) sendto(handle->client, (const void *) message, (size_t) length, 0,
                              (const struct sockaddr *) &sin, sizeof(SOCKADDR));
    if (result < 0) {
        LOGE("%s %d send %x fail", handle->ip, handle->port, message);
        return SOCKET_SEND_FAIL;
    } else {
        LOGD("%s  %d send %x successful", handle->ip, handle->port, message);
    }
    return result;
}
