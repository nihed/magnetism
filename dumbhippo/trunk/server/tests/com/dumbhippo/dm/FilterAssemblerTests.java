package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import junit.framework.TestCase;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestGroupMemberDMO;
import com.dumbhippo.dm.dm.TestGroupMemberKey;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.filter.CompiledFilter;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.CompiledSetFilter;
import com.dumbhippo.dm.filter.FilterAssembler;
import com.dumbhippo.identity20.Guid;

public class FilterAssemblerTests extends TestCase {
	ClassPool classPool;
	
	@Override
	protected void setUp()  {
		classPool = new ClassPool();
		classPool.insertClassPath(new ClassClassPath(this.getClass()));
	}
	
	@Override
	protected void tearDown() {
		classPool = null;
	}
	
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterAssemblerTests.class);
	
	private CtClass ctClassForClass(Class<?> c) {
		String className = c.getName();

		try {
			return classPool.get(className);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find the bytecode for" + className);
		}
	}
	
	private <K,T extends DMObject<K>> CompiledFilter<K,T> createFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledFilter.class));
		
		assembler.addMethodToClass(ctClass, "filterKey");
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledFilter<K,T>> subclass = (Class<? extends CompiledFilter<K,T>>)clazz.asSubclass(CompiledFilter.class);
		return subclass.newInstance();
	}
	
	private <K,T extends DMObject<K>,KI,TI extends DMObject<KI>> CompiledItemFilter<K,T,KI,TI> createItemFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledItemFilter.class));
		
		assembler.addMethodToClass(ctClass, "filterKey");
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledItemFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledItemFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledItemFilter.class);
		return subclass.newInstance();
	}
	
	private <K,T extends DMObject<K>,KI,TI extends DMObject<KI>> CompiledListFilter<K,T,KI,TI> createListFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledListFilter.class));
		
		assembler.addMethodToClass(ctClass, "filterKeys");
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledListFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledListFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledListFilter.class);
		return subclass.newInstance();
	}
	
	private <K,T extends DMObject<K>,KI,TI extends DMObject<KI>> CompiledSetFilter<K,T,KI,TI> createSetFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledSetFilter.class));
		
		assembler.addMethodToClass(ctClass, "filterKeys");
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledSetFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledSetFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledSetFilter.class);
		return subclass.newInstance();
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// test thisCondition(), no property, returnNull, returnThis()
	public void testThisConditionFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForFilter(TestViewpoint.class, Guid.class, false);
		
		assembler.thisCondition("sameAs", null, true, "STATE1");
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnThis();
		
		CompiledFilter<Guid, TestUserDMO> filter = createFilter("KeyConditionFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId(), filter.filterKey(viewpoint, viewpoint.getViewerId()));
		assertNull(filter.filterKey(viewpoint, Guid.createNew()));
	}
	
	// test thisCondition(), with a property
	public void testKeyPropertyConditionFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForFilter(TestViewpoint.class, TestGroupMemberKey.class, false);
		
		assembler.thisCondition("sameAs", "memberId", true, "STATE1");
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnThis();
		
		CompiledFilter<TestGroupMemberKey, TestGroupMemberDMO> filter = createFilter("KeyPropertyConditionFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestGroupMemberKey key;
		
		key = new TestGroupMemberKey(Guid.createNew(), viewpoint.getViewerId());
		assertEquals(viewpoint.getViewerId(), filter.filterKey(viewpoint, key).getMemberId());
		
		key = new TestGroupMemberKey(Guid.createNew(), Guid.createNew());
		assertNull(filter.filterKey(viewpoint, key));
	}

	// test switchState()
	public void testSwitchFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForFilter(TestViewpoint.class, Guid.class, false);
		
		assembler.setState(1);
		assembler.switchState(
				new int[] { 0, 1 },
				new String[] { "STATE0", "STATE1" }
		);
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnThis();
		
		CompiledFilter<Guid, TestUserDMO> filter = createFilter("SwitchFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId(), filter.filterKey(viewpoint, viewpoint.getViewerId()));
	}

	// Test returnItem(), itemCondition() for an item filter 
	public void testItemFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForItemFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.itemCondition("sameAs", null, false, "NULL");
		assembler.returnItem();
		assembler.label("NULL");
		assembler.returnNull();
		
		CompiledItemFilter<Guid, ?, Guid, ?> filter = createItemFilter("ItemFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		Guid result;

		result = filter.filterKey(viewpoint, Guid.createNew(), viewpoint.getViewerId());
		assertEquals(viewpoint.getViewerId().toString(), result.toString());
		
		result = filter.filterKey(viewpoint, Guid.createNew(), Guid.createNew());
		assertNull(result);
	}
	
	// Test returnEmpty()
	public void testReturnEmptyFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.returnEmpty();
		
		CompiledListFilter<Guid, ?, Guid, ?> filter = createListFilter("ReturnEmptyFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> result = filter.filterKeys(viewpoint, viewpoint.getViewerId(), Collections.singletonList(viewpoint.getViewerId()));
		assertEquals(0, result.size());
	}
	
	// Test returnItems()
	public void testReturnItemsFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.returnItems();
		
		CompiledListFilter<Guid, ?, Guid, ?> filter = createListFilter("ReturnItemsFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> result = filter.filterKeys(viewpoint, Guid.createNew(), Collections.singletonList(viewpoint.getViewerId()));
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).toString());
	}
	
	// Test startResult(), addResultItem(), startItems(), nextItem(), itemCondition() for a list, no property
	public void testIterateItemsFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.startResult();
		assembler.startItems();
		assembler.label("NEXT");
		assembler.nextItem("DONE");
		assembler.itemCondition("sameAs", null, false, "NEXT");
		assembler.addResultItem();
		assembler.jump("NEXT");
		assembler.label("DONE");
		assembler.returnResult();
		
		CompiledListFilter<Guid, ?, Guid, ?> filter = createListFilter("IterateItemsFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> items = new ArrayList<Guid>();
		items.add(Guid.createNew());
		items.add(viewpoint.getViewerId());
		
		List<Guid> result = filter.filterKeys(viewpoint, Guid.createNew(), items);
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).toString());
	}

	// Test itemCondition() for a list, with a property
	public void testItemPropertyFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, TestGroupMemberKey.class, false);
		
		assembler.startResult();
		assembler.startItems();
		assembler.label("NEXT");
		assembler.nextItem("DONE");
		assembler.itemCondition("sameAs", "memberId", false, "NEXT");
		assembler.addResultItem();
		assembler.jump("NEXT");
		assembler.label("DONE");
		assembler.returnResult();
		
		CompiledListFilter<Guid, ?, TestGroupMemberKey, ?> filter = createListFilter("ItemPropertyFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		Guid groupId = Guid.createNew();
		List<TestGroupMemberKey> items = new ArrayList<TestGroupMemberKey>();
		items.add(new TestGroupMemberKey(groupId, Guid.createNew()));
		items.add(new TestGroupMemberKey(groupId, viewpoint.getViewerId()));
		
		List<TestGroupMemberKey> result = filter.filterKeys(viewpoint, Guid.createNew(), items);
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).getMemberId().toString());
	}
	
	// Test startResult(), addResultItem(), startItems(), nextItem(), itemCondition() for a list, no property
	public void testSetFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForSetFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.startResult();
		assembler.startItems();
		assembler.label("NEXT");
		assembler.nextItem("DONE");
		assembler.itemCondition("sameAs", null, false, "NEXT");
		assembler.addResultItem();
		assembler.jump("NEXT");
		assembler.label("DONE");
		assembler.returnResult();
		
		CompiledSetFilter<Guid, ?, Guid, ?> filter = createSetFilter("SetFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		Set<Guid> items = new HashSet<Guid>();
		items.add(Guid.createNew());
		items.add(viewpoint.getViewerId());
		
		Set<Guid> result = filter.filterKeys(viewpoint, Guid.createNew(), items);
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.iterator().next().toString());
	}

}
