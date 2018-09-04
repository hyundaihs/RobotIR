//
// Created by Administrator on 2018/5/14.
//


#include "Util.h"
#include <math.h>

int m_nChsFeatureNum=3;

void GetAffinePara1(POINT_ pPointIR[3], POINT_ pPointVis[3], double* pDbAffPara)
{
    // pDbBMatrix中存放的是基准图象中特征点的坐标，
    // 大小为2*m_nChsFeatureNum，前m_nChsFeatureNum为X坐标
    double *pDbBMatrix;
    pDbBMatrix = new double[2*m_nChsFeatureNum];

    // pDbSMatrix中存放的是待配准图象中特征点的扩展坐标
    // 大小为3*m_nChsFeatureNum，其中前m_nChsFeatureNum为X坐标
    // 中间m_nChsFeatureNum个为Y坐标，后面m_nChsFeatureNum为1
    double *pDbSMatrix;
    pDbSMatrix = new double[3*m_nChsFeatureNum];

    // pDbSMatrixT中存放的pDbSMatrix的转置矩阵，
    // 大小为m_nChsFeatureNum*3
    double *pDbSMatrixT;
    pDbSMatrixT = new double[m_nChsFeatureNum*3];

    // pDbInvMatrix为临时变量，存放的是pDbSMatrix*pDbSMatrixT的逆
    // 大小为3*3
    double *pDbInvMatrix;
    pDbInvMatrix = new double[3*3];

    // 临时内存
    double *pDbTemp;
    pDbTemp = new double[2*3];

    // 循环变量
    int count;

    // 给矩阵赋值
    for(count = 0; count<m_nChsFeatureNum; count++)
    {
        pDbBMatrix[0*m_nChsFeatureNum + count] = pPointVis[count].x;
        pDbBMatrix[1*m_nChsFeatureNum + count] = pPointVis[count].y;
        pDbSMatrix[0*m_nChsFeatureNum + count] = pPointIR[count].x;
        pDbSMatrix[1*m_nChsFeatureNum + count] = pPointIR[count].y;
        pDbSMatrix[2*m_nChsFeatureNum + count] = 1;
        pDbSMatrixT[count*m_nChsFeatureNum + 0] = pPointIR[count].x;
        pDbSMatrixT[count*m_nChsFeatureNum + 1] = pPointIR[count].y;
        pDbSMatrixT[count*m_nChsFeatureNum + 2] = 1;
    }

    // 计算pDbSMatrix*pDbSMatrixT，并将结果放入pDbInvMatrix中
    CalMatProduct(pDbSMatrix,pDbSMatrixT,pDbInvMatrix,3,3,m_nChsFeatureNum);

    // 计算pDbInvMatrix的逆矩阵
    CalInvMatrix(pDbInvMatrix, 3);

    // 计算仿射变换系数
    CalMatProduct(pDbBMatrix, pDbSMatrixT, pDbTemp, 2, 3, m_nChsFeatureNum);
    CalMatProduct(pDbTemp, pDbInvMatrix, pDbAffPara, 2, 3, 3);

    // 释放内存
    delete[]pDbBMatrix;
    delete[]pDbSMatrix;
    delete[]pDbSMatrixT;
    delete[]pDbInvMatrix;
    delete[]pDbTemp;

}

void CalMatProduct(double* pDbSrc1, double *pDbSrc2, double *pDbDest, int y, int x, int z)
{

    for(int i=0;i<y;i++)
        for(int j=0;j<x;j++)
        {
            pDbDest[i*x + j] = 0;
            for(int m=0;m<z;m++)
                pDbDest[i*x + j] += pDbSrc1[i*z + m]*pDbSrc2[m*x + j];
        }
}

bool CalInvMatrix(double *pDbSrc, int nLen)
{
    int *is,*js,i,j,k;
    double d,p;
    is = new int[nLen];
    js = new int[nLen];
    for(k=0;k<nLen;k++)
    {
        d=0.0;
        for(i=k;i<nLen;i++)
            for(j=k;j<nLen;j++)
            {
                p=fabs(pDbSrc[i*nLen + j]);
                if(p>d)
                {
                    d     = p;
                    is[k] = i;
                    js[k] = j;
                }
            }
        if(d+1.0==1.0)
        {
            delete is;
            delete js;
            return false;
        }
        if(is[k] != k)
            for(j=0;j<nLen;j++)
            {
                p = pDbSrc[k*nLen + j];
                pDbSrc[k*nLen + j] = pDbSrc[(is[k]*nLen) + j];
                pDbSrc[(is[k])*nLen + j] = p;
            }
        if(js[k] != k)
            for(i=0; i<nLen; i++)
            {
                p = pDbSrc[i*nLen + k];
                pDbSrc[i*nLen + k] = pDbSrc[i*nLen + (js[k])];
                pDbSrc[i*nLen + (js[k])] = p;
            }

        pDbSrc[k*nLen + k]=1.0/pDbSrc[k*nLen + k];
        for(j=0; j<nLen; j++)
            if(j != k)
            {
                pDbSrc[k*nLen + j]*=pDbSrc[k*nLen + k];
            }
        for(i=0; i<nLen; i++)
            if(i != k)
                for(j=0; j<nLen; j++)
                    if(j!=k)
                    {
                        pDbSrc[i*nLen + j] -= pDbSrc[i*nLen + k]*pDbSrc[k*nLen + j];
                    }
        for(i=0; i<nLen; i++)
            if(i != k)
            {
                pDbSrc[i*nLen + k] *= -pDbSrc[k*nLen + k];
            }
    }
    for(k=nLen-1; k>=0; k--)
    {
        if(js[k] != k)
            for(j=0; j<nLen; j++)
            {
                p = pDbSrc[k*nLen + j];
                pDbSrc[k*nLen + j] = pDbSrc[(js[k])*nLen + j];
                pDbSrc[(js[k])*nLen + j] = p;
            }
        if(is[k] != k)
            for(i=0; i<nLen; i++)
            {
                p = pDbSrc[i*nLen + k];
                pDbSrc[i*nLen + k] = pDbSrc[i*nLen +(is[k])];
                pDbSrc[i*nLen + (is[k])] = p;
            }
    }
    delete is;
    delete js;
    return true;
}

