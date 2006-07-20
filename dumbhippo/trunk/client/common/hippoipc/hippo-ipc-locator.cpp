/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */

#include "hippo-ipc.h"
#include <vector>

class HippoIpcLocatorMapEntry {
public:
    const char *url_;
    HippoIpcProvider *provider_;
    HippoIpcController *controller_;
    unsigned int refcount_;
};

class HippoIpcLocatorMap {
public:
    HippoIpcController *get(const char *url);
    void insert(const char *url, HippoIpcProvider *provider, HippoIpcController *controller);
    void release(HippoIpcController *controller);
    
private:
    std::vector<HippoIpcLocatorMapEntry> entries_;
};

HippoIpcController *
HippoIpcLocatorMap::get(const char *url)
{
    for (std::vector<HippoIpcLocatorMapEntry>::iterator i = entries_.begin();
         i != entries_.end();
         i++) {
        if (strcmp(i->url_, url) == 0) {
            i->refcount_++;
            return i->controller_;
        }
    }

    return (HippoIpcController *)0;
}

void
HippoIpcLocatorMap::release(HippoIpcController *controller)
{
    for (std::vector<HippoIpcLocatorMapEntry>::iterator i = entries_.begin();
         i != entries_.end();
         i++) {
        if (i->controller_ == controller) {
            i->refcount_--;
            if (i->refcount_ == 0) {
                delete i->controller_;
                delete i->provider_;
                entries_.erase(i);
            }
            return;
        }
    }
}

void 
HippoIpcLocatorMap::insert(const char         *url,
                           HippoIpcProvider   *provider,
                           HippoIpcController *controller)
{
    HippoIpcLocatorMapEntry entry;
    entry.url_ = url;
    entry.provider_ = provider;
    entry.controller_ = controller;
    entry.refcount_ = 1;

    entries_.push_back(entry);
}

HippoIpcController *
HippoIpcLocator::getController(const char *url)
{
    HippoIpcController *controller = map_->get(url);
    if (!controller) {
        HippoIpcProvider *provider = createProvider(url);
        controller = HippoIpcController::createInstance(provider);
        map_->insert(url, provider, controller);
    }

    return controller;
}

void 
HippoIpcLocator::releaseController(HippoIpcController *controller)
{
    map_->release(controller);
}

HippoIpcLocator::HippoIpcLocator()
{
    map_ = new HippoIpcLocatorMap();
}

HippoIpcLocator::~HippoIpcLocator()
{
    delete map_;
}

