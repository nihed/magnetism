package com.dumbhippo;

import java.io.File;

import junit.framework.TestCase;

import com.dumbhippo.Filesystem;

public class FilesystemTests extends TestCase {

	/*
	 * Test method for 'com.dumbhippo.Filesystem.createTempDirectory(String)'
	 */
	public void testCreateTempDirectoryString() throws Exception {
		File tmpdir = Filesystem.createTempDirectory("foobar");
		assertTrue(tmpdir.getName().startsWith("foobar"));		
		assertTrue(tmpdir.exists());
		Filesystem.recursiveDelete(tmpdir, true);
	}

	/*
	 * Test method for 'com.dumbhippo.Filesystem.recursiveDelete(File, boolean)'
	 */
	public void testRecursiveDelete() throws Exception {
		File tmpdir = Filesystem.createTempDirectory("foobar");
		assertTrue(tmpdir.exists());
		Filesystem.recursiveDelete(tmpdir, false);
		assertFalse(tmpdir.exists());		
	}

}
