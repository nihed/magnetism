package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.CompiledSetFilter;
import com.dumbhippo.dm.filter.Filter;
import com.dumbhippo.dm.filter.FilterCompiler;
import com.dumbhippo.dm.parser.FilterParser;
import com.dumbhippo.identity20.Guid;

public class FilterCompilerTests extends TestCase {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterCompilerTests.class);
	
	private DataModel model;

	@Override
	protected void setUp()  {
		// The model is only used for the viewpoint class and the class pool; we don't
		// want to set up a real model, because we want to avoid compiling filters.
		model = new DataModel("http://mugshot.org", null, null, null,
							  TestViewpoint.class, new TestViewpoint(null));
	}
	
	@Override
	protected void tearDown() {
		model = null;
	}
	
	public void testFilterTrue() throws Exception {
		Filter filter =  FilterParser.parse("true");
		CompiledFilter<Guid,TestUserDMO> instance = FilterCompiler.compileFilter(model, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId().toString(), instance.filterKey(viewpoint, viewpoint.getViewerId()).toString());
	}
	
	public void testFilterFalse() throws Exception {
		Filter filter =  FilterParser.parse("false");
		CompiledFilter<Guid,TestUserDMO> instance = FilterCompiler.compileFilter(model, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertNull(instance.filterKey(viewpoint, viewpoint.getViewerId()));
	}
	
	public void testFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(this)");
		CompiledFilter<Guid,TestUserDMO> instance = FilterCompiler.compileFilter(model, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId().toString(), instance.filterKey(viewpoint, viewpoint.getViewerId()).toString());
		assertNull(instance.filterKey(viewpoint, Guid.createNew()));
	}
	
	public void testItemFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(item)");
		CompiledItemFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileItemFilter(model, Guid.class, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId().toString(), instance.filterKey(viewpoint, Guid.createNew(), viewpoint.getViewerId()).toString());
		assertNull(instance.filterKey(viewpoint, Guid.createNew(), Guid.createNew()));
	}
	
	public void testListFilterTrue() throws Exception {
		Filter filter =  FilterParser.parse("true");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		List<Guid> result = instance.filterKeys(viewpoint, Guid.createNew(), input);
		
		assertEquals(2, result.size());
	}
	
	public void testListFilterFalse() throws Exception {
		Filter filter =  FilterParser.parse("false");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		List<Guid> result = instance.filterKeys(viewpoint, Guid.createNew(), input);
		
		assertEquals(0, result.size());
	}
	
	public void testListFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(item)");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		List<Guid> result = instance.filterKeys(viewpoint, Guid.createNew(), input);
		
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).toString());
	}
	
	public void testSetFilterSimple() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(item)");
		CompiledSetFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileSetFilter(model, Guid.class, Guid.class, filter);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());

		Set<Guid> input = new HashSet<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		Set<Guid> result = instance.filterKeys(viewpoint, Guid.createNew(), input);
		
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.iterator().next().toString());
	}
	
	public void testAnyAllPhase() throws Exception {
		Filter filter =  FilterParser.parse("viewer.sameAs(any)||viewer.isBuddy(all)");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
		List<Guid> buddies = new ArrayList<Guid>();
		buddies.add(Guid.createNew());
		buddies.add(Guid.createNew());
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew(), buddies);

		List<Guid> input = new ArrayList<Guid>();
		input.add(viewpoint.getViewerId());
		input.add(Guid.createNew());
		
		assertEquals(2, instance.filterKeys(viewpoint, viewpoint.getViewerId(), buddies).size());
		assertEquals(1, instance.filterKeys(viewpoint, viewpoint.getViewerId(), Collections.singletonList(viewpoint.getViewerId())).size());
		assertEquals(0, instance.filterKeys(viewpoint, viewpoint.getViewerId(), Collections.singletonList(Guid.createNew())).size());
	}
	
	public void testMultiplePhases() throws Exception {
		// Imaginary rule for viewing someone's friends list:
		// - The person themselves can see the entire list
		// - You can any mutual friends on the list unless you are an enemy of any of the friends 
		
		Filter filter =  FilterParser.parse("viewer.sameAs(this)||(viewer.isBuddy(item) && !viewer.isEnemy(any))");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
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
		input.add(enemy1); // Both a friend and an enemy? sameAs(this) makes it visible anyways
		
		assertEquals(3, instance.filterKeys(viewpoint, viewer, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		
		assertEquals(2, instance.filterKeys(viewpoint, stranger2, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		input.add(enemy1);

		assertEquals(0, instance.filterKeys(viewpoint, stranger2, input).size());
	}
	
	// Tests the case where we have multiple distinct rules when entering the any-all phase
	public void testSwitchedAnyAllPhase() throws Exception {
		// Here the rule is, you can see the list if:
		// - Friends can see all the list, unless the list contains one of their enemies 
		// - Non-enemies can see all the list, if there is at least one mutual friend,
		//   unless the list contains one of their enemies
		
		Filter filter =  FilterParser.parse("!viewer.isEnemy(any)&&(viewer.isBuddy(this)||(!viewer.isEnemy(this)&&viewer.isBuddy(any)))");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
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
		
		assertEquals(2, instance.filterKeys(viewpoint, buddy1, input).size());
		
		input.add(enemy1);
		assertEquals(0, instance.filterKeys(viewpoint, buddy1, input).size());
		
		input = new ArrayList<Guid>();
		input.add(stranger1);
		
		assertEquals(0, instance.filterKeys(viewpoint, stranger2, input).size());
		
		input.add(buddy1);
		assertEquals(2, instance.filterKeys(viewpoint, stranger2, input).size());
	}
	
	// Tests the case where we have two multiple rules we use when scanning the items
	public void testSwitchedItemPhase() throws Exception {
		// Here the rule is:
		// - Friends can see all the items in the list, except for the viewer's enemies (we
		//   won't tell...)
		// - Non-enemies can see mutual friends in the list
		// - Enemies see nothing
		
		Filter filter =  FilterParser.parse("(viewer.isBuddy(this)&&!viewer.isEnemy(item))||(!viewer.isEnemy(this)&&viewer.isBuddy(item))");
		CompiledListFilter<Guid,TestUserDMO,Guid,TestUserDMO> instance = FilterCompiler.compileListFilter(model, Guid.class, Guid.class, filter);
		
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
		
		assertEquals(2, instance.filterKeys(viewpoint, buddy1, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);
		
		assertEquals(2, instance.filterKeys(viewpoint, stranger2, input).size());
		
		input = new ArrayList<Guid>();
		input.add(buddy1);
		input.add(buddy2);
		input.add(stranger1);

		assertEquals(0, instance.filterKeys(viewpoint, enemy1, input).size());
	}
}
