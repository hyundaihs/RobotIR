//
// Created by Kevin on 16/12/2.
//

#ifndef HYL026_ERRORTABLE_H
#define HYL026_ERRORTABLE_H

extern const char *errorTable[10] ;

 typedef enum _ErrorType_ {
    SUCCESSFUL = 0,
    SOCKET_OPEN_FAIL = 1,
    SOCKET_BIND_FAIL = 2,
    SOCKET_RECEIVER_FAIL = 3,
    SOCKET_SEND_FAIL = 4,
    LEPTON_OPEN_FAIL = 5,
    NULL_POINTER = 6
} ErrorType,*PErrorType;

#endif //HYL026_ERRORTABLE_H
