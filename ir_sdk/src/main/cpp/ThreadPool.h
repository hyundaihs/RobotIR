//
// Created by Kevin on 16/12/14.
//

#ifndef HYL026_THREADPOOL_H
#define HYL026_THREADPOOL_H

#include <pthread.h>

/* 要执行的任务链表 */

typedef struct t_pool_work {


    void *(*routine)(void *);

    /* 任务函数 */
    void *arg;
    /* 传入任务函数的参数 */

    struct t_pool_work *next;

} t_pool_work_t;


typedef struct t_pool {
    int shutdown;
    /* 线程池是否销毁 */
    int max_thr_num;
    /* 最大线程数 */
    pthread_t *thr_id;
    /* 线程ID数组 */
    t_pool_work_t *queue_head;
    /* 线程链表 */
    pthread_mutex_t queue_lock;
    pthread_cond_t queue_ready;

} t_pool_t;

/*
 * @brief     创建线程池
 * @param     max_thr_num 最大线程数
 * @return     0: 成功 其他: 失败
 */
int t_pool_create(int max_thr_num);

/*
 * @brief     销毁线程池
 */
void t_pool_destroy();

/*
* @brief     向线程池中添加任务
* @param    routine 任务函数指针
* @param     arg 任务函数参数
* @return     0: 成功 其他:失败
*/
int t_pool_add_work(void *(*routine)(void *), void *arg);

#endif //HYL026_THREADPOOL_H
