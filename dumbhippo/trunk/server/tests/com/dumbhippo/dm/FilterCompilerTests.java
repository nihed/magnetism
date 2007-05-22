package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.CompiledKeyFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;
import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.identity20.Guid;

public class FilterCompilerTests extends TestCase {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterCompilerTests.class);

	public void testKeyFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(key)");
		Class<? extends CompiledKeyFilter<Guid,TestUserDMO>> compiled = FilterCompiler.compileKeyFilter(TestViewpoint.class, Guid.class, filter);
		CompiledKeyFilter<Guid,TestUserDMO> instance = compiled.newInstance();
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId().toString(), instance.filter(viewpoint, viewpoint.getViewerId()).toString());
		assertNull(instance.filter(viewpoint, Guid.createNew()));
	}
	
	public void testItemFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(item)");
		Class<? extends CompiledItemFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileItemFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledItemFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId().toString(), instance.filter(viewpoint, Guid.createNew(), viewpoint.getViewerId()).toString());
		assertNull(instance.filter(viewpoint, Guid.createNew(), Guid.createNew()));
	}
	
	public void testListFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(item)");
		Class<? extends CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileListFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		List<Guid> result = instance.filter(viewpoint, Guid.createNew(), input);
		
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).toString());
	}
	
	public void testAnyAllPhase() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(any)||viewer.isBuddy(all)");
		Class<? extends CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileListFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		List<Guid> buddies = new ArrayList<Guid>();
		buddies.add(Guid.createNew());
		buddies.add(Guid.createNew());
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew(), buddies);

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		assertEquals(2, instance.filter(viewpoint, viewpoint.getViewerId(), buddies).size());
		assertEquals(1, instance.filter(viewpoint, viewpoint.getViewerId(), Collections.singletonList(viewpoint.getViewerId())).size());
		assertEquals(0, instance.filter(viewpoint, viewpoint.getViewerId(), Collections.singletonList(Guid.createNew())).size());
	}
	
	public void testMultiplePhases() throws Exception {
		// Imaginary rule for viewing someone's friends list:
		// - The person themselves can see the entire list
		// - You can any mutual friends on the list unless you are an enemy of any of the friends 
		
		Filter filter =  FilterParser.parse("viewer.sameAs(key)||(viewer.isBuddy(item) && !viewer.isEnemy(any))");
		Class<? extends CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileListFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		Guid viewer = Guid.createNew();

		List<Guid> buddies = new ArrayList<Guid>();
		Guid buddy1 = Guid.createNew();
		buddies.add(buddy1);
		Guid buddy2 = Guid.createNew();
		buddies.add(buddy2);
		
		List<Guid> enemies = new ArrayList<Guid>();
		Guid enemy1 = Guid.createNew();
		enemies.add(enemy1);
		Guid enemy2 = Guid.createNew();
		enemies.add(enemy2);
		
		Guid stranger1 = Guid.createNew();
		Guid stranger2 = Guid.createNew();

		TestViewpoint viewpoint = new TestViewpoint(viewer, buddies, enemies);

		List<Guid> input;
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(enemy1); // Both a friend and an enemy? sameAs(key) makes it visible anyways
		
		assertEquals(3, instance.filter(viewpoint, viewer, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		
		assertEquals(2, instance.filter(viewpoint, stranger2, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		input.add(enemy1);

		assertEquals(0, instance.filter(viewpoint, stranger2, input).size());
	}
	
	// Tests the case where we have multiple distinct rules when entering the any-all phase
	public void testSwitchedAnyAllPhase() throws Exception {
		// Here the rule is, you can see the list if:
		// - Friends can see all the list, unless the list contains one of their enemies 
		// - Non-enemies can see all the list, if there is at least one mutual friend,
		//   unless the list contains one of their enemies
		
		Filter filter =  FilterParser.parse("!viewer.isEnemy(any)&&(viewer.isBuddy(key)||(!viewer.isEnemy(key)&&viewer.isBuddy(any)))");
		Class<? extends CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileListFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		Guid viewer = Guid.createNew();

		List<Guid> buddies = new ArrayList<Guid>();
		Guid buddy1 = Guid.createNew();
		buddies.add(buddy1);
		Guid buddy2 = Guid.createNew();
		buddies.add(buddy2);
		
		List<Guid> enemies = new ArrayList<Guid>();
		Guid enemy1 = Guid.createNew();
		enemies.add(enemy1);
		Guid enemy2 = Guid.createNew();
		enemies.add(enemy2);
		
		Guid stranger1 = Guid.createNew();
		Guid stranger2 = Guid.createNew();

		TestViewpoint viewpoint = new TestViewpoint(viewer, buddies, enemies);

		List<Guid> input;
		input = new ArrayList<Guid>();
		input.add(viewer);
		input.add(buddy2);
		
		assertEquals(2, instance.filter(viewpoint, buddy1, input).size());
		
		input.add(enemy1);
		assertEquals(0, instance.filter(viewpoint, buddy1, input).size());
		
		input = new ArrayList<Guid>();
		input.add(stranger1);
		
		assertEquals(0, instance.filter(viewpoint, stranger2, input).size());
		
		input.add(buddy1);
		assertEquals(2, instance.filter(viewpoint, stranger2, input).size());
	}
	
	// Tests the case where we have two multiple rules we use when scanning the items
	public void testSwitchedItemPhase() throws Exception {
		// Here the rule is:
		// - Friends can see all the items in the list, except for the viewer's enemies (we
		//   won't tell...)
		// - Non-enemies can see mutual friends in the list
		// - Enemies see nothing
		
		Filter filter =  FilterParser.parse("(viewer.isBuddy(key)&&!viewer.isEnemy(item))||(!viewer.isEnemy(key)&&viewer.isBuddy(item))");
		Class<? extends CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO>> compiled = FilterCompiler.compileListFilter(TestViewpoint.class, Guid.class, Guid.class, filter);
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = compiled.newInstance();
		
		Guid viewer = Guid.createNew();

		List<Guid> buddies = new ArrayList<Guid>();
		Guid buddy1 = Guid.createNew();
		buddies.add(buddy1);
		Guid buddy2 = Guid.createNew();
		buddies.add(buddy2);
		
		List<Guid> enemies = new ArrayList<Guid>();
		Guid enemy1 = Guid.createNew();
		enemies.add(enemy1);
		Guid enemy2 = Guid.createNew();
		enemies.add(enemy2);
		
		Guid stranger1 = Guid.createNew();
		Guid stranger2 = Guid.createNew();

		TestViewpoint viewpoint = new TestViewpoint(viewer, buddies, enemies);

		List<Guid> input;
		input = new ArrayList<Guid>();
		input.add(viewer);
		input.add(buddy2);
		input.add(enemy1);
		
		assertEquals(2, instance.filter(viewpoint, buddy1, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		
		assertEquals(2, instance.filter(viewpoint, stranger2, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);

		assertEquals(0, instance.filter(viewpoint, enemy1, input).size());
	}
}
