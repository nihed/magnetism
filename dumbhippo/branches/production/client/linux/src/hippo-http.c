/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <curl/curl.h>
#include <curl/types.h>
#include <curl/easy.h>
#include <glib.h>
#include <glib/gi18n-lib.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include "hippo-http.h"

static GThreadPool *pool = NULL;
static int active_task_count = 0;
G_LOCK_DEFINE(completed_queue);
static GSList *completed_tasks = NULL;
#define WRITE_END 1
#define READ_END 0
static int pipe_fds[2];
static guint pipe_io_watch = 0;

typedef struct {
    char *url;
    HippoHttpFunc func;
    void *data;
    GString *buffer;
    char *content_type;
    gboolean failed;
} Task;

static Task*
task_new(const char   *url,
         HippoHttpFunc func,
         void         *data)
{
    Task *task;
    
    task = g_new0(Task, 1);
    
    task->url = g_strdup(url);
    task->func = func;
    task->data = data;
    task->buffer = g_string_new(NULL);
    task->failed = FALSE;
    
    return task;
}

static void
task_free(Task *task)
{
    g_string_free(task->buffer, TRUE);
    g_free(task->content_type);
    g_free(task->url);
    g_free(task);
}

static size_t
curl_write_func(void *ptr, size_t elem_size, size_t n_elems, void *data)
{
    Task *task = data;
    
    g_string_append_len(task->buffer, ptr, elem_size * n_elems);
    
    return elem_size * n_elems;
}

static size_t
curl_read_func(void *ptr, size_t elem_size, size_t n_elems, FILE *stream)
{
    return fread(ptr, elem_size, n_elems, stream);
}

static int
curl_progress_func(void  *data,
                   double download_total,
                   double download_current,
                   double upload_total,
                   double upload_current)
{
    return 0;
}

static void
do_error(Task       *task,
         const char *message)
{
    task->failed = TRUE;
    /* clear any data we got */
    g_string_truncate(task->buffer, 0);
    /* replace it with error message */
    g_string_append(task->buffer, message);
}

static void
do_task(void *task_ptr,
        void *pool_data)
{
    Task *task = task_ptr;
    CURL *curl;
    int result;
    
    curl = curl_easy_init();
    if (curl == NULL) {
        do_error(task, "Could not initialize http library");
    } else {
        CURLcode result;
        
        /* Note that curl doesn't copy the stuff passed into it (e.g. the url) */
        curl_easy_setopt(curl, CURLOPT_URL, task->url);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, task);
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, curl_write_func);
        curl_easy_setopt(curl, CURLOPT_READFUNCTION, curl_read_func);
        curl_easy_setopt(curl, CURLOPT_NOPROGRESS, FALSE);
        curl_easy_setopt(curl, CURLOPT_PROGRESSFUNCTION, curl_progress_func);
        curl_easy_setopt(curl, CURLOPT_PROGRESSDATA, task);

        result = curl_easy_perform(curl);
        
        if (result != CURLE_OK) {
            do_error(task, curl_easy_strerror(result));
        } else {
            long response_code = 404;

#define RESPONSE_CODE_OK 200
#define RESPONSE_CODE_PERM_REDIRECT 301
#define RESPONSE_CODE_TEMP_REDIRECT 302

            result = curl_easy_getinfo(curl, CURLINFO_RESPONSE_CODE,
                                       &response_code);
            if (result != CURLE_OK) {
                do_error(task, curl_easy_strerror(result));
            } else if (response_code != RESPONSE_CODE_OK) {
                char *s = g_strdup_printf("HTTP error code: %ld", response_code);
                do_error(task, s);
                g_free(s);
            } else {
                const char *content_type = NULL;
                
                result = curl_easy_getinfo(curl,
                    CURLINFO_CONTENT_TYPE, &content_type);
                if (result != CURLE_OK) {
                    do_error(task, curl_easy_strerror(result));
                } else if (content_type == NULL) {
                    do_error(task, _("No content type received"));
                } else {
                    task->content_type = g_strdup(content_type);
                }
            }
        }
    }
    
    curl_easy_cleanup(curl);
    
    G_LOCK(completed_queue);
    completed_tasks = g_slist_prepend(completed_tasks, task);
    G_UNLOCK(completed_queue);

    /* Notify the main thread */
    while (TRUE) {
        result = write(pipe_fds[WRITE_END], "b", 1);
        if (result == 0 || (result < 0 && errno == EINTR)) {
            continue;
        } else {
            if (result < 0)
                g_warning("Failed to write byte to pipe: %s", strerror(errno));
            break;
        }
    }
}

/* This runs in the main thread */
static void
process_completed_queue(void)
{
    GSList *tasks;
    
    G_LOCK(completed_queue);    
    tasks = completed_tasks;
    completed_tasks = NULL;
    G_UNLOCK(completed_queue);
 
    while (tasks != NULL) {
        Task *task = tasks->data;
        tasks = g_slist_remove(tasks, tasks->data);
        
        /* g_debug("Completing http GET for '%s'", task->url); */
        
        /* buffer is the error message if we failed and the data otherwise */
        if (task->failed)
            (*task->func)(NULL, task->buffer, task->data);
        else
            (*task->func)(task->content_type, task->buffer, task->data);
            
        task_free(task);
        active_task_count -= 1;
    }

    if (active_task_count == 0) {
        g_debug("Global http subsystem uninit");
        
        /* args are immediate=FALSE (don't cancel tasks)
         * wait=TRUE (block until they complete)
         * however there should be no tasks, so it doesn't matter.
         */
        g_thread_pool_free(pool, FALSE, TRUE);
        pool = NULL;
    
        curl_global_cleanup();
        
        g_source_remove(pipe_io_watch);
        pipe_io_watch = 0;
        
        close(pipe_fds[READ_END]);
        close(pipe_fds[WRITE_END]);
    }
}

static gboolean
pipe_read_callback(GIOChannel   *channel,
                   GIOCondition  condition,
                   void         *data)
{
    int fd;
    char buf[32]; /* try to read everything pending at once */
 
    /* g_debug("Got data on HTTP thread notification pipe"); */
    
    fd = g_io_channel_unix_get_fd(channel);
    
    if (read(fd, buf, sizeof(buf)) > 0) {
        process_completed_queue();
    }
    
    return TRUE;
}

void
hippo_http_get(const char   *url,
               HippoHttpFunc func,
               void         *data)
{
    Task *task;
    GError *error;
    
    if (active_task_count == 0) {
        GIOChannel *pipe_read_channel;
        
        g_debug("Global http subsystem init");
        
        if (pipe(pipe_fds) < 0) {
            /* should not happen in any reasonable scenario... */
            GString *str;  
            g_warning("Could not create pipe: %s", strerror(errno));
            str = g_string_new("Failed to create pipe");
            (* func)(NULL, str, data);
            g_string_free(str, TRUE);
            return;
        }

        pipe_read_channel = g_io_channel_unix_new(pipe_fds[READ_END]);
        pipe_io_watch = g_io_add_watch(pipe_read_channel, G_IO_IN, pipe_read_callback, NULL);
        g_io_channel_unref(pipe_read_channel);
        
        /* not passing in the SSL flag, we don't need SSL 
         * and we'd have to do openssl's special thread setup
         * whatever that is, according to curl docs 
         */
        curl_global_init(0);

        error = NULL;
        pool = g_thread_pool_new(do_task, NULL, 8, FALSE, &error);
        if (pool == NULL) {
            g_error("Can't create thread pool: %s", error->message);
            g_error_free(error); /* not reached */
        }        
    }

    g_debug("Starting new http GET task for '%s'", url);

    task = task_new(url, func, data);

    error = NULL;
    g_thread_pool_push(pool, task, &error);
    if (error != NULL) {
        g_error("Can't create a new thread: %s", error->message);
        g_error_free(error); /* not reached */
    }
    
    active_task_count += 1;
}

#ifdef TEST_HIPPO_HTTP

/*
 To build this, for example:

   cc -Wall -g -DTEST_HIPPO_HTTP -I/home/hp/workspace/trunk/client/linux/build/config -I/home/hp/workspace/trunk/client/common -lcurl `pkg-config --libs --cflags gthread-2.0`  hippo-http.c -o testhttp
   
*/

static GMainLoop *loop = NULL;
static int total = 50;
static int outstanding = 0;
static const char *sample_urls[] = {
    "http://lwn.net/images/lcorner.png",
    "http://www.gnome.org",
    "http://boingboing.net", 
    "http://slashdot.org", 
    "http://nyt.com",
    "http://cnn.com", 
    "http://google.com"
};

static void
print_http_result(const char *content_type,
                  GString    *content_or_error,
                  void       *data);

static void
add_one(void)
{
    int i;
    
    g_assert(total > 0);
    
    i = total % G_N_ELEMENTS(sample_urls);
    g_assert(i < G_N_ELEMENTS(sample_urls));
    g_print("Starting outstanding %d to url: %s\n", outstanding + 1, sample_urls[i]);
    hippo_http_get(sample_urls[i], print_http_result, sample_urls[i]);
    ++outstanding;
    --total;
}

static void
queue_requests(void)
{
    /* we want to try to let outstanding get down to 0, to test global 
     * init/uninit
     */
    if (outstanding == 0) {
        while (total > 0 && outstanding < 8) {
            add_one();
        }
        g_print("%d more to queue, %d outstanding now\n", total, outstanding);
    }
    
    if (total == 0 && outstanding == 0) {
        g_main_loop_quit(loop);
    }
}

static gboolean
queue_requests_idle(void *data)
{
    queue_requests();
    return FALSE;
}

static void
print_http_result(const char *content_type,
                  GString    *content_or_error,
                  void       *data)
{
    const char *url = data;
    
    if (content_type == NULL) {
        g_printerr("URL: %s\n", url);
        g_printerr("Failed: %s\n", content_or_error->str);
    } else {
        g_print("URL: %s\n", url);    
        g_print("Content-Type: %s\n", content_type);
        if (g_utf8_validate(content_or_error->str, content_or_error->len, NULL)) {
            /* g_print("%s", content_or_error->str); */
            g_print("%d bytes of valid UTF-8\n", content_or_error->len);
        } else {
            g_print("%d bytes of binary data\n", content_or_error->len);
        }
    }
    
    --outstanding;
    
    /* This ensures the http system gets down to 0 pending requests, 
     * to test init/uninit 
     */
    g_idle_add(queue_requests_idle, NULL);
}

int
main(int argc, char **argv)
{   
    g_thread_init(NULL);
    
    loop = g_main_loop_new(NULL, FALSE);
    
    queue_requests();
    
    g_main_loop_run(loop);
    
    g_main_loop_unref(loop); 
    
    return 0;
}

#endif
