//
// Created by Kevin on 16/12/2.
//

#ifndef HYL026_MSOCKET_H
#define HYL026_MSOCKET_H

#include <sys/socket.h>
#include <arpa/inet.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#define SOCKADDR_IN struct sockaddr_in
#define SOCKADDR struct sockaddr
#define SOCKET int

//#define OPEN "OpenIRVideo24"
//#define CLOSE "CloseIRVideo88"
//#define HEART_PACKET "CWDde"
//#define CORRECT "DoFFC82"

#define OPEN "OpenIRVideo24"
#define CLOSE "CloseIRVideo88"
#define HEART_PACKET "CWDde"
#define CORRECT "DoFFC82"

#endif //HYL026_MSOCKET_H
