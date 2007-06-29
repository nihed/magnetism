/* HippoThreadExecutor.h: Run tasks in a separate thread
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include <HippoUtilExport.h>
#include <HippoMessageHook.h>

// Interface for tasks that can be executed by a HippoThreadExecutor
class HippoThreadTask
{
public:
    // Called to execute the task
    virtual void call() = 0;
    // Called if the task is cancelled prior to execution
    virtual void cancel() = 0;
};

template <class T, typename A1>
class HippoGenericTask1 : public HippoThreadTask
{
public:
    typedef void (T::*MemberType)(A1);
    HippoGenericTask1(T *t, MemberType func, A1 a1) 
        : t_(t), func_(func), a1_(a1)
    {
    }

    virtual void call();
    virtual void cancel();
private:
    T *t_;
    MemberType func_;
    A1 a1_;
};


template <class T, typename A1>
void 
HippoGenericTask1<T,A1>::call() {
    (t_->*func_)(a1_);
}

template <class T, typename A1>
void 
HippoGenericTask1<T,A1>::cancel() 
{
}

template <class T, typename R, typename A1>
class HippoGenericTaskR1 : public HippoThreadTask
{
public:
    typedef R (T::*MemberType)(A1);
    HippoGenericTaskR1(T *t, MemberType func, A1 a1) 
        : t_(t), func_(func), a1_(a1)
    {
    }

    virtual void call();
    virtual void cancel();
    R getResult() { return r_; }
private:
    T *t_;
    MemberType func_;
    A1 a1_;
    R r_;
};


template <class T, typename R, typename A1>
void 
HippoGenericTaskR1<T,R,A1>::call() {
    r_ = (t_->*func_)(a1_);
}

template <class T, typename R, typename A1>
void 
HippoGenericTaskR1<T,R,A1>::cancel() 
{
}

// Interface for extending HippoThreadExecutor to do other things in the execution thread.
// For example, if you create a window in the init() callback, then the window procedure
// for that window will be dispatched in the execution thread.
class HippoThreadExecutorHelper {
public:
    // Called from the executor thread when it is starting up
    virtual void init() = 0;
    // Called from the executor thread before shutdown
    virtual void shutdown() = 0;
};

class HippoThreadExecutor {
public:
    static DLLEXPORT HippoThreadExecutor *createInstance(HippoThreadExecutorHelper *helper = 0);

    virtual ~HippoThreadExecutor() {}

    // Run the task synchronously and wait for completion. Note that no reentrancy is
    // permitted so if the task blocks on the calling thread for any reason, it will
    // hang. A primary example is trying to make a COM call to an object in the calling
    // thread.
    virtual void doSync(HippoThreadTask *task) = 0;

    // Run the task asynchronously. If the HippoThreadExecutor is destroyed before the
    // task executes, then the task's cancel() callback will be called.
    virtual void doAsync(HippoThreadTask *task) = 0;

    // A couple of convenience functions for calling functions synchronously without
    // having to explicitly create a helper task object
    template<class T, typename A1>
    void callSync(T *t, void (T::*func)(A1), A1 a1) {
        HippoGenericTask1<T,A1> task(t, func, a1);
        doSync(&task);
    }

    // In some cases the compiler can automatically disambiguate between this and
    // callSync, but I've had problems in other cases, so marking the return-value
    // variant explicitely seems easier.
    template<class T, typename R, typename A1>
    R callSyncR(T *t, R (T::*func)(A1), A1 a1) {
        HippoGenericTaskR1<T,R,A1> task(t, func, a1);
        doSync(&task);
        return task.getResult();
    }

    // These functions provides a way of intercepting messages for particular windows.
    // They must be called from within the executor thread; for example, within 
    // init() and shutdown().
    virtual void registerMessageHook(HWND window, HippoMessageHook *hook) = 0;
    virtual void unregisterMessageHook(HWND window) = 0;
};
