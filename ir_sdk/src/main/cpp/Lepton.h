//
// Created by Kevin on 16/12/2.
//

#ifndef HYL026_LEPTON_H
#define HYL026_LEPTON_H

#include "mSocket.h"
#include "mImage.h"
#include "jni.h"
#include "aLog.h"


typedef enum _STATUS_ {
    STATUS_ERROR = -1,
    STATUS_CLOSED = 0,
    STATUS_OPENED = 1,
    STATUS_STOP = 2,
    STATUS_STARTING = 3,
    STATUS_RUNNING = 4
} STATUS;

typedef struct _CALLBACK_ {
    SOCKET client;
    JNIEnv *env;
    jobject jobject1;
    jmethodID callback;
} CALLBACK, *P_CALLBACK;

typedef enum _DEVICE_TYPE_ {
    DEVICE_80_60 = 0,
    DEVICE_324_256 = 1,
    DEVICE_640_512 = 2,
    DEVICE_336_256 = 3
} DEVICE_TYPE;

typedef struct _DEVICE_ {
    int img_width; //图像宽度
    int img_height; //图像高度
    int row_of_package; //每包行数
    int package_count_of_frame; //每帧包数
    int params_packet_of_frame; //每帧参数包数
    int max_packetId;//最大包ID (package_count_of_frame -params_packet_of_frame-1 )
    int img_raw_size; // (img_width * img_height * 2)
    int img_package_size; //(img_width * row_of_package * 2)
    int package_size;// (img_package_size + sizeof(ir_packet))
    int gray_size; //(img_width * img_height)
    int cache_count;
} DEVICE, *P_DEVICE;


typedef struct _HANDLE_ {
    int id;
    char name[100];
    char ip[100];
    int port;
    STATUS status;
    P_DEVICE p_device;
    DEVICE_TYPE device_type;
    SOCKET client;
    long lastTime;
    int tempFrame;//临时帧数
    float fps;
    float maxTemp;
    int max_x;
    int max_y;
    float minTemp;
    int min_x;
    int min_y;
    float envTemp;
    float sutTemp;
    int cachePoint;
    short isRun;
    int colorName;
    unsigned int frameId;
    unsigned char *raw_data_cache;
    short scalingData[50];
    float cof[50];
    float bbb[50];
    short fancha[(50 - 1) * 5];
    P_CALLBACK p_callback;
} HANDLE, *P_HANDLE;

P_HANDLE initHandle(int, DEVICE_TYPE);

P_DEVICE setDeviceType(DEVICE_TYPE);

void setScalingData(P_HANDLE handle, short scalingData[]);

void countCof(P_HANDLE handle);

void countFan(P_HANDLE handle);

void computxxx1(P_HANDLE handle, long computed, float *coffic, float *bbbbbb);

double temperature(P_HANDLE handle, short gray, short miss, double e, double s, double value);

void getMaxAndMin(P_HANDLE handle, short *grayData);

void pointNext(P_HANDLE handle);

void getNext(P_HANDLE handle, short *raw);

short* getNextShort(P_HANDLE handle);

void openCBThread(P_HANDLE handle);

void saveFrame(P_HANDLE handle, unsigned char *data);

void setName(P_HANDLE handle, const char *name);

void getName(P_HANDLE handle, const char *name);

void setIp(P_HANDLE handle, const char *ip);

void getIp(P_HANDLE handle, const char *ip);


#endif //HYL026_LEPTON_H
