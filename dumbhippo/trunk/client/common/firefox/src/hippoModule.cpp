/* -*- mode: C++; c-basic-offset: 4; indent-tabs-mode: nil; -*- */
#include "nspr.h"
#include "nsCOMPtr.h"
#include "nsEmbedString.h"
#include "nsXPCOMCID.h"

#include "nsICategoryManager.h"
#include "nsIGenericFactory.h"
#include "nsIScriptNameSpaceManager.h"
#include "nsIServiceManager.h"
#include "nsISupportsUtils.h"
#include "nsServiceManagerUtils.h"

#include "hippoService.h"

NS_GENERIC_FACTORY_CONSTRUCTOR(hippoService);
NS_DECL_CLASSINFO(hippoService);

static NS_METHOD 
registerGlobalConstructors(nsIComponentManager *aCompMgr,
                           nsIFile *aPath,
                           const char *registryLocation,
                           const char *componentType,
                           const nsModuleComponentInfo *info)
{
    nsresult rv = NS_OK;

    nsCOMPtr<nsICategoryManager> catman = do_GetService(NS_CATEGORYMANAGER_CONTRACTID, &rv);
    if (NS_FAILED(rv))
	return rv;

    nsCString previous;
    rv = catman->AddCategoryEntry(JAVASCRIPT_GLOBAL_CONSTRUCTOR_CATEGORY,
				  "HippoService",
				  HIPPO_SERVICE_CONTRACTID,
				  PR_TRUE, PR_TRUE, getter_Copies(previous));
    
    NS_ENSURE_SUCCESS(rv, rv);

    return rv;
}

static const nsModuleComponentInfo components[] = {
    { 
	"Mugshot Service",
	HIPPO_SERVICE_CID,
	HIPPO_SERVICE_CONTRACTID,
	hippoServiceConstructor,
	registerGlobalConstructors,
	NULL, // mFactoryDestrucrtor
	NULL, // mGetInterfacesProcPtr
	NS_CI_INTERFACE_GETTER_NAME(hippoService),
	NULL, // mGetLanguageHelperProc
	&NS_CLASSINFO_NAME(hippoService),
	nsIClassInfo::DOM_OBJECT
    }
};

NS_IMPL_NSGETMODULE(hippoModule, components)
