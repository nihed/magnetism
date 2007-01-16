/* HippoCrash.h: Crash dump management
 *
 * Copyright Red Hat, Inc. 2007
 **/

// Called when the executable is invoked from a crash handler in reporting
// mode. Returns true if we should continue on to normal executions
bool hippoCrashReport(const char *filename);

// Initialize the exception handler 
void hippoCrashInit();

// Force a dump
void hippoCrashDump();