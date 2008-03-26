//
//  main.m
//  Mugshot
//
//  Created by Dan Williams on 5/30/06.
//  Copyright __MyCompanyName__ 2006. All rights reserved.
//

#import <Cocoa/Cocoa.h>
#include <glib.h>

int main(int argc, char *argv[])
{
	g_thread_init(NULL);
	g_type_init ();
	return NSApplicationMain(argc,  (const char **) argv);
}
