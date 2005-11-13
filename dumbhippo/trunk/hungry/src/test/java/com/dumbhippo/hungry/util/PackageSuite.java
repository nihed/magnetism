package com.dumbhippo.hungry.util;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PackageSuite extends TestSuite {
	
	public PackageSuite(String name, Class clazz) {
		super(name);
		
		Package pack = clazz.getPackage();
		String packName = pack.getName();
		String packPath = packName;

		if (!packPath.startsWith("/")) {
			packPath = "/" + packPath;
		}

		packPath = packPath.replace('.', '/');
		
		URL packUrl = clazz.getResource(packPath);
		
		File packDir = new File(packUrl.getPath());
		
		if (!packDir.exists())
			throw new IllegalStateException("Directory " + packDir + " does not exist");
		
		File[] files = packDir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".class");
			}
		});
		
		for (File f : files) {
			String type = packName + "." + f.getName().substring(0, f.getName().length() - 6);
			
			Class c = null;
			try {
				c = Class.forName(type);
			} catch (ClassNotFoundException e) {
				System.err.println("Can't load: " + type);
				System.exit(1);
			}
			
			if (TestCase.class.isAssignableFrom(c)) {
				System.out.println("Adding test case: " + c.getCanonicalName());
				addTest(new TestSuite(c));
			}
		}
	}
}
