//
// Created by Administrator on 2017/4/20.
//
#include "jni_util.h"
#include <string.h>
#include <android/bitmap.h>

/**
 * jstring转char数组
 * @param buf 填充的数组
 * @param string 源数据
 */
void jstring2chars(JNIEnv *env, char *buf, jstring prompt) {
    const char *str = (*env)->GetStringUTFChars(env, prompt, 0);
    strcpy(buf, str);
    (*env)->ReleaseStringUTFChars(env, prompt, str);
}
