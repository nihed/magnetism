//
//  HippoApp.m
//  Mugshot
//
//  Created by Dan Williams on 5/30/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//

#import "HippoApp.h"
#include "HippoPlatformImpl.h"


@implementation HippoApp

- (id)init
{
	platform = hippo_platform_impl_new (HIPPO_INSTANCE_NORMAL);
	connection = hippo_connection_new (platform);
}

@end
