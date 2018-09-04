//
// Created by Kevin on 16/12/2.
//

#include "Lepton.h"
#include "aLog.h"
#include "SocketManager.h"
#include "LeptonControl.h"

P_HANDLE initHandle(int id, DEVICE_TYPE device_type) {
    P_HANDLE handle;
    handle = malloc(sizeof(HANDLE));
    handle->id = id;
    handle->port = 50001;
    handle->status = STATUS_CLOSED;
    handle->device_type = device_type;
    handle->lastTime = 0;
    handle->tempFrame = 0;
    handle->fps = 0.0;
    handle->envTemp = 0.0;
    handle->sutTemp = 0.0;
    handle->cachePoint = 0;
    handle->frameId = 0;
    handle->isRun = false;
    handle->colorName = 0;
    handle->p_device = setDeviceType(device_type);
    handle->raw_data_cache = (unsigned char *) malloc(
            (size_t) (handle->p_device->img_raw_size * handle->p_device->cache_count));
    memset(handle->raw_data_cache, 0x00,
           (size_t) handle->p_device->img_raw_size * handle->p_device->cache_count);
    memset(handle->scalingData, 0x00, (size_t) 50);
    memset(handle->cof, 0x00, (size_t) 50);
    memset(handle->bbb, 0x00, (size_t) 50);
    memset(handle->fancha, 0x00, (size_t) ((50 - 1) * 5));
    addBMPImage_Info_Header(handle->p_device->img_width, handle->p_device->img_height);
    addFileHeader(54 + handle->p_device->img_width * handle->p_device->img_height);
    setColorsName(handle->colorName);
    handle->p_callback = malloc(sizeof(CALLBACK));
    return handle;
}

P_DEVICE setDeviceType(DEVICE_TYPE device_type) {
    P_DEVICE p_device = malloc(sizeof(DEVICE));
    int size = 0;
    if (device_type == DEVICE_80_60) {
        size = sizeof(IR_PACKET_80);
    } else {
        size = sizeof(IR_PACKET_ELSE);
    }
    switch (device_type) {
        case DEVICE_80_60:
            p_device->img_width = IMG_WIDTH_80_60;
            p_device->img_height = IMG_HEIGHT_80_60;
            p_device->row_of_package = ROW_OF_PACKAGE_80_60;
            p_device->package_count_of_frame = PACKAGE_COUNT_OF_FRAME_80_60;
            p_device->params_packet_of_frame = PARAMS_PACKET_OF_FRAME_80_60;
            break;
        case DEVICE_324_256:
            p_device->img_width = IMG_WIDTH_324_256;
            p_device->img_height = IMG_HEIGHT_324_256;
            p_device->row_of_package = ROW_OF_PACKAGE_324_256;
            p_device->package_count_of_frame = PACKAGE_COUNT_OF_FRAME_324_256;
            p_device->params_packet_of_frame = PARAMS_PACKET_OF_FRAME_324_256;
            break;
        case DEVICE_640_512:
            p_device->img_width = IMG_WIDTH_640_512;
            p_device->img_height = IMG_HEIGHT_640_512;
            p_device->row_of_package = ROW_OF_PACKAGE_640_512;
            p_device->package_count_of_frame = PACKAGE_COUNT_OF_FRAME_640_512;
            p_device->params_packet_of_frame = PARAMS_PACKET_OF_FRAME_640_512;
            break;
        case DEVICE_336_256:
            p_device->img_width = IMG_WIDTH_336_256;
            p_device->img_height = IMG_HEIGHT_336_256;
            p_device->row_of_package = ROW_OF_PACKAGE_336_256;
            p_device->package_count_of_frame = PACKAGE_COUNT_OF_FRAME_336_256;
            p_device->params_packet_of_frame = PARAMS_PACKET_OF_FRAME_336_256;
            break;
    }
    p_device->max_packetId =
            p_device->package_count_of_frame - 1 - p_device->params_packet_of_frame;
    p_device->img_raw_size =
            p_device->img_width * p_device->img_height * sizeof(short);
    p_device->img_package_size =
            p_device->img_width * p_device->row_of_package * sizeof(short);
    p_device->package_size =
            p_device->img_package_size + size;
    p_device->gray_size =
            p_device->img_width * p_device->img_height;
    p_device->cache_count = 5;
    return p_device;
}

void setScalingData(P_HANDLE handle, short scalingData[]) {
    memcpy(handle->scalingData, scalingData, 50 * sizeof(short));
    countCof(handle);
    countFan(handle);
}

void countCof(P_HANDLE handle) {
    short i;
    int length = 50;
    for (i = 0; i < length - 1; i++) {
        handle->cof[i] = (float) (1000 / (handle->scalingData[i + 1] - handle->scalingData[i]) *
                                  0.001);
        handle->bbb[i] = (float) (1 * i - handle->cof[i] * handle->scalingData[i]);
    }
}

void countFan(P_HANDLE handle) {
    int i = 0;
    int length = 50;
    for (i = 0; i < length - 1; i++) {
        handle->fancha[i] = (short) ((-handle->bbb[i] + i * 1) / handle->cof[i]);
    }
}

void computxxx1(P_HANDLE handle, long computed, float *coffic, float *bbbbbb) {
    short i;
    int length = 50;
    for (i = 0; i < length - 1; i++) {
        if ((computed >= handle->scalingData[i]) && (computed <= handle->scalingData[i + 1])) {
            *coffic = handle->cof[i];
            *bbbbbb = handle->bbb[i];
            break;
        } else if (computed < handle->scalingData[0]) {
            *coffic = handle->cof[0];
            *bbbbbb = handle->bbb[0];
            break;
        } else if (computed > handle->scalingData[length - 1]) {
            *coffic = handle->cof[length - 2];
            *bbbbbb = handle->bbb[length - 2];
            break;
        }
    }
}

double temperature(P_HANDLE handle, short gray, short miss, double e, double s, double value) {
    double o2 = handle->sutTemp - s;
    double off;
    off = o2;
    double tempra;// 返回的实际温度
    gray += off * 2;
    float coffic, bbbbbb;
    computxxx1(handle, gray, &coffic, &bbbbbb);// 计算系数coffic和bbbbbb
    tempra = (gray * coffic + bbbbbb) + value;
    return tempra;
}

void pointNext(P_HANDLE handle) {
    handle->cachePoint++;
    if (handle->cachePoint >= handle->p_device->cache_count) {
        handle->cachePoint = 0;
    }
}

void openCBThread(P_HANDLE handle) {
    while (handle->isRun) {
        if (handle->status != STATUS_RUNNING) {
            handle->status = STATUS_RUNNING;
        }
        short raw[handle->p_device->gray_size];
        getNextFrame(handle, raw);
        jshort *by = (jshort *) raw;
        jbyteArray jarray = (*handle->p_callback->env)->NewShortArray(handle->p_callback->env,
                                                                      handle->p_device->gray_size);
        (*handle->p_callback->env)->SetShortArrayRegion(handle->p_callback->env, jarray, 0,
                                                        handle->p_device->gray_size, by);
        (*handle->p_callback->env)->CallVoidMethod(handle->p_callback->jobject1,
                                                   handle->p_callback->callback, jarray,
                                                   handle->p_device->img_width,
                                                   handle->p_device->img_height,
                                                   handle->p_device->gray_size);
    }
}

void getNext(P_HANDLE handle, short *raw) {
    int temp = handle->cachePoint - 1;
    if (temp < 0) {
        temp = handle->p_device->cache_count - 1;
    }
    short *tmp = (short *) (handle->raw_data_cache + handle->p_device->img_raw_size * temp);
    memcpy(raw, tmp, (size_t) handle->p_device->img_raw_size);

    getMaxAndMin(handle, tmp);
}

short* getNextShort(P_HANDLE handle) {
    int temp = handle->cachePoint - 1;
    if (temp < 0) {
        temp = handle->p_device->cache_count - 1;
    }
    short *tmp = (short *) (handle->raw_data_cache + handle->p_device->img_raw_size * temp);
//    memcpy(raw, tmp, (size_t) handle->p_device->img_raw_size);

    getMaxAndMin(handle, tmp);
    return tmp;
}

void getMaxAndMin(P_HANDLE handle, short *grayData) {
    int max = -99999;
    int min = 99999;
    int value = 0, i = 0, j = 0;
    for (i = 4; i < handle->p_device->img_height - 4; i++) {
        for (j = 4; j < handle->p_device->img_width - 4; j++) {
            int pos = i * handle->p_device->img_width + j;
            value = grayData[pos];
            if (max < value) {
                max = value;
                handle->max_x = j;
                handle->max_y = i;

            }
            if (min > value) {
                min = value;
                handle->min_x = j;
                handle->min_y = i;
            }
        }
    }
    handle->minTemp = (float) (min * 0.04 - 273.15);
    handle->maxTemp = (float) (max * 0.04 - 273.15);

}


void saveFrame(P_HANDLE handle, unsigned char *data) {
    memcpy(handle->raw_data_cache + handle->p_device->img_raw_size * handle->cachePoint, data,
           (size_t) handle->p_device->img_raw_size);
    pointNext(handle);
}

void setName(P_HANDLE handle, const char *name) {
    strcpy(handle->name, name);
}

void getName(P_HANDLE handle, const char *name) {
    strcpy((char *) name, handle->name);
}

void setIp(P_HANDLE handle, const char *ip) {
    strcpy(handle->ip, ip);
}

void getIp(P_HANDLE handle, const char *ip) {
    strcpy((char *) ip, handle->ip);
}