//
// Created by Kevin on 16/12/12.
//

#ifndef HYL026_LEPTONCONTROL_H
#define HYL026_LEPTONCONTROL_H

#include "SocketManager.h"
#include "ColorTable.h"
#include "ErrorTable.h"

#define  FIXED_BITS           16
#define  FIXED_ONE            (1 << FIXED_BITS)
#define  PALETTE_BITS   8
#define  PALETTE_SIZE   256//(1 << PALETTE_BITS)
#define  FIXED_FRAC(x)        ((x) & ((1 << FIXED_BITS)-1))

#define PALETTE(head)  (head.biClrUsed * sizeof(RGBQUAD))
#define DIB_SIZE(head)          (head.biSize + PALETTE(head)+ head.biSizeImage)


typedef int INT32;
typedef short INT16;
typedef int DWORD;

#pragma pack(push, 1)
typedef struct BITMAP_INFO_HEADER_s {
    INT32 biSize;
    INT32 biWidth;
    INT32 biHeight;
    INT16 biPlanes;
    INT16 biBitCount;
    INT32 biCompression;
    INT32 biSizeImage;
    INT32 biXPelsPerMeter;
    INT32 biYPelsPerMeter;
    INT32 biClrUsed;
    INT32 biClrImportant;
} BITMAP_INFO_HEADER, *PBITMAP_INFO_HEADER;

typedef struct IRRGBQUAD_t {
    char rgbBlue;
    char rgbGreen;
    char rgbRed;
    char rgbReserved;
}RGBQUAD;

typedef struct BITMAP_INFO_s {
    BITMAP_INFO_HEADER bmHeader;
    RGBQUAD bmColors[1];
} BITMAP_INFO, *PBITMAP_INFO;

typedef struct BITMAP_FILE_HEADER_s {
    INT16 bfType;
    DWORD bfSize;
    INT16 bfReserved1;
    INT16 bfReserved2;
    DWORD bfOffBits;
} BITMAP_FILE_HEADER, *PBITMAP_FILE_HEADER;
#pragma pack(pop)

int init();

int search(JNIEnv *env, jobject obj, jmethodID callback);

long openDevice(const char *ip, int port, DEVICE_TYPE deviceType);

long openDeviceByCB(const char *ip, int port, DEVICE_TYPE deviceType,JNIEnv *env, jobject obj, jmethodID callback);

int openThread(P_HANDLE handle);

void setScalingDataByHandle(P_HANDLE handle, short scalingData[]);

double temperatureByGray(P_HANDLE handle, short gray, double e, double s, double value);

//void createConnectListener();

P_HANDLE getPointerByIndex(int index);
//
//P_HANDLE  getPointerByHandle(int handle);

int startDevice(P_HANDLE handle);

int pauseDevice(P_HANDLE handle);

int closeDevice(P_HANDLE handle);

int correctDevice(P_HANDLE handle);

int _14To8(P_HANDLE,short *data, unsigned char *bmpData);

int _14To565(P_HANDLE,short *data, int* bmpData);

int _14To565Toshort(P_HANDLE,short *data, short* bmpData);

void getNextFrame(P_HANDLE handle, short *raw);

void setColorsName(int colorsName);

void setColorName(int colorName);

int getColorName(void);

void addFileHeader(int size);

void addBMPImage_Info_Header(int w, int h);

void init_palette(int index);

int byteArrayToShortArray(unsigned char *src, short *dst,int lenght);

int getNextBmp(P_HANDLE handle,unsigned char *RgbData);


#endif //HYL026_LEPTONCONTROL_H
