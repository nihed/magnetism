package com.dumbhippo;

import javax.ejb.EJBException;


public class ExceptionUtils {

	public static Throwable getRootCause(Throwable symptom) {
		if (symptom == null)
			throw new IllegalArgumentException("null exception to getRootCause");
		Throwable root = symptom;
		while (root != null) {
			Throwable cause = root.getCause();
			if (cause == null) {
				// try the old ways
				if (root instanceof EJBException) {
					EJBException ejb = (EJBException) root;
					cause = ejb.getCausedByException();
				}
			}
			
			if (cause != null)
				root = cause;
			else
				break;
		}
		if (root == null)
			throw new RuntimeException("bug in getRootCause");
		return root;
	}
	
	public static void throwAsRuntimeException(Exception e) {
		if (e instanceof RuntimeException)
			throw (RuntimeException) e;
		else
			throw new RuntimeException(e);
	}
}
