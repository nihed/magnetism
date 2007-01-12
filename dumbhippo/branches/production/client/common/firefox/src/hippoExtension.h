/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "hippoIExtension.h"
#include "hippo-ipc.h"
#include "nsCOMPtr.h"
#include "nsStringAPI.h"

class hippoExtension : public hippoIExtension
{
public:
  NS_DECL_ISUPPORTS
  NS_DECL_HIPPOIEXTENSION

  hippoExtension();

private:
  ~hippoExtension();

  nsCString servers_;
  
protected:
  /* additional members */
};
