//
//  MugshotMenulet.m
//  Mugshot
//
//  Created by Dan Williams on 5/30/06.
//  Copyright 2006 __MyCompanyName__. All rights reserved.
//

#import "MugshotMenulet.h"
#include "HippoPlatformImpl.h"

@implementation MugshotMenulet
-(void)dealloc
{
	[statusItem release];
	[super dealloc];
}

- (void)awakeFromNib
{
	[aboutItem setAction:@selector(showAboutPanel:)];
	[aboutItem setTarget:self];

	statusItem = [[[NSStatusBar systemStatusBar] statusItemWithLength:NSSquareStatusItemLength] retain];
	[statusItem setHighlightMode:YES];
	[statusItem setEnabled:YES];
	[statusItem setToolTip:@"Mugshot"];
 	[statusItem setMenu:theMenu];

	// Grab and rescale our image
	menuIcon = [[NSImage alloc] initWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"mugshot_swarm_1" ofType:@"tiff"]];
	[menuIcon setScalesWhenResized:TRUE];
	NSSize size = {16, 16};
	[menuIcon setSize:size];
	[statusItem setImage:menuIcon];

	NSBundle *bundle = [NSBundle bundleForClass:[self class]];

	app = [[HippoApp alloc] init];
}

- (IBAction) showAboutPanel:(id)sender
{
	[[NSApplication sharedApplication] activateIgnoringOtherApps:YES];
	[[NSApplication sharedApplication] orderFrontStandardAboutPanel:sender];
}

@end
