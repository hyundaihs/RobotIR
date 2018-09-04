//
// Created by Kevin on 16/12/14.
//

#include "ThreadPool.h"

#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <string.h>


static t_pool_t *t_pool = NULL;

/* 工作者线程函数, 从任务链表中取出任务并执行 */
static void *thread_routine(void *arg) {
    t_pool_work_t *work;

    while (1) {
        /* 如果线程池没有被销毁且没有任务要执行，则等待 */
        pthread_mutex_lock(&t_pool->queue_lock);
        while (!t_pool->queue_head && !t_pool->shutdown) {
            pthread_cond_wait(&t_pool->queue_ready, &t_pool->queue_lock);
        }
        if (t_pool->shutdown) {
            pthread_mutex_unlock(&t_pool->queue_lock);
            pthread_exit(NULL);
        }
        work = t_pool->queue_head;
        t_pool->queue_head = t_pool->queue_head->next;
        pthread_mutex_unlock(&t_pool->queue_lock);

        work->routine(work->arg);
        free(work);
    }

    return NULL;
}

/*
:  * 创建线程池
:  */
int t_pool_create(int max_thr_num) {
    int i;

    t_pool = calloc(1, sizeof(t_pool_t));
    if (!t_pool) {
        printf("%s: calloc failed\n", __FUNCTION__);
        exit(1);
    }

    /* 初始化 */
    t_pool->max_thr_num = max_thr_num;
    t_pool->shutdown = 0;
    t_pool->queue_head = NULL;
    if (pthread_mutex_init(&t_pool->queue_lock, NULL) != 0) {
        printf("%s: pthread_mutex_init failed, errno:%d, error:%s\n",
               __FUNCTION__, errno, strerror(errno));
        exit(1);
    }
    if (pthread_cond_init(&t_pool->queue_ready, NULL) != 0) {
        printf("%s: pthread_cond_init failed, errno:%d, error:%s\n",
               __FUNCTION__, errno, strerror(errno));
        exit(1);
    }

    /* 创建工作者线程 */
    t_pool->thr_id = calloc((size_t) max_thr_num, sizeof(pthread_t));
    if (!t_pool->thr_id) {
        printf("%s: calloc failed\n", __FUNCTION__);
        exit(1);
    }
    for (i = 0; i < max_thr_num; ++i) {
        if (pthread_create(&t_pool->thr_id[i], NULL, thread_routine, NULL) != 0) {
            printf("%s:pthread_create failed, errno:%d, error:%s\n", __FUNCTION__,
                   errno, strerror(errno));
            exit(1);
        }

    }

    return 0;
}

/* 销毁线程池 */
void t_pool_destroy() {
    int i;
    t_pool_work_t *member;

    if (t_pool->shutdown) {
        return;
    }
    t_pool->shutdown = 1;

    /* 通知所有正在等待的线程 */
    pthread_mutex_lock(&t_pool->queue_lock);
    pthread_cond_broadcast(&t_pool->queue_ready);
    pthread_mutex_unlock(&t_pool->queue_lock);
    for (i = 0; i < t_pool->max_thr_num; ++i) {
        pthread_join(t_pool->thr_id[i], NULL);
    }
    free(t_pool->thr_id);

    while (t_pool->queue_head) {
        member = t_pool->queue_head;
        t_pool->queue_head = t_pool->queue_head->next;
        free(member);
    }

    pthread_mutex_destroy(&t_pool->queue_lock);
    pthread_cond_destroy(&t_pool->queue_ready);

    free(t_pool);
}

/* 向线程池添加任务 */
int t_pool_add_work(void *(*routine)(void *), void *arg) {
    t_pool_work_t *work, *member;

    if (!routine) {
        printf("%s:Invalid argument\n", __FUNCTION__);
        return -1;
    }

    work = malloc(sizeof(t_pool_work_t));
    if (!work) {
        printf("%s:malloc failed\n", __FUNCTION__);
        return -1;
    }
    work->routine = routine;
    work->arg = arg;
    work->next = NULL;

    pthread_mutex_lock(&t_pool->queue_lock);
    member = t_pool->queue_head;
    if (!member) {
        t_pool->queue_head = work;
    } else {
        while (member->next) {
            member = member->next;
        }
        member->next = work;
    }
    /* 通知工作者线程，有新任务添加 */
    pthread_cond_signal(&t_pool->queue_ready);
    pthread_mutex_unlock(&t_pool->queue_lock);

    return 0;
}