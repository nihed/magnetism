/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippoIService.h"
#include "nsCOMPtr.h"

class hippoService: public hippoIService
{
public:
    hippoService();

    NS_DECL_ISUPPORTS
    NS_DECL_HIPPOISERVICE

private:
    ~hippoService();
    
    hippoIServiceListener *listener_;
};
