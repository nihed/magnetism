//
//  MugshotMenulet.h
//  Mugshot
//
//  Created by Dan Williams on 5/30/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#include <hippo/hippo-common.h>
#import "HippoApp.h"

@interface MugshotMenulet : NSObject {
	NSStatusItem *statusItem;
	NSImage *menuIcon;
	HippoApp *app;

	IBOutlet NSMenu *theMenu;
	IBOutlet NSMenuItem *aboutItem;
}

- (IBAction) showAboutPanel:(id)sender;
@end
