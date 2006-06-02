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
	needUpdate = TRUE;

	statusItem = [[[NSStatusBar systemStatusBar] statusItemWithLength:NSVariableStatusItemLength] retain];
	[statusItem setHighlightMode:YES];
	[statusItem setEnabled:YES];
	[statusItem setToolTip:@"Mugshot"];
	NSLog(@"foobar: %@", theMenu);
 	[statusItem setMenu:theMenu];

	// Grab and rescale our image
	menuIcon = [[NSImage alloc] initWithContentsOfFile:[[NSBundle mainBundle] pathForResource:@"mugshot_swarm_1" ofType:@"tiff"]];
	[menuIcon setScalesWhenResized:TRUE];
	NSSize size = {16, 16};
	[menuIcon setSize:size];
	[statusItem setImage:menuIcon];

	[statusItem setAction:@selector(updateMenu:)];
	[statusItem setTarget:self];
	
	NSBundle *bundle = [NSBundle bundleForClass:[self class]];

	app = [[HippoApp alloc] init];
}

- (void) showAboutPanel:(id)sender
{
	[[NSApplication sharedApplication] activateIgnoringOtherApps:YES];
	[[NSApplication sharedApplication] orderFrontStandardAboutPanel:sender];
}

- (void) addDefaultMenuItems
{
	NSMenuItem *item;

	// About Mugshot item
	item = [[NSMenuItem alloc] initWithTitle:@"About Mugshot" action:@selector(showAboutPanel:) keyEquivalent: nil];
	[item setTarget:self];
	[theMenu addItem: item];
}

- (IBAction) updateMenu:(id)sender
{
	int numItems = 0;

	NSLog(@"blah");
	if (!needUpdate)
		return;

	needUpdate = FALSE;

	// Remove all the old items
	numItems = [theMenu numberOfItems];
	while (numItems > 0)
	{
		[theMenu removeItemAtIndex: 0];
		numItems--;
	}

	[self addDefaultMenuItems];
	NSLog(@"foobar: %@", theMenu);
}

@end
