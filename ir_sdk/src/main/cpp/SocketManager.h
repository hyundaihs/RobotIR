//
// Created by Kevin on 16/12/2.
//

#ifndef HYL026_SOCKETMANAGER_H
#define HYL026_SOCKETMANAGER_H

#include "Lepton.h"
#include "mSocket.h"
#include "aLog.h"

#define MAX_RESERVED_LEN    10
#define MAX_MAC_STR_LEN        6
#define MAX_IP_STR_LEN        4
#define MAX_SERIAL_CMD_LEN    16
#define MAX_SERIAL_NUM_LEN    16
#define MAX_MCARD_NUM_HANDLE 255

#define UINT8 unsigned char

typedef struct _PACKET_80_ {
    UINT8 ver[9];                //版本信息
    UINT8 temp1[9];                //1号温传温度（快门温度）
    UINT8 temp2[9];                //2号温传温度（环境温度）
    UINT8 res[5 + 24];                    //预留字节
    unsigned int frameID;
    unsigned short dataLen;       //包数据长度  164*6
    UINT8 dataFol;
    UINT8 packetID;
} IR_PACKET_80, *PIR_PACKET_80;

typedef struct _PACKET_ELSE_ {
    UINT8 ver[9];                //版本信息
    UINT8 temp1[9];                //1号温传温度（快门温度）
    UINT8 temp2[9];                //2号温传温度（环境温度）
    UINT8 res[5];                    //预留字节
    unsigned int frameID;
    unsigned short dataLen;       //包数据长度  164*6
    unsigned short dataFol;
    unsigned short packetID;
    UINT8 res1[22];                    //预留字节
} IR_PACKET_ELSE, *PIR_PACKET_ELSE;

typedef struct _PACKET_ {
    UINT8 *ver;
    UINT8 *temp1;
    UINT8 *temp2;
    UINT8 *res;
    unsigned int frameID;
    unsigned short dataLen;
    unsigned short dataFol;
    unsigned short packetID;
    UINT8 *res1;
} IR_PACKET, *PIR_PACKET;

typedef enum MESSAGE_TYPE_e {
    MESSAGE_TYPE_NONE = 0x8000,
    MESSAGE_TYPE_SEARCH = 0x8001,    //Search Request
    MESSAGE_TYPE_NET_CFG = 0x8002,    //Config Target Net Configuration
    MESSAGE_TYPE_VIDEO_CFG_GET = 0x8003,    //Get Video Configuration
    MESSAGE_TYPE_VIDEO_OPEN = 0x8004,    //Open Video
    MESSAGE_TYPE_VIDEO_PAUSE = 0x8005,    //Pause Video
    MESSAGE_TYPE_VIDEO_CLOSE = 0x8006,    //Close Video
    MESSAGE_TYPE_SERIAL_CMD = 0x8007,    //Serial Command & Response
    MESSAGE_TYPE_SERIAL_NO = 0x8008,    //Get Target Serial Number
    MESSAGE_TYPE_VIDEO_MODE_SW = 0x8009,   //Video Mode Switch Command
    MESSAGE_TYPE_VIDEO_FETCH_FR = 0x800a,   //Fetch a frame from Device
} MESSAGE_TYPE;

typedef struct NET_CONFIG_t {
    UINT8 mode;    // 0 for Static; 1 for Dynamic
    UINT8 mac[MAX_MAC_STR_LEN];
    UINT8 ip[MAX_IP_STR_LEN];
    UINT8 netmask[MAX_IP_STR_LEN];
    UINT8 gateway[MAX_IP_STR_LEN];
    UINT8 dns1[MAX_IP_STR_LEN];
    UINT8 dns2[MAX_IP_STR_LEN];
    char rev[5];
    short iWith;
    short iHeight;
    short iPackCnt;
    char ID[26];
} NET_CONFIG, *PNET_CONFIG;


typedef enum MESSAGE_DIRECTION_e {
    MESSAGE_DIRECTION_TO_TARGET = 0x0000,            //Message From Host to Target
    MESSAGE_DIRECTION_TO_HOST = 0x0001            //Message From Target to Host
} MESSAGE_DIRECTION;

typedef struct MESSAGE_STRUCT_t {
    unsigned short type;
    unsigned short dir;
    unsigned short datLen;
    char resv[MAX_RESERVED_LEN];
} MESSAGE_STRUCT, *PMESSAGE_STRUCT;

typedef struct VIDEO_TRANS_DEF_t {
    unsigned short port;
    unsigned short mode;
} VIDEO_TRANS_DEF, *PVIDEO_TRANS_DEF;

typedef struct SERIAL_DATA_t {
    // UINT8 flagMoreData;		//If Set, more data will be followed
    UINT8 serDat[MAX_SERIAL_CMD_LEN];
} SERIAL_DATA, *PSERIAL_DATA;

void openSearchReceiver(P_CALLBACK p_callback);

void * onCountFps(P_HANDLE handle);

void openReceiver(P_HANDLE handle);

int sendOpenCmd(P_HANDLE handle);

int sendCloseCmd(P_HANDLE handle);

int sendCorrect(P_HANDLE handle);

int sendSearchCmd(SOCKET client);

int sendMessage(P_HANDLE handle, char *message, int);

#endif //HYL026_SOCKETMANAGER_H
