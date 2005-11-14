package com.dumbhippo.hungry.util;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.util.ArrayList;

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
		
		ArrayList<Class<?>> tests = new ArrayList<Class<?>>();
		
		for (File f : files) {
			String type = packName + "." + f.getName().substring(0, f.getName().length() - 6);
			
			Class<?> c = null;
			try {
				c = Class.forName(type);
			} catch (ClassNotFoundException e) {
				System.err.println("Can't load: " + type);
				System.exit(1);
			}
			
			if (TestCase.class.isAssignableFrom(c)) {
				System.out.println("Found test case: " + c.getCanonicalName());
				tests.add(c);
			}
		}
		
		long start = System.currentTimeMillis();
		
		// inefficiency rulez
		ArrayList<Class<?>> sortedTests = new ArrayList<Class<?>>();
		while (!tests.isEmpty()) {
			Class<?> c = tests.remove(0);
			OrderAfter orderAfter = c.getAnnotation(OrderAfter.class);
			
			if (orderAfter != null) {
				Class<?> before = orderAfter.value();
				
				System.out.println("Ordering test case " + c.getName() + " after " + before.getName());
				
				int i = sortedTests.indexOf(before);
				if (i >= 0) {
					sortedTests.add(i+1, c);
				} else {
					i = tests.indexOf(before);
					if (i < 0) {
						throw new RuntimeException("Class " + c.getName() + " ordered after " + before.getName() + " but " + before.getName() + " is not in the test suite");
					}
					tests.remove(i);
					sortedTests.add(before);
					sortedTests.add(c);
				}
			} else {
				sortedTests.add(c);
			}
		}
		
		// inefficiently check the inefficiency
		for (int i = 0; i < sortedTests.size(); ++i) {
			Class<?> c = sortedTests.get(i);
			OrderAfter orderAfter = c.getAnnotation(OrderAfter.class);
			if (orderAfter != null) {
				Class<?> before = orderAfter.value();
				
				int j = sortedTests.indexOf(before);
				
				if (j < 0)
					throw new RuntimeException("what the heck, orderAfter class vanished");
				else if (j > i)
					throw new RuntimeException("class " + c.getName() + " should have been after " + before.getName() + " at " + j + " but is before at " + i);
			}
		}
		
		System.out.println("Spent " + (System.currentTimeMillis() - start)/1000.0 + " seconds on terrible sort implementation");
		
		for (Class<?> c : sortedTests) {
			if (c == null)
				throw new NullPointerException("null class in sortedTests");
			addTest(new TestSuite(c));
		}
	}
}
