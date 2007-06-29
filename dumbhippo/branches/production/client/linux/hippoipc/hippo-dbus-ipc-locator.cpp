#include "hippo-dbus-ipc-locator.h"
#include "hippo-dbus-ipc-provider.h"

class HippoDBusIpcLocatorImpl : public HippoDBusIpcLocator
{
public:
    virtual HippoIpcController *createController(const char *url);
};

HippoDBusIpcLocator *
HippoDBusIpcLocator::getInstance()
{
    static HippoDBusIpcLocator *instance;

    if (!instance)
	instance = new HippoDBusIpcLocatorImpl();

    return instance;
}

HippoIpcController *
HippoDBusIpcLocatorImpl::createController(const char *url)
{
    HippoIpcProvider *provider = HippoDBusIpcProvider::createInstance(url);
    HippoIpcController *controller = HippoIpcController::createInstance(provider);
    provider->unref();

    return controller;
}

