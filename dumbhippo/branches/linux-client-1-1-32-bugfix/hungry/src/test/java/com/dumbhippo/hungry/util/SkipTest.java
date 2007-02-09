package com.dumbhippo.hungry.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks something that looks like a test case as
 * not a test case; such classes will be skipped by PackageSuite.
 * 
 * @author otaylor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SkipTest {
}
