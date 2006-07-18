#include "hippoipc/hippo-ipc.h"

class HippoDBusIpcProvider : public HippoIpcProvider
{
public:
    static HippoDBusIpcProvider *createInstance(const char *serverName);
};
