/* -*- mode: C; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include <config.h>
#include <glib/gi18n-lib.h>
#include <glib.h>
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <errno.h>
#include "hippo-dbus-async.h"

static GThreadPool *pool = NULL;
static int active_task_count = 0;
G_LOCK_DEFINE(completed_queue);
static GSList *completed_tasks = NULL;
#define WRITE_END 1
#define READ_END 0
static int pipe_fds[2];
static guint pipe_io_watch = 0;

typedef struct {
    char *address;
    DBusConnection *opened;
    DBusError error;
    HippoDBusConnectionOpenedHandler handler;
    void *data;
} Task;

static Task*
task_new(const char                      *address,
         HippoDBusConnectionOpenedHandler handler,
         void                            *data)
{
    Task *task;
    
    task = g_new0(Task, 1);
    
    task->address = g_strdup(address);
    task->handler = handler;
    task->data = data;
    task->opened = NULL;
    dbus_error_init(&task->error);
    
    return task;
}

static void
task_free(Task *task)
{
    dbus_error_free(&task->error);
    if (task->opened)
        dbus_connection_unref(task->opened);
    g_free(task->address);
    g_free(task);
}

static void
do_task(void *task_ptr,
        void *pool_data)
{
    Task *task = task_ptr;

    g_assert(task->opened == NULL);
    task->opened = dbus_connection_open_private(task->address,
                                                &task->error);    
    
    G_LOCK(completed_queue);
    completed_tasks = g_slist_prepend(completed_tasks, task);
    G_UNLOCK(completed_queue);

    /* Notify the main thread */
    while (TRUE) {
        int result;
        
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
        
        if (task->opened == NULL) {
            g_assert(dbus_error_is_set(&task->error));
            (*task->handler)(NULL, &task->error, task->data);
        } else {
            (*task->handler)(task->opened, NULL, task->data);
        }
        
        task_free(task);
        active_task_count -= 1;
    }

    if (active_task_count == 0) {
        g_debug("Global dbus async subsystem uninit");
        
        /* args are immediate=FALSE (don't cancel tasks)
         * wait=TRUE (block until they complete)
         * however there should be no tasks, so it doesn't matter.
         */
        g_thread_pool_free(pool, FALSE, TRUE);
        pool = NULL;
        
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
hippo_dbus_connection_open_private_async (const char                      *address,
                                          HippoDBusConnectionOpenedHandler handler,
                                          void                            *data)
{
    Task *task;
    GError *error;
    
    if (active_task_count == 0) {
        GIOChannel *pipe_read_channel;
        
        g_debug("Global async dbus subsystem init");
        
        if (pipe(pipe_fds) < 0) {
            /* should not happen in any reasonable scenario... */
            DBusError error;
            g_warning("Could not create pipe: %s", strerror(errno));
            dbus_error_init(&error);
            dbus_set_error(&error, DBUS_ERROR_FAILED, "Failed to create pipe");
            (* handler)(NULL, &error, data);
            dbus_error_free(&error);
            return;
        }

        pipe_read_channel = g_io_channel_unix_new(pipe_fds[READ_END]);
        pipe_io_watch = g_io_add_watch(pipe_read_channel, G_IO_IN, pipe_read_callback, NULL);
        g_io_channel_unref(pipe_read_channel);

        error = NULL;
        pool = g_thread_pool_new(do_task, NULL, 8, FALSE, &error);
        if (pool == NULL) {
            g_error("Can't create thread pool: %s", error->message);
            g_error_free(error); /* not reached */
        }
    }

    g_debug("Starting new connection open task for '%s'", address);

    task = task_new(address, handler, data);

    error = NULL;
    g_thread_pool_push(pool, task, &error);
    if (error != NULL) {
        g_error("Can't create a new thread: %s", error->message);
        g_error_free(error); /* not reached */
    }
    
    active_task_count += 1;
}
