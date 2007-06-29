package com.dumbhippo.dm;

import junit.framework.TestCase;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.filter.FilterStateMap;
import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.dm.parser.ParseException;

public class FilterStateMapTests extends TestCase {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterStateMapTests.class);

	private FilterStateMap createMap(String filterString, boolean singlePhase) throws ParseException {
		return new FilterStateMap(FilterParser.parse(filterString), singlePhase);
	}
	
	public void testMultipleConditions() throws Exception {
		String test = "(viewer.t(this) || viewer.a(all)) && viewer.i(item)";
		FilterStateMap map = createMap(test, false);
		String mapString = map.toString();
		logger.debug("Map of {} is:\n{}", test, mapString);
		assertEquals(
			"GlobalPhase:\n" +
			"    State0: ((viewer.t(this))||(viewer.a(all)))&&(viewer.i(item)) [r]\n" +
			"        viewer.t(this) => (State2, State1)\n" +
			"AnyAllPhase:\n" +
			"    State1: (viewer.a(all))&&(viewer.i(item)) [r]\n" +
			"        viewer.a(all) => (State2, FalseState)\n" +
			"        defaults => State2\n" +
			"ItemPhase:\n" +
			"    State2: viewer.i(item) [r]\n" +
			"        viewer.i(item) => (TrueState, FalseState)\n",
			mapString);
	}
	
	public void testAnyAll() throws Exception {
		String test = "viewer.a1(any) || viewer.a2(any)";
		FilterStateMap map = createMap(test, false);
		String mapString = map.toString();
		logger.debug("Map of {} is:\n{}", test, mapString);
		assertEquals(
			"GlobalPhase:\n" +
			"AnyAllPhase:\n" +
			"    State0: (viewer.a1(any))||(viewer.a2(any)) [r]\n" +
			"        viewer.a1(any) => (TrueState, State1)\n" +
			"        viewer.a2(any) => (TrueState, State2)\n" +
			"        defaults => FalseState\n" +
			"    State1: viewer.a2(any)\n" +
			"        viewer.a2(any) => (TrueState, FalseState)\n" +
			"        defaults => FalseState\n" +
			"    State2: viewer.a1(any)\n" +
			"        viewer.a1(any) => (TrueState, FalseState)\n" +
			"        defaults => FalseState\n" +
			"ItemPhase:\n",
			mapString);
	}
	
	public void testNot() throws Exception {
		String test = "!viewer.t(this)";
		FilterStateMap map = createMap(test, false);
		String mapString = map.toString();
		logger.debug("Map of {} is:\n{}", test, mapString);
		assertEquals(
			"GlobalPhase:\n" +
			"    State0: !(viewer.t(this)) [r]\n" +
			"        viewer.t(this) => (FalseState, TrueState)\n" +
			"AnyAllPhase:\n" +
			"ItemPhase:\n",
			mapString);
	}
	
	public void testSinglePhase() throws Exception {
		String test = "(viewer.t(this) || viewer.a(all)) && viewer.i(item)";
		FilterStateMap map = createMap(test, true);
		String mapString = map.toString();
		logger.debug("Map of {} is:\n{}", test, mapString);
		assertEquals(
			"GlobalPhase:\n" +
			"    State0: ((viewer.t(this))||(viewer.a(all)))&&(viewer.i(item)) [r]\n" +
			"        viewer.t(this) => (State1, State2)\n" +
			"    State1: viewer.i(item)\n" +
			"        viewer.i(item) => (TrueState, FalseState)\n" +
			"    State2: (viewer.a(all))&&(viewer.i(item))\n" +
			"        viewer.a(all) => (State1, FalseState)\n" +
			"AnyAllPhase:\n" +
			"ItemPhase:\n",
			mapString);
	}
	
}
