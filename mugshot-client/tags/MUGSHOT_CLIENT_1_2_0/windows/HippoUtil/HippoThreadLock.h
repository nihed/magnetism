/* HippoThreadLock.h: Basic wrappers for mutexes/critical sections
 *
 * Copyright Red Hat, Inc. 2006
 */
#pragma once

#include <HippoUtilExport.h>

// A straightforward wrapper around the Windows critical section object
class HippoThreadLock
{
public:
    HippoThreadLock() {
        InitializeCriticalSection(&criticalSection_);
    }

    ~HippoThreadLock() {
        DeleteCriticalSection(&criticalSection_);
    }

    void lock() {
        EnterCriticalSection(&criticalSection_);
    }

    void unlock() {
        LeaveCriticalSection(&criticalSection_);
    }

private:
    HippoThreadLock(const HippoThreadLock &other) {}
    HippoThreadLock operator=(const HippoThreadLock &other) {}

    CRITICAL_SECTION criticalSection_;
};

// This object is used to get block scoped locking; when it goes out
// of scope and is destructed, the lock will be released
class HippoWithLock
{
public:
    HippoWithLock(HippoThreadLock *lock) : lock_(lock) {
        lock_->lock();
    }

    ~HippoWithLock() {
        lock_->unlock();
    }

private:
    HippoWithLock(const HippoWithLock &other) {}
    HippoWithLock operator=(const HippoWithLock &other) {}

    HippoThreadLock *lock_;
};

// Convenience macro for declaring a block-scoped lock
#define HIPPO_WITH_LOCK(l) HippoWithLock wl__(l)
