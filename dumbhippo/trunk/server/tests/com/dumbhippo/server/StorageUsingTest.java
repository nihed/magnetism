package com.dumbhippo.server;

import java.io.File;
import java.io.IOException;

import com.dumbhippo.Filesystem;
import com.dumbhippo.persistence.GlobalSetup;
import com.dumbhippo.persistence.Storage;
import com.dumbhippo.persistence.Storage.SessionWrapper;

public class StorageUsingTest extends junit.framework.TestCase {
	private File storageDir;
	
	protected StorageUsingTest() {
		GlobalSetup.initializeBase();

	}
	
	private void initTempStorage() throws IOException {
		File tempdir = Filesystem.createTempDirectory("dumbhippo-test-storage");
		assertTrue(tempdir.exists());
		storageDir = new File(tempdir, "db");
		assertFalse(storageDir.exists());
		Storage.initGlobalInstance(storageDir.toString());		
	}

	protected void setUp() throws Exception {
		super.setUp();
		
		initTempStorage();
		GlobalSetup.initializeStorage();		
	}
	
	protected SessionWrapper getSession() {
		return getStorage().getPerThreadSession();
	}
	
	protected Storage getStorage() {
		return Storage.getGlobalInstance();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		
		Storage.destroyGlobalInstance();
		Filesystem.recursiveDelete(storageDir, true);
		assertFalse(storageDir.exists());		
	}

}
