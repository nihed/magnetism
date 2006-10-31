/* HippoThreadExecutor.cpp: Run tasks in a separate thread
 *
 * Copyright Red Hat, Inc. 2006
 */
#include "stdafx-hippoutil.h"
#include <vector>

#include "HippoUtil.h"
#include "HippoThreadExecutor.h"
#include "HippoThreadLock.h"

class HippoThreadTaskInfo 
{
public:
    HippoThreadTaskInfo(HippoThreadTask *task, HANDLE event = 0) {
        task_ = task;
        event_ = event;
    }
    HANDLE getEvent() { return event_; }
    HippoThreadTask *getTask() { return task_; }

private:
    HippoThreadTask *task_;
    HANDLE event_;
};

class HippoThreadExecutorImpl : public HippoThreadExecutor
{
public:
    HippoThreadExecutorImpl(HippoThreadExecutorHelper *helper);
    ~HippoThreadExecutorImpl();
    
    virtual void doSync(HippoThreadTask *task);
    virtual void doAsync(HippoThreadTask *task);

    virtual void registerMessageHook(HWND window, HippoMessageHook *hook);
    virtual void unregisterMessageHook(HWND window);

private:
    HippoThreadExecutorHelper *helper_;
    HANDLE thread_; // The worker thread
    HANDLE workSemaphore_; // Signaled when haveWork has been set

    HippoThreadLock lock_; // Protects the following variables
    bool haveWork_; // Indicates that either we have tasks to do shouldExit_ is set
    bool shouldExit_; // Done, worker thread should unregister and exit

    std::vector<HippoThreadTaskInfo> tasks_;
    std::vector<HANDLE> freeEvents_; // A cache of unused event objects
    HippoMessageHookList hooks_;

    void setHaveWork();
    bool processWork();

    void run();

    static DWORD WINAPI threadProc(void *data);
};

HippoThreadExecutor *
HippoThreadExecutor::createInstance(HippoThreadExecutorHelper *helper)
{
    return new HippoThreadExecutorImpl(helper);
}

HippoThreadExecutorImpl::HippoThreadExecutorImpl(HippoThreadExecutorHelper *helper)
{
    helper_ = helper;

    workSemaphore_ = CreateSemaphore(NULL, 0, MAXLONG, NULL);
    haveWork_ = false;
    shouldExit_ = false;

    thread_ = CreateThread(NULL, 0, threadProc, this, 0, NULL);
}

HippoThreadExecutorImpl::~HippoThreadExecutorImpl()
{
    {
        HIPPO_WITH_LOCK(&lock_);

        shouldExit_ = true;
        setHaveWork();
    }

    // It's important not to hang indefinitely, but killing our thread
    // is impolite and will leak a small amount of memory. So we wait
    // 250 milliseconds and if that fails, kill it explicitly
    LONG result = WaitForSingleObject(thread_, 250);
    if (result == WAIT_TIMEOUT || result == WAIT_FAILED) {
        TerminateThread(thread_, 0);
    }

    CloseHandle(thread_);
    CloseHandle(workSemaphore_);

    while (!freeEvents_.empty()) {

        CloseHandle(freeEvents_.back());
        freeEvents_.pop_back();
    }
}

void 
HippoThreadExecutorImpl::doSync(HippoThreadTask *task)
{
    HANDLE event;

    lock_.lock();

    if (shouldExit_) {
        lock_.unlock();
        task->cancel();
        return;
    }

    // In order to execute a task synchronously, we associate an event
    // object with the task; when the executor thread processes the
    // task, it signals the event object and wakes us back up.
    if (!freeEvents_.empty()) {
        event = freeEvents_.back();
        freeEvents_.pop_back();
    } else
        event = CreateEvent(NULL, FALSE, FALSE, NULL);

    tasks_.push_back(HippoThreadTaskInfo(task, event));
    setHaveWork();

    lock_.unlock();
    WaitForSingleObject(event, INFINITE);
    lock_.lock();

    freeEvents_.push_back(event);
    lock_.unlock();
}

void 
HippoThreadExecutorImpl::doAsync(HippoThreadTask *task)
{
    lock_.lock();

    if (shouldExit_) {
        lock_.unlock();
        task->cancel();
        return;
    }

    tasks_.push_back(HippoThreadTaskInfo(task));
    setHaveWork();
    lock_.unlock();
}

void 
HippoThreadExecutorImpl::registerMessageHook(HWND window, HippoMessageHook *hook)
{
    hooks_.registerMessageHook(window, hook);
}

void 
HippoThreadExecutorImpl::unregisterMessageHook(HWND window)
{
    hooks_.unregisterMessageHook(window);
}

void
HippoThreadExecutorImpl::setHaveWork()
{
    if (!haveWork_) {
        haveWork_ = true;
        // Releasing the semaphore wakes up the work thread
        ReleaseSemaphore(workSemaphore_, 1, NULL);
    }
}

bool
HippoThreadExecutorImpl::processWork()
{
    lock_.lock();

    haveWork_ = false;

    // We copy the pending tasks with the lock held, then release the
    // lock before actually processing them
    std::vector<HippoThreadTaskInfo> toProcess = tasks_;
    tasks_.clear();
    bool shouldExit = shouldExit_;

    lock_.unlock();

    for (std::vector<HippoThreadTaskInfo>::iterator i = toProcess.begin();
         i != toProcess.end();
         i++) 
    {
        if (shouldExit)
            i->getTask()->cancel();
        else
            i->getTask()->call();
        if (i->getEvent()) 
            SetEvent(i->getEvent());

        // Check shouldExit again to reduce (but not eliminate) the probability 
        // that we will call a task after beginning to shut down.
        lock_.lock();
        bool shouldExit = shouldExit_;
        lock_.unlock();

    }

    return shouldExit;
 }
 
void
HippoThreadExecutorImpl::run()
{
    if (helper_)
        helper_->init();

    // Now loop processing updates and notifications

    while (true) {
        // We always call MsgWaitForMultipleObjects and look for messages, even though we
        // might be used in a case where only the incoming task semaphore is in use,
        // and we could just use WaitForSingleObject. I think the efficiency difference is 
        // small, but it's conceivable that the call MsgWaitForMultipleObjects actually will 
        // cause the creation of a message queue when none is needed; even if that happens,
        // the overhead should be within reason.
        MSG message;
        while (PeekMessage(&message, NULL, 0, 0, TRUE)) {
            if (!hooks_.processMessage(&message)) {
                TranslateMessage(&message);
                DispatchMessage(&message);
            }
        }
        DWORD result = MsgWaitForMultipleObjects(1, &workSemaphore_, FALSE, INFINITE, QS_ALLINPUT);
        if (result == WAIT_OBJECT_0) { // Our semaphore was triggered
            if (processWork())
                break;
        } else if (result == WAIT_OBJECT_0 + 1) { // Received a message
            // Do nothing, we check before waiting the next time to deal with the case that
            // processWork() might peek a message from the queue but not remove it.
        } else {
            hippoDebugLogW(L"Unexpected result from MsgWaitForMultipleObjects");
            break;
        }
    }

    if (helper_)
        helper_->shutdown();
}

DWORD WINAPI
HippoThreadExecutorImpl::threadProc(void *data) 
{
    HippoThreadExecutorImpl *executor = (HippoThreadExecutorImpl *)data;
    executor->run();

    return 0;
}
