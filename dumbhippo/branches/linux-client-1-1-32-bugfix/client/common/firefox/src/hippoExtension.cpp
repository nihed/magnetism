/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "nspr.h"
#include "nsMemory.h"
#include "nsNetCID.h"
#include "nsISupportsUtils.h"
#include "hippoExtension.h"

NS_IMPL_ISUPPORTS1(hippoExtension, hippoIExtension)

hippoExtension::hippoExtension()
{
  /* member initializers and constructor code */
}

hippoExtension::~hippoExtension()
{
  /* destructor code */
}

/* readonly attribute AUTF8String serverUrls; */
NS_IMETHODIMP hippoExtension::GetServers(nsACString & aServers)
{
    aServers.Assign(servers_);
    return NS_OK;
}

/* void start (in AUTF8String serverUrls); */
NS_IMETHODIMP hippoExtension::Start(const nsACString & servers)
{
    servers_.Assign(servers);
    return NS_OK;
}
