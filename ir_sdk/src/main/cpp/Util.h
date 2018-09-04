//
// Created by Administrator on 2018/5/14.
//

#ifndef FRAREDDEMO_UTIL_H
#define FRAREDDEMO_UTIL_H

#endif //FRAREDDEMO_UTIL_H


typedef struct tagIRPoint {
    int x;
    int y;
}POINT_,*PPOINT_;

void GetAffinePara1(POINT_ pPointIR[3], POINT_ pPointVis[3], double* pDbAffPara);

void CalMatProduct(double* pDbSrc1, double *pDbSrc2, double *pDbDest, int y, int x, int z);
bool CalInvMatrix(double *pDbSrc, int nLen);