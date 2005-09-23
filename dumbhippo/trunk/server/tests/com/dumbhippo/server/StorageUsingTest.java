package com.dumbhippo.server;

import java.io.File;

import com.dumbhippo.server.util.Filesystem;

public class StorageUsingTest extends junit.framework.TestCase {
	private File storageDir;
	
	static {
		GlobalSetup.initialize();
	}
	
	protected void setUp() throws Exception {
		super.setUp();

		storageDir = new File(Filesystem.createTempDirectory("dumbhippo-test-storage"), "db");
		assertFalse(storageDir.exists());
		
		Storage.initGlobalInstance(storageDir.toString());
	}
	
	public void testDBExists() {
		assertTrue(storageDir.exists());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
		Storage.destroyGlobalInstance();
		Filesystem.recursiveDelete(storageDir, true);
		assertFalse(storageDir.exists());		
	}

}
