//
//  HippoApp.h
//  Mugshot
//
//  Created by Dan Williams on 5/30/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#import <hippo/hippo-common.h>


@interface HippoApp : NSObject {
	HippoPlatform *platform;
	HippoConnection *connection;
}

-(id)init;

@end
