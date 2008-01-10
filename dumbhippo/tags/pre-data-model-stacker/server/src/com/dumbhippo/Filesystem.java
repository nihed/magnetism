package com.dumbhippo;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

/*
 * Class with miscellaneous file utilites that should have been in java.io.File
 * @author walters
 */
public class Filesystem {
	    /**
	     * Create a new, empty temporary directory
	     * 
	     * @param dir base temporary directory
	     * @param prefix prefix for new directory name
	     * @param suffix suffix for new directory name
	     * @return new temporary directory
	     * @throws IOException if too many attempts fail
	     */
	    public static File createTempDirectory(File dir, String prefix, String suffix) throws IOException {
	    	Random random = new SecureRandom();	    	
	    	for (int attempts = 0; attempts < 1000; attempts++) {
	    		String name = prefix + random.nextLong() + suffix;
	    		File newtempd = new File(dir, name);
	    		if (newtempd.mkdir()) {
	    			return newtempd;	
	    		}
	    	}
	    	throw new IOException("Couldn't create temporary directory in " + dir);
	    }
	    
	    public static File createTempDirectory(String prefix) throws IOException {
	    	return createTempDirectory(new File(System.getProperty("java.io.tmpdir")), prefix, "");
	    }
	    
	    /**
	     * Recursively deletes a directory (or file)
	     * @param file a file or directory to delete, recursively if necessary
	     * @param ignoreErrors whether or not to ignore the return value of File.delete
	     */
	    public static void recursiveDelete(File file, boolean ignoreErrors) throws IOException {
	        if (file.isDirectory()) {
	            String[] list = file.list();
	            for (int i=0; i < list.length; i++) {
	                recursiveDelete(new File(file, list[i]), ignoreErrors);
	            }
	        }
	        if (!file.delete() && !ignoreErrors)
	        	throw new IOException("Failed to delete " + file);
	    }	    
}
