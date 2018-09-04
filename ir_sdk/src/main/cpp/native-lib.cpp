#include <jni.h>
#include <string>

extern "C" {
#include "LeptonControl.h"


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_init(JNIEnv *env, jclass type) {
    return init();
}


JNIEXPORT jlong JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_open(JNIEnv *env, jclass type, jstring ip_,
                                                       jint port,
                                                       jint deviceType) {
    const char *ip = env->GetStringUTFChars(ip_, 0);

    long handle = openDevice(ip, port, (DEVICE_TYPE) deviceType);

    env->ReleaseStringUTFChars(ip_, ip);
    return handle;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_start(JNIEnv *env, jclass type, jlong handle) {
    return startDevice((P_HANDLE) handle);
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_stop(JNIEnv *env, jclass type, jlong handle) {
    return pauseDevice((P_HANDLE) handle);
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_close(JNIEnv *env, jclass type, jlong handle) {
    return closeDevice((P_HANDLE) handle);
}

JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_correct(JNIEnv *env, jclass type, jlong handle) {
    return correctDevice((P_HANDLE) handle);
}

JNIEXPORT void JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_nextFrame(JNIEnv *env, jclass type, jlong handle,
                                                            jshortArray raw_) {
    jshort *raw = env->GetShortArrayElements(raw_, NULL);

    getNextFrame((P_HANDLE) handle, raw);

    env->ReleaseShortArrayElements(raw_, raw, 0);
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getFps(JNIEnv *env, jclass type, jlong handle) {
    return ((P_HANDLE) handle)->fps;
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getEnvTemp(JNIEnv *env, jclass type,
                                                             jlong handle) {
    return ((P_HANDLE) handle)->envTemp;
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getSutTemp(JNIEnv *env, jclass type,
                                                             jlong handle) {
    return ((P_HANDLE) handle)->sutTemp;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMaxX(JNIEnv *env, jclass type, jlong handle) {
    return ((P_HANDLE) handle)->max_x;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMaxY(JNIEnv *env, jclass type, jlong handle) {
    return ((P_HANDLE) handle)->max_y;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMinX(JNIEnv *env, jclass type, jlong handle) {
    return ((P_HANDLE) handle)->min_x;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMinY(JNIEnv *env, jclass type, jlong handle) {
    return ((P_HANDLE) handle)->min_y;
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMaxTemp(JNIEnv *env, jclass type,
                                                             jlong handle) {
    return ((P_HANDLE) handle)->maxTemp;
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getMinTemp(JNIEnv *env, jclass type,
                                                             jlong handle) {
    return ((P_HANDLE) handle)->minTemp;
}


JNIEXPORT jfloat JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getPointTemp(JNIEnv *env, jclass type,
                                                               jlong handle,
                                                               jint x, jint y) {

    return 0.0;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_img_114To8(JNIEnv *env, jclass type, jlong handle,
                                                             jshortArray raw_, jbyteArray bmp_) {
    jshort *raw = env->GetShortArrayElements(raw_, NULL);
    jbyte *bmp = env->GetByteArrayElements(bmp_, NULL);

    int rel = _14To8((P_HANDLE) handle, raw, (unsigned char *) bmp);

    env->ReleaseShortArrayElements(raw_, raw, 0);
    env->ReleaseByteArrayElements(bmp_, bmp, 0);
    return rel;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_img_114To565(JNIEnv *env, jclass type,
                                                               jlong handle,
                                                               jshortArray rawData_,
                                                               jintArray rgbData_) {
    jshort *rawData = env->GetShortArrayElements(rawData_, NULL);
    jint *rgbData = env->GetIntArrayElements(rgbData_, NULL);

    int rel = _14To565((P_HANDLE) handle, rawData, rgbData);

    env->ReleaseShortArrayElements(rawData_, rawData, 0);
    env->ReleaseIntArrayElements(rgbData_, rgbData, 0);
    return rel;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_img_114To565ToShort(JNIEnv *env, jclass type,
                                                                      jlong handle,
                                                                      jshortArray rawData_,
                                                                      jshortArray rgbData_) {
    jshort *rawData = env->GetShortArrayElements(rawData_, NULL);
    jshort *rgbData = env->GetShortArrayElements(rgbData_, NULL);

    int rel = _14To565Toshort((P_HANDLE) handle, rawData, rgbData);

    env->ReleaseShortArrayElements(rawData_, rawData, 0);
    env->ReleaseShortArrayElements(rgbData_, rgbData, 0);
    return rel;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getColorName(JNIEnv *env, jclass type,
                                                               jlong handle) {
    return getColorName();
}


JNIEXPORT void JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_setColorName(JNIEnv *env, jclass type,
                                                               jlong handle,
                                                               jint index) {
    setColorName(index);
}


JNIEXPORT void JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_setStatus(JNIEnv *env, jclass type, jlong handle,
                                                            jint status) {
    ((P_HANDLE) handle)->status = (STATUS) status;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getStatus(JNIEnv *env, jclass type,
                                                            jlong handle) {
    return ((P_HANDLE) handle)->status;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getCurrentFrameNum(JNIEnv *env, jclass type,
                                                                     jlong handle) {
    return ((P_HANDLE) handle)->frameId;
}


JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_bToS(JNIEnv *env, jclass type, jbyteArray b_,
                                                       jshortArray s_, jint l) {
    jbyte *b = env->GetByteArrayElements(b_, NULL);
    jshort *s = env->GetShortArrayElements(s_, NULL);

    int rel = byteArrayToShortArray(reinterpret_cast<unsigned char *>(b), s, l);

    env->ReleaseByteArrayElements(b_, b, 0);
    env->ReleaseShortArrayElements(s_, s, 0);
    return rel;
}

JNIEXPORT jint JNICALL
Java_com_hzncc_kevin_frareddemo_ir_1sdk_CameraSDK_getNextBmp(JNIEnv *env, jclass type, jlong handle,
                                                             jbyteArray bmp_) {
    jbyte *bmp = env->GetByteArrayElements(bmp_, NULL);

    int rel = getNextBmp((P_HANDLE) handle, (unsigned char *) bmp);

    env->ReleaseByteArrayElements(bmp_, bmp, 0);
    return rel;
}
}
