// Based on code from:
//
// http://developer.mozilla.org/en/docs/Using_Dependent_Libraries_In_Extension_Components
//
// Which is licensed under the following terms (The "MIT License")
//
// Copyright (c) 2005 Benjamin Smedberg <benjamin@smedbergs.us>
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the "Software"),
//  to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit persons to whom the
// Software is furnished to do so, subject to the following conditions:
// 
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.

#include <stdlib.h>

#include "nscore.h"
#include "nsXPCOM.h"
#include "prlink.h"
#include "nsILocalFile.h"
#include "nsStringAPI.h"
#include "nsCOMPtr.h"

static char const *const kDependentLibraries[] =
{
    "HippoUtil.dll",
    NULL
};

static char kRealComponent[] = "HippoFirefox.dll";

extern "C" NS_EXPORT nsresult
NSGetModule(nsIComponentManager* aCompMgr,
            nsIFile* aLocation,
            nsIModule* *aResult)
{
  nsresult rv;

  // This is not the real component. We want to load the dependent libraries
  // of the real component, then the component itself, and call NSGetModule on
  // the component.

  // The stub component lives in (for example)
  // C:\Program Files\Mugshot\firefox\components. The dependent libraries and
  // the real HippoFirefox.dll can be found by backing up to 
  // C:\Program Files\Mugshot.

  nsCOMPtr<nsIFile> componentsDir;
  rv = aLocation->GetParent(getter_AddRefs(componentsDir));
  if (NS_FAILED(rv))
    return rv;

  nsCOMPtr<nsIFile> firefoxDir;
  rv = componentsDir->GetParent(getter_AddRefs(firefoxDir));
  if (NS_FAILED(rv))
    return rv;

  nsCOMPtr<nsIFile> libraries;
  rv = firefoxDir->GetParent(getter_AddRefs(libraries));
  if (NS_FAILED(rv))
    return rv;

  nsCOMPtr<nsILocalFile> library(do_QueryInterface(libraries));
  if (!library)
    return NS_ERROR_UNEXPECTED;

  library->AppendNative(NS_LITERAL_CSTRING("dummy"));

  // loop through and load dependent libraries
  for (char const *const *dependent = kDependentLibraries;
       *dependent;
       ++dependent) {
    library->SetNativeLeafName(nsDependentCString(*dependent));
    PRLibrary *lib;
    library->Load(&lib);
    // 1) We don't care if this failed!
    // 2) We are going to leak this library. We don't care about that either.
  }

  library->SetNativeLeafName(NS_LITERAL_CSTRING(kRealComponent));

  PRLibrary *lib;
  rv = library->Load(&lib);
  if (NS_FAILED(rv))
    return rv;

  nsGetModuleProc getmoduleproc = (nsGetModuleProc)
    PR_FindFunctionSymbol(lib, "NSGetModule");

  if (!getmoduleproc)
    return NS_ERROR_FAILURE;

  return getmoduleproc(aCompMgr, aLocation, aResult);
}
