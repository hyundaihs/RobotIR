//
// Created by Kevin on 16/12/2.
//
#include "LeptonControl.h"
#include "ThreadPool.h"
#include <fcntl.h>
#include <unistd.h>

#define MAX_DEVICE_COUNT 4

#define HIST_DEFAULT                        65536
#define AD_OFFSET_VALUE                    32768
#define DEFAULT_K_THRESHOLD                1.8
#define AD_LIMIT_VALUE                    255
#define POINT_CNT(l)                        (l * ((5 * 1.0) / 100))


float g_CBuff[10] = {0};
float g_BBuff[10] = {0};
int g_BCPT = 0;

int colorName = 9;

BITMAP_INFO_HEADER header;
BITMAP_FILE_HEADER file_header;
RGBQUAD rgb[256];

long objs[MAX_DEVICE_COUNT] = {-1, -1, -1, -1};

int index_id = 0;

int init() {
    if (t_pool_create(5) != 0) {
        LOGE("t_pool_create failed\n");
        exit(1);
    }
    return SUCCESSFUL;
}

//设置非阻塞
static void setnonblocking(int sockfd) {
    int flag = fcntl(sockfd, F_GETFL, 0);
    if (flag < 0) {
        LOGE("fcntl F_GETFL fail");
        return;
    }
    if (fcntl(sockfd, F_SETFL, flag | O_NONBLOCK) < 0) {
        LOGE("fcntl F_SETFL fail");
    }
}

int search(JNIEnv *env, jobject obj, jmethodID callback) {
    LOGE("search");
    SOCKET client = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (client < 0) {
        LOGE("search socket openDevice fail");
        return -1;
    } else {
        LOGE("search socket openDevice succ");
    }
    setnonblocking(client);
    SOCKADDR_IN sin;
    sin.sin_family = AF_INET;
    sin.sin_port = htons(60001);
//    sin.sin_addr.s_addr = INADDR_ANY;
//    sin.sin_addr.s_addr = inet_addr("192.168.255.255");
    if (bind(client, (SOCKADDR *) &sin, sizeof(SOCKADDR)) == -1) {
        LOGE("search socket bind fail");
        return -1;
    } else {
        LOGE("search socket bind succ");
    }
    int nRecvBuf = 10 * 1024 * 1024;
    if (setsockopt(client, SOL_SOCKET, SO_RCVBUF, (const char *) &nRecvBuf,
                   sizeof(int)) < 0) {
        shutdown(client, 2);
        LOGE("search setsockopt receiv buffer error");
        return -1;
    }
    P_CALLBACK p_callback;
    p_callback = malloc(sizeof(CALLBACK));
    p_callback->client = client;
    p_callback->env = env;
    p_callback->jobject1 = obj;
    p_callback->callback = callback;
    int result = t_pool_add_work((void *) openSearchReceiver, (P_CALLBACK) p_callback);
    if (result == -1) {
        LOGE("search thread receiver create faild");
        return -1;
    } else {
        LOGE("search thread receiver create succ");
        return sendSearchCmd(client);
    }
}

long openDevice(const char *ip, int port, DEVICE_TYPE device_type) {
    SOCKET client = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (client < 0) {
        LOGE("socket openDevice fail");
        return -1;
    }
    setnonblocking(client);
    SOCKADDR_IN sin;
    sin.sin_family = AF_INET;
    sin.sin_port = htons(0);
    sin.sin_addr.s_addr = INADDR_ANY;
    if (bind(client, (SOCKADDR *) &sin, sizeof(SOCKADDR)) == -1) {
        LOGE("socket bind fail");
        return -1;
    }
    int nRecvBuf = 10 * 1024 * 1024;
    if (setsockopt(client, SOL_SOCKET, SO_RCVBUF, (const char *) &nRecvBuf,
                   sizeof(int)) < 0) {
        shutdown(client, 2);
        LOGE("setsockopt receiv buffer error");
        return -1;
    }
    P_HANDLE handle = initHandle(index_id, device_type);
    setIp(handle, ip);
    handle->port = port;
    handle->client = client;
    objs[index_id] = (long) handle;
    openThread(handle);
    return objs[index_id++];
}

long openDeviceByCB(const char *ip, int port, DEVICE_TYPE deviceType, JNIEnv *env, jobject obj,
                    jmethodID callback) {
    long handle = openDevice(ip, port, deviceType);
    P_HANDLE p_handle = (P_HANDLE) handle;
    p_handle->p_callback->env = env;
    p_handle->p_callback->jobject1 = obj;
    p_handle->p_callback->callback = callback;
    int result = t_pool_add_work((void *) openCBThread, p_handle);
    if (result == -1) {
        LOGE("thread receiver faild");
    } else {
        LOGE("thread receiver succ");
    }
    return handle;
}

void setScalingDataByHandle(P_HANDLE handle, short scalingData[]) {
    setScalingData(handle, scalingData);
}

double temperatureByGray(P_HANDLE handle, short gray, double e, double s, double value) {
    return temperature(handle, gray, 100, e, s, value);
}

int openThread(P_HANDLE handle) {
    handle->isRun = 1;
    int result = t_pool_add_work((void *) openReceiver, (P_HANDLE) handle);
    if (result == -1) {
        LOGE("thread receiver faild");
    } else {
        LOGE("thread receiver succ");
    }
    return 0;
}

P_HANDLE getPointerByIndex(int index) {
    return (P_HANDLE) objs[index];
}

int startDevice(P_HANDLE handle) {
    sendOpenCmd(handle);
    return 0;
}

int pauseDevice(P_HANDLE handle) {
    sendCloseCmd(handle);
    return 0;
}

int closeDevice(P_HANDLE handle) {
    if (handle->status > STATUS_OPENED) {
        pauseDevice(handle);
    }
    handle->isRun = 0;
    handle->status = STATUS_CLOSED;
    return 0;
};

int correctDevice(P_HANDLE handle) {
    return sendCorrect(handle);
};

void getNextFrame(P_HANDLE handle, short *raw) {
    getNext(handle, raw);
};

int getNextBmp(P_HANDLE handle, unsigned char *RgbData) {
    //short rawData[handle->p_device->img_raw_size];
    short* rawData = getNextShort(handle);
    _14To8(handle, rawData, RgbData);
    return 0;
}

int _14To8(P_HANDLE handle, short *RawData, unsigned char *RgbData) {
    if (RawData == NULL || RgbData == NULL) {
        return (-1);
    }
    int size = handle->p_device->gray_size;
    const short *pSrc = RawData;
    int i = 0, j = 0, k = 0, sum = 0;

    int nSum = 0;
    int nMin = 0x3fff;
    int nMax = 0;
    int nSigma = 0;
    float nK = 0, nC = 0;
    int pHist[HIST_DEFAULT] = {0};
    memset((void *) pHist, 0, HIST_DEFAULT * sizeof(int));

    for (i = 0; i < size; i++) {
        pHist[pSrc[i] + AD_OFFSET_VALUE]++;
    }
    nSum = 0;
    //
    nSigma = (int) POINT_CNT(size);
    for (i = 0; i < HIST_DEFAULT; i++) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMin = i;
            break;
        }
    }
    nSum = 0;
    for (i = HIST_DEFAULT - 1; i >= 0; i--) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMax = i;
            break;
        }
    }

    nMin -= AD_OFFSET_VALUE;
    nMax -= AD_OFFSET_VALUE;

    float K = (float) ((220.0 - 30) / (nMax - nMin + 1)); //对比度
    float C = (float) ((30.0 * nMax - 220 * nMin)
                       / (nMax - nMin + 1)); //亮度

    if (K > DEFAULT_K_THRESHOLD) {
        K = (float) DEFAULT_K_THRESHOLD;
        int nSigma = 0.5 * size;
        int nSum = 0;
        int nMid = 0;

        for (i = 0; i < HIST_DEFAULT; i++) {
            nSum += pHist[i];
            if (nSum >= nSigma) {
                nMid = i;
                break;
            }
        }

        nMid -= AD_OFFSET_VALUE;
        C = (128 - K * nMid);
    }

    g_CBuff[g_BCPT] = K;
    g_BBuff[g_BCPT] = C;
    g_BCPT++;
    if (g_BCPT == 8)
        g_BCPT = 0;
    nK = 0;
    nC = 0;
    for (i = 0; i < 3; i++) {
        nK += g_CBuff[i];
        nC += g_BBuff[i];
    }

    nK = nK / 3;
    nC = nC / 3;
    unsigned char m_p8BitBmp[handle->p_device->gray_size];
    for (i = 0; i < size; i++) {
        int nValue = (int) (nK * pSrc[i] + nC);
        if (nValue < 0) {
            m_p8BitBmp[i] = 0;
        } else if (nValue > AD_LIMIT_VALUE) {
            m_p8BitBmp[i] = (unsigned char) AD_LIMIT_VALUE;
        } else {
            m_p8BitBmp[i] = (unsigned char) nValue;
        }
    }

    int headSize = sizeof(BITMAP_FILE_HEADER);
    int infoSize = sizeof(BITMAP_INFO_HEADER);
    int colorSize = sizeof(RGBQUAD) * 256;
    int countSize = headSize + infoSize + colorSize;

    memcpy(RgbData, &file_header, headSize);
    memcpy(RgbData + headSize, &header, (infoSize));
    memcpy(RgbData + headSize + infoSize, rgb, (colorSize));
    memcpy(RgbData + countSize, m_p8BitBmp, (size));

    return 0;
}

static uint16_t palette[PALETTE_SIZE];

static uint16_t make565(int red, int green, int blue) {
#if 0
    return (uint16_t)( ((red   << 8) & 0xf800) |
                   ((green << 3) & 0x03f0) |
                   ((blue  >> 3) & 0x001f) );
#else
    //
    return (uint16_t) ((blue >> 3) | ((green & ~3) << 3) | ((red & ~7) << 8));
#endif
}

void init_palette(int index) {
    int nn, mm = 0;
    /* fun with colors */
    for (nn = 0; nn < PALETTE_SIZE; nn++) {
        int r = colorsTable[index][nn][0];
        int g = colorsTable[index][nn][1];
        int b = colorsTable[index][nn][2];
        palette[nn] = make565(r, g, b);

//        D("%s : R:%d , G:%d , B:%d , 565RGB: %d\n", __FUNCTION__ , r , g , b , palette[nn]);
    }
}

static __inline__ uint16_t palette_from_fixed(int x) {
    if (x < 0) x = -x;
    if (x >= FIXED_ONE) x = FIXED_ONE - 1;
    int idx = FIXED_FRAC(x) >> (FIXED_BITS - PALETTE_BITS);

//    D("%s : idx: %d\n", __FUNCTION__ , idx);
    return palette[idx & (PALETTE_SIZE - 1)];
}

int _14To565Toshort(P_HANDLE p_handle, short *RawData, short *RgbData) {
    if (RawData == NULL || RgbData == NULL) {
        return (-1);
    }
    int size = p_handle->p_device->gray_size;
    const short *pSrc = RawData;
    int i = 0, j = 0, k = 0, sum = 0;

    int nSum = 0;
    int nMin = 0x3fff;
    int nMax = 0;
    int nSigma = 0;
    float nK = 0, nC = 0;
    int pHist[HIST_DEFAULT] = {0};
    memset((void *) pHist, 0, HIST_DEFAULT * sizeof(int));

    for (i = 0; i < size; i++) {
        pHist[pSrc[i] + AD_OFFSET_VALUE]++;
    }
    nSum = 0;
    //
    nSigma = POINT_CNT(size);
    for (i = 0; i < HIST_DEFAULT; i++) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMin = i;
            break;
        }
    }
    nSum = 0;
    for (i = HIST_DEFAULT - 1; i >= 0; i--) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMax = i;
            break;
        }
    }

    nMin -= AD_OFFSET_VALUE;
    nMax -= AD_OFFSET_VALUE;

    float K = (float) ((220.0 - 30) / (nMax - nMin + 1)); //对比度
    float C = (float) ((30.0 * nMax - 220 * nMin)
                       / (nMax - nMin + 1)); //亮度

    if (K > DEFAULT_K_THRESHOLD) {
        K = (float) DEFAULT_K_THRESHOLD;
        int nSigma = 0.5 * size;
        int nSum = 0;
        int nMid = 0;

        for (i = 0; i < HIST_DEFAULT; i++) {
            nSum += pHist[i];
            if (nSum >= nSigma) {
                nMid = i;
                break;
            }
        }

        nMid -= AD_OFFSET_VALUE;
        C = (float) (128 - K * nMid);
    }

    g_CBuff[g_BCPT] = K;
    g_BBuff[g_BCPT] = C;
    g_BCPT++;
    if (g_BCPT == 8)
        g_BCPT = 0;
    nK = 0;
    nC = 0;
    for (i = 0; i < 8; i++) {
        nK += g_CBuff[i];
        nC += g_BBuff[i];
    }

    nK = nK / 8;
    nC = nC / 8;

    for (i = 0; i < size; i++) {
        int nValue = (int) (nK * pSrc[i] + nC);
        if (nValue < 0) {
            RgbData[i] = 0;
        } else if (nValue > AD_LIMIT_VALUE) {
            RgbData[i] = palette_from_fixed(
                    (AD_LIMIT_VALUE >> 3) | ((AD_LIMIT_VALUE & ~3) << 3) |
                    ((AD_LIMIT_VALUE & ~7) << 8));
        } else {
            //RgbData[i] = (LEP_CHAR8) nValue;
            int t = nValue;
            RgbData[i] = palette_from_fixed((t >> 3) | ((t & ~3) << 3) | ((t & ~7) << 8));
        }
    }

    return 0;
}

int _14To565(P_HANDLE p_handle, short *RawData, int *RgbData) {
    if (RawData == NULL || RgbData == NULL) {
        return (-1);
    }
    int size = p_handle->p_device->gray_size;
    const short *pSrc = RawData;
    int i = 0, j = 0, k = 0, sum = 0;

    int nSum = 0;
    int nMin = 0x3fff;
    int nMax = 0;
    int nSigma = 0;
    float nK = 0, nC = 0;
    int pHist[HIST_DEFAULT] = {0};
    memset((void *) pHist, 0, HIST_DEFAULT * sizeof(int));

    for (i = 0; i < size; i++) {
        pHist[pSrc[i] + AD_OFFSET_VALUE]++;
    }
    nSum = 0;
    //
    nSigma = POINT_CNT(size);
    for (i = 0; i < HIST_DEFAULT; i++) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMin = i;
            break;
        }
    }
    nSum = 0;
    for (i = HIST_DEFAULT - 1; i >= 0; i--) {
        nSum += pHist[i];
        if (nSum >= nSigma) {
            nMax = i;
            break;
        }
    }

    nMin -= AD_OFFSET_VALUE;
    nMax -= AD_OFFSET_VALUE;

    float K = (float) ((220.0 - 30) / (nMax - nMin + 1)); //对比度
    float C = (float) ((30.0 * nMax - 220 * nMin)
                       / (nMax - nMin + 1)); //亮度

    if (K > DEFAULT_K_THRESHOLD) {
        K = (float) DEFAULT_K_THRESHOLD;
        int nSigma = 0.5 * size;
        int nSum = 0;
        int nMid = 0;

        for (i = 0; i < HIST_DEFAULT; i++) {
            nSum += pHist[i];
            if (nSum >= nSigma) {
                nMid = i;
                break;
            }
        }

        nMid -= AD_OFFSET_VALUE;
        C = (float) (128 - K * nMid);
    }

    g_CBuff[g_BCPT] = K;
    g_BBuff[g_BCPT] = C;
    g_BCPT++;
    if (g_BCPT == 8)
        g_BCPT = 0;
    nK = 0;
    nC = 0;
    for (i = 0; i < 8; i++) {
        nK += g_CBuff[i];
        nC += g_BBuff[i];
    }

    nK = nK / 8;
    nC = nC / 8;

    for (i = 0; i < size; i++) {
        int nValue = (int) (nK * pSrc[i] + nC);
        if (nValue < 0) {
            RgbData[i] = 0;
        } else if (nValue > AD_LIMIT_VALUE) {
            RgbData[i] = palette_from_fixed(
                    (AD_LIMIT_VALUE >> 3) | ((AD_LIMIT_VALUE & ~3) << 3) |
                    ((AD_LIMIT_VALUE & ~7) << 8));
        } else {
            //RgbData[i] = (LEP_CHAR8) nValue;
            int t = nValue;
            RgbData[i] = palette_from_fixed((t >> 3) | ((t & ~3) << 3) | ((t & ~7) << 8));
        }
    }

    return 0;
}

/**
     * BMP文件信息头
     *
     * @param w
     * @param h
     * @return
     */
void addBMPImage_Info_Header(int w, int h) {

    header.biSize = sizeof(BITMAP_INFO_HEADER);
    header.biHeight = h;
    header.biWidth = w;
    header.biPlanes = 1;
    header.biBitCount = 8;
    header.biCompression = 0L;
    header.biSizeImage = w * h;
    header.biXPelsPerMeter = 0;
    header.biYPelsPerMeter = 0;
    header.biClrImportant = 0;
    header.biClrUsed = 256;
}

/**
 * 头文件
 *
 * @param size
 * @return
 */
void addFileHeader(int size) {

    file_header.bfType = 0x4d42; // 'BM' WINDOWS_BITMAP_SIGNATURE
    file_header.bfSize = DIB_SIZE(header) + sizeof(BITMAP_FILE_HEADER);
    file_header.bfReserved1 = 0;
    file_header.bfReserved2 = 0;
    file_header.bfOffBits = sizeof(BITMAP_FILE_HEADER) + header.biSize
                            + PALETTE(header);

}

void setColorName(int c) {
    colorName = c;
    setColorsName(colorName);
}

int getColorName(void) {
    return colorName;
};

void setColorsName(int colorsName) {
    init_palette(colorsName);
    int i = 0;
    for (i = 0; i < 256; i++) {
        rgb[i].rgbBlue = (char) colorsTable[colorsName][i][2];
        rgb[i].rgbGreen = (char) colorsTable[colorsName][i][1];
        rgb[i].rgbRed = (char) colorsTable[colorsName][i][0];
        rgb[i].rgbReserved = 0;
    }
}


int byteArrayToShortArray(unsigned char *src, short *dst, int lenght) {
    short *tmp = (short *) src;
    memcpy(dst, tmp, (size_t) lenght);
    return 0;
}
