package com.dumbhippo.dm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.Descriptor;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.MethodInfo;
import junit.framework.TestCase;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.dm.TestGroupMemberDMO;
import com.dumbhippo.dm.dm.TestGroupMemberKey;
import com.dumbhippo.dm.dm.TestUserDMO;
import com.dumbhippo.dm.filter.CompiledItemFilter;
import com.dumbhippo.dm.filter.CompiledKeyFilter;
import com.dumbhippo.dm.filter.CompiledListFilter;
import com.dumbhippo.dm.filter.FilterAssembler;
import com.dumbhippo.identity20.Guid;

public class FilterAssemblerTests extends TestCase {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterAssemblerTests.class);
	
	private CtClass ctClassForClass(Class<?> c) {
		ClassPool classPool = DataModel.getInstance().getClassPool();
		String className = c.getName();

		try {
			return classPool.get(className);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find the bytecode for" + className);
		}
	}
	
	private <K1,T1 extends DMObject<K1>> CompiledKeyFilter<K1,T1> createKeyFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		ClassPool classPool = DataModel.getInstance().getClassPool();
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledKeyFilter.class));
		
		assembler.addMethodToClass(ctClass);
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledKeyFilter<K1,T1>> subclass = (Class<? extends CompiledKeyFilter<K1,T1>>)clazz.asSubclass(CompiledKeyFilter.class);
		return subclass.newInstance();
	}
	
	private <K1,K2,T1 extends DMObject<K1>,T2 extends DMObject<K2>> CompiledItemFilter<K1,K2,T1,T2> createItemFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		ClassPool classPool = DataModel.getInstance().getClassPool();
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledItemFilter.class));
		
		assembler.addMethodToClass(ctClass);
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledItemFilter<K1,K2,T1,T2>> subclass = (Class<? extends CompiledItemFilter<K1,K2,T1,T2>>)clazz.asSubclass(CompiledItemFilter.class);
		return subclass.newInstance();
	}
	
	private <K1,K2,T1 extends DMObject<K1>,T2 extends DMObject<K2>> CompiledListFilter<K1,K2,T1,T2> createListFilter(String name, FilterAssembler assembler) throws CannotCompileException, InstantiationException, IllegalAccessException {
		ClassPool classPool = DataModel.getInstance().getClassPool();
		CtClass ctClass = classPool.makeClass("com.dumbhippo.tests." + name);
		
		ctClass.addInterface(ctClassForClass(CompiledListFilter.class));
		
		assembler.addMethodToClass(ctClass);
		
		Class<?> clazz = ctClass.toClass();
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledListFilter<K1,K2,T1,T2>> subclass = (Class<? extends CompiledListFilter<K1,K2,T1,T2>>)clazz.asSubclass(CompiledListFilter.class);
		return subclass.newInstance();
	}
	
	public void addListFilterMethod(CtClass ctClass, CodeAttribute code) throws DuplicateMemberException { 
		ClassFile classFile = ctClass.getClassFile();
		String desc = Descriptor.ofMethod(ctClassForClass(List.class), 
				                          new CtClass[] { ctClassForClass(DMViewpoint.class), 
				                                          ctClassForClass(Object.class),
				                                          ctClassForClass(List.class) });
		
		MethodInfo methodInfo = new MethodInfo(classFile.getConstPool(), "filter", desc);
		methodInfo.setAccessFlags(AccessFlag.PUBLIC);
		methodInfo.setCodeAttribute(code);

		classFile.addMethod(methodInfo);
	}
	
	////////////////////////////////////////////////////////////////////////////////
	
	// test keyCondition(), no property, returnNull, returnKey()
	public void testKeyConditionFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForKeyFilter(TestViewpoint.class, Guid.class, false);
		
		assembler.keyCondition("sameAs", null, true, "STATE1");
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnKey();
		
		CompiledKeyFilter<Guid, TestUserDMO> filter = createKeyFilter("KeyConditionFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId(), filter.filter(viewpoint, viewpoint.getViewerId()));
		assertNull(filter.filter(viewpoint, Guid.createNew()));
	}
	
	// test keyCondition(), with a property
	public void testKeyPropertyConditionFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForKeyFilter(TestViewpoint.class, TestGroupMemberKey.class, false);
		
		assembler.keyCondition("sameAs", "memberId", true, "STATE1");
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnKey();
		
		CompiledKeyFilter<TestGroupMemberKey, TestGroupMemberDMO> filter = createKeyFilter("KeyPropertyConditionFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		TestGroupMemberKey key;
		
		key = new TestGroupMemberKey(Guid.createNew(), viewpoint.getViewerId());
		assertEquals(viewpoint.getViewerId(), filter.filter(viewpoint, key).getMemberId());
		
		key = new TestGroupMemberKey(Guid.createNew(), Guid.createNew());
		assertNull(filter.filter(viewpoint, key).getMemberId());
	}

	// test switchState()
	public void testSwitchFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForKeyFilter(TestViewpoint.class, Guid.class, false);
		
		assembler.setState(1);
		assembler.switchState(
				new int[] { 0, 1 },
				new String[] { "STATE0", "STATE1" }
		);
		assembler.label("STATE0");
		assembler.returnNull();
		assembler.label("STATE1");
		assembler.returnKey();
		
		CompiledKeyFilter<Guid, TestUserDMO> filter = createKeyFilter("SwitchFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		assertEquals(viewpoint.getViewerId(), filter.filter(viewpoint, viewpoint.getViewerId()));
	}

	// Test returnItem(), itemCondition() for an item filter 
	public void testItemFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForItemFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.itemCondition("sameAs", null, false, "NULL");
		assembler.returnItem();
		assembler.label("NULL");
		assembler.returnNull();
		
		CompiledItemFilter<Guid, Guid, ?, ?> filter = createItemFilter("ItemFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		Guid result;

		result = filter.filter(viewpoint, Guid.createNew(), viewpoint.getViewerId());
		assertEquals(viewpoint.getViewerId().toString(), result.toString());
		
		result = filter.filter(viewpoint, Guid.createNew(), Guid.createNew());
		assertNull(result);
	}
	
	// Test returnEmpty()
	public void testReturnEmptyFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.returnEmpty();
		
		CompiledListFilter<Guid, Guid, ?, ?> filter = createListFilter("ReturnEmptyFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> result = filter.filter(viewpoint, viewpoint.getViewerId(), Collections.singletonList(viewpoint.getViewerId()));
		assertEquals(0, result.size());
	}
	
	// Test returnItems()
	public void testReturnItemsFilterMethod() throws CannotCompileException, InstantiationException, IllegalAccessException {
		FilterAssembler assembler = FilterAssembler.createForListFilter(TestViewpoint.class, Guid.class, Guid.class, false);
		
		assembler.returnItems();
		
		CompiledListFilter<Guid, Guid, ?, ?> filter = createListFilter("ReturnItemsFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> result = filter.filter(viewpoint, Guid.createNew(), Collections.singletonList(viewpoint.getViewerId()));
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
		
		CompiledListFilter<Guid, Guid, ?, ?> filter = createListFilter("IterateItemsFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		List<Guid> items = new ArrayList<Guid>();
		items.add(Guid.createNew());
		items.add(viewpoint.getViewerId());
		
		List<Guid> result = filter.filter(viewpoint, Guid.createNew(), items);
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
		
		CompiledListFilter<Guid, TestGroupMemberKey, ?, ?> filter = createListFilter("ItemPropertyFilter", assembler);
		
		TestViewpoint viewpoint = new TestViewpoint(Guid.createNew());
		
		Guid groupId = Guid.createNew();
		List<TestGroupMemberKey> items = new ArrayList<TestGroupMemberKey>();
		items.add(new TestGroupMemberKey(groupId, Guid.createNew()));
		items.add(new TestGroupMemberKey(groupId, viewpoint.getViewerId()));
		
		List<TestGroupMemberKey> result = filter.filter(viewpoint, Guid.createNew(), items);
		assertEquals(1, result.size());
		assertEquals(viewpoint.getViewerId().toString(), result.get(0).getMemberId().toString());
	}
}
