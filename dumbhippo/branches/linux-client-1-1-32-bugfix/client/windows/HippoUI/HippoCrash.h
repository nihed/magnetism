/* HippoCrash.h: Crash dump management
 *
 * Copyright Red Hat, Inc. 2007
 **/

#include <hippo/hippo-basics.h>

// Called when the executable is invoked from a crash handler in reporting
// mode. Returns true if we should continue on to normal executions
bool hippoCrashReport(HippoInstanceType instance, const char *crashName);

// Initialize the exception handler 
void hippoCrashInit(HippoInstanceType instance);

// Force a dump
void hippoCrashDump();