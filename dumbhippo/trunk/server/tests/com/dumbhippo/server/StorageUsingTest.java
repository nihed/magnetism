package com.dumbhippo.server;

import java.io.File;

import com.dumbhippo.Filesystem;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

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
	
	protected SessionWrapper getSession() {
		return Storage.getGlobalPerThreadSession();
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
