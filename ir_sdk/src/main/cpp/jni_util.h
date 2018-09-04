//
// Created by Administrator on 2017/4/20.
//

#include <jni.h>

#ifndef ZHUDAO_JNI_UTIL_H
#define ZHUDAO_JNI_UTIL_H

#endif //ZHUDAO_JNI_UTIL_H

/**
 * jstring 2 char[]
 * @param env
 * @param buf char* buffer
 * @param string source data
 */
void jstring2chars(JNIEnv *env, char *buf, jstring string);
