package com.hzncc.kevin.robot_ir.utils

import android.graphics.Point
import com.hzncc.kevin.robot_ir.utils.GetAffParam.Companion.m_nChsFeatureNum

/**
 * Created by kevin on 2018/6/4.
 */

class GetAffParam {
    companion object {
        val m_nChsFeatureNum = 3
    }
}

fun GetAffinePara1(pPointIR: Array<Point>, pPointVis: Array<Point>): DoubleArray {
    val pDbAffPara: DoubleArray = kotlin.DoubleArray(6)
    // pDbBMatrix中存放的是基准图象中特征点的坐标，
    // 大小为2*3，前3为X坐标
    val pDbBMatrix = kotlin.DoubleArray(2 * m_nChsFeatureNum)

    // pDbSMatrix中存放的是待配准图象中特征点的扩展坐标
    // 大小为3*3，其中前3为X坐标
    // 中间3个为Y坐标，后面3为1
    val pDbSMatrix = kotlin.DoubleArray(3 * m_nChsFeatureNum)

    // pDbSMatrixT中存放的pDbSMatrix的转置矩阵，
    // 大小为3*3
    val pDbSMatrixT = kotlin.DoubleArray(3 * m_nChsFeatureNum)

    // pDbInvMatrix为临时变量，存放的是pDbSMatrix*pDbSMatrixT的逆
    // 大小为3*3
    val pDbInvMatrix = kotlin.DoubleArray(3 * m_nChsFeatureNum)

    // 临时内存
    val pDbTemp = kotlin.DoubleArray(2 * m_nChsFeatureNum)

    // 给矩阵赋值
    for (count in 0 until m_nChsFeatureNum) {
        pDbBMatrix[0 * m_nChsFeatureNum + count] = pPointVis[count].x.toDouble()
        pDbBMatrix[1 * m_nChsFeatureNum + count] = pPointVis[count].y.toDouble()
        pDbSMatrix[0 * m_nChsFeatureNum + count] = pPointIR[count].x.toDouble()
        pDbSMatrix[1 * m_nChsFeatureNum + count] = pPointIR[count].y.toDouble()
        pDbSMatrix[2 * m_nChsFeatureNum + count] = 1.0
        pDbSMatrixT[count * m_nChsFeatureNum + 0] = pPointIR[count].x.toDouble()
        pDbSMatrixT[count * m_nChsFeatureNum + 1] = pPointIR[count].y.toDouble()
        pDbSMatrixT[count * m_nChsFeatureNum + 2] = 1.0
    }

    // 计算pDbSMatrix*pDbSMatrixT，并将结果放入pDbInvMatrix中
    CalMatProduct(pDbSMatrix, pDbSMatrixT, pDbInvMatrix, 3, 3, m_nChsFeatureNum)

    // 计算pDbInvMatrix的逆矩阵
    CalInvMatrix(pDbInvMatrix, 3)

    // 计算仿射变换系数
    CalMatProduct(pDbBMatrix, pDbSMatrixT, pDbTemp, 2, 3, m_nChsFeatureNum)
    CalMatProduct(pDbTemp, pDbInvMatrix, pDbAffPara, 2, 3, 3)
    return pDbAffPara
}

fun CalMatProduct(pDbSrc1: DoubleArray, pDbSrc2: DoubleArray, pDbDest: DoubleArray, y: Int, x: Int, z: Int) {

    for (i in 0 until y) {
        for (j in 0 until x) {
            pDbDest[i * x + j] = 0.0
            for (m in 0 until z)
                pDbDest[i * x + j] += pDbSrc1[i * z + m] * pDbSrc2[m * x + j]
        }
    }
}

fun CalInvMatrix(pDbSrc: DoubleArray, nLen: Int): Boolean {
    var is_ = IntArray(nLen)
    var js = IntArray(nLen)
    var d = 0.0
    var p: Double
    for (k in 0 until nLen) {
        for (i in k until nLen) {
            for (j in k until nLen) {
                p = Math.abs(pDbSrc[i * nLen + j])
                if (p > d) {
                    d = p
                    is_[k] = i
                    js[k] = j
                }
            }
        }
        if (d + 1.0 == 1.0) {
            return false
        }
        if (is_[k] != k) {
            for (j in 0 until nLen) {
                p = pDbSrc[k * nLen + j]
                pDbSrc[k * nLen + j] = pDbSrc[(is_[k] * nLen) + j]
                pDbSrc[(is_[k]) * nLen + j] = p
            }
        }
        if (js[k] != k) {
            for (i in 0 until nLen) {
                p = pDbSrc[i * nLen + k]
                pDbSrc[i * nLen + k] = pDbSrc[i * nLen + (js[k])]
                pDbSrc[i * nLen + (js[k])] = p
            }
        }
        pDbSrc[k * nLen + k] = 1.0 / pDbSrc[k * nLen + k]
        for (j in 0 until nLen) {
            if (j != k) {
                pDbSrc[k * nLen + j] *= pDbSrc[k * nLen + k]
            }
        }
        for (i in 0 until nLen) {
            if (i != k) {
                for (j in 0 until nLen) {
                    if (j != k) {
                        pDbSrc[i * nLen + j] -= pDbSrc[i * nLen + k] * pDbSrc[k * nLen + j]
                    }
                }
            }
        }
        for (i in 0 until nLen) {
            if (i != k) {
                pDbSrc[i * nLen + k] *= -pDbSrc[k * nLen + k]
            }
        }
    }
    for (k in (nLen - 1) downTo 0) {
        if (js[k] != k) {
            for (j in 0 until nLen) {
                p = pDbSrc[k * nLen + j]
                pDbSrc[k * nLen + j] = pDbSrc[(js[k]) * nLen + j]
                pDbSrc[(js[k]) * nLen + j] = p
            }
        }
        if (is_[k] != k) {
            for (i in 0 until nLen) {
                p = pDbSrc[i * nLen + k]
                pDbSrc[i * nLen + k] = pDbSrc[i * nLen + (is_[k])]
                pDbSrc[i * nLen + (is_[k])] = p
            }
        }
    }
    return true
}
