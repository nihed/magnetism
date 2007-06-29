package com.dumbhippo.dm.filter;

import java.util.List;

import org.slf4j.Logger;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMObject;
import com.dumbhippo.dm.DMViewpoint;
import com.dumbhippo.dm.DataModel;
import com.dumbhippo.identity20.Guid;

public class FilterCompiler<K, T extends DMObject<K>, KI, TI extends DMObject<KI>> {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterCompiler.class);

	/**
	 * Compile a Filter expression into a CompiledFilter, specifying the types of the
	 * key for the objects that that this filter filters. 
	 * 
	 * @param <K>
	 * @param <T>
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass class of the object's key
	 * @param filter
	 * @return
	 */
	static public <K, T extends DMObject<K>> CompiledFilter<K,T> compileFilter(DataModel model, Class<K> objectKeyClass, Filter filter) {
		return new FilterCompiler<K,T,Guid,DMObject<Guid>>(model, objectKeyClass, null, filter).doCompileFilter();
	}
	
	/**
	 * Compile a Filter expression into a CompiledItemFilter, specifying the types of the
	 * key of the object that the item is retrieved from and the type of the key for the item itself. 
	 * 
	 * @param <K>
	 * @param <T>
	 * @param <KI>
	 * @param <TI>
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass Class of the object's key
	 * @param itemKeyClass Class of the item's key
	 * @param filter
	 * @return
	 */
	public static <K, T extends DMObject<K>, KI, TI extends DMObject<KI>> CompiledItemFilter<K,T,KI,TI> compileItemFilter(DataModel model, Class<K> objectKeyClass, Class<KI> itemKeyClass, Filter filter) {
		return new FilterCompiler<K,T,KI,TI>(model, objectKeyClass, itemKeyClass, filter).doCompileItemFilter();
	}

	/**
	 * Compile a Filter expression into a CompiledListFilter, specifying the types of the
	 * key of the object that the items are retrieved from and the type of the key for the item themselves.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param <KI>
	 * @param <TI>
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass Class of the object's key
	 * @param itemKeyClass Class of the item's key
	 * @param filter
	 * @return
	 */
	public static <K, T extends DMObject<K>, KI, TI extends DMObject<KI>> CompiledListFilter<K,T,KI,TI> compileListFilter(DataModel model, Class<K> objectKeyClass, Class<KI> itemKeyClass, Filter filter) {
		return new FilterCompiler<K,T,KI,TI>(model, objectKeyClass, itemKeyClass, filter).doCompileListFilter();
	}

	/**
	 * Compile a Filter expression into a CompiledSetFilter, specifying the types of the
	 * key of the object that the items are retrieved from and the type of the key for the item themselves.
	 * 
	 * @param <K>
	 * @param <T>
	 * @param <KI>
	 * @param <TI>
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass Class of the object's key
	 * @param itemKeyClass Class of the item's key
	 * @param filter
	 * @return
	 */
	public static <K, T extends DMObject<K>, KI, TI extends DMObject<KI>> CompiledSetFilter<K,T,KI,TI> compileSetFilter(DataModel model, Class<K> objectKeyClass, Class<KI> itemKeyClass, Filter filter) {
		return new FilterCompiler<K,T,KI,TI>(model, objectKeyClass, itemKeyClass, filter).doCompileSetFilter();
	}

	private Class<? extends DMViewpoint> viewpointClass;
	private Class<K> objectKeyClass; 
	private Class<KI> itemKeyClass;
	private Filter filter; 
	private ClassPool classPool;
	private static int serial = 1;
	
	private FilterCompiler(DataModel model, Class<K> objectKeyClass, Class<KI> itemKeyClass, Filter filter) {
		this.filter = filter;
		this.viewpointClass = model.getViewpointClass();
		this.objectKeyClass = objectKeyClass;
		this.itemKeyClass = itemKeyClass;
		
		classPool = model.getClassPool();
	}
	
	private CompiledFilter<K,T> doCompileFilter() {
		CtClass ctClass = makeCtClass();
		ctClass.addInterface(ctClassForClass(CompiledFilter.class));

		FilterStateMap stateMap = new FilterStateMap(filter, false);
		logger.debug("State map for {} is:\n{}",  filter, stateMap);
		
		FilterAssembler assembler;
		assembler = FilterAssembler.createForFilter(viewpointClass, objectKeyClass, false);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_KEY, ReturnAction.RETURN_NULL);
		assembler.optimize(); // call explicitly for better debug output
		logger.debug("KeyFilter assembly code for {} is:\n{}",  filter, assembler);
		
		assembler.addMethodToClass(ctClass, "filterKey");

		assembler = FilterAssembler.createForFilter(viewpointClass, objectKeyClass, true);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_KEY, ReturnAction.RETURN_NULL);
		assembler.addMethodToClass(ctClass, "filterObject");

		Class<?> clazz;
		try {
			clazz = ctClass.toClass();
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling generated class", e);
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledFilter<K,T>> subclass = (Class<? extends CompiledFilter<K,T>>)clazz.asSubclass(CompiledFilter.class);
		
		try {
			return subclass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Error creating filter instance", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error creating filter instance", e);
		}
	}
	
	private CompiledItemFilter<K,T,KI,TI> doCompileItemFilter() {
		CtClass ctClass = makeCtClass();
		ctClass.addInterface(ctClassForClass(CompiledItemFilter.class));

		FilterStateMap stateMap = new FilterStateMap(filter, true);
		logger.debug("State map (singlePhase) for {} is:\n{}",  filter, stateMap);
		
		FilterAssembler assembler;
		assembler = FilterAssembler.createForItemFilter(viewpointClass, objectKeyClass, itemKeyClass, false);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEM, ReturnAction.RETURN_NULL);
		assembler.optimize(); // call explicitly for better debug output
		logger.debug("ItemFilter assembly code for {} is:\n{}",  filter, assembler);

		assembler.addMethodToClass(ctClass, "filterKey");
		
		assembler = FilterAssembler.createForItemFilter(viewpointClass, objectKeyClass, itemKeyClass, true);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEM, ReturnAction.RETURN_NULL);
		assembler.addMethodToClass(ctClass, "filterObject");
		
		Class<?> clazz;
		try {
			clazz = ctClass.toClass();
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling generated class", e);
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledItemFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledItemFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledItemFilter.class);
		
		try {
			return subclass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Error creating filter instance", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error creating filter instance", e);
		}
	}
	
	private CompiledListFilter<K,T,KI,TI> doCompileListFilter() {
		CtClass ctClass = makeCtClass();
		ctClass.addInterface(ctClassForClass(CompiledListFilter.class));

		FilterStateMap stateMap = new FilterStateMap(filter, false);
		logger.debug("State map for {} is:\n{}",  filter, stateMap);
		
		FilterAssembler assembler;
		assembler = FilterAssembler.createForListFilter(viewpointClass, objectKeyClass, itemKeyClass, false);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEMS, ReturnAction.RETURN_EMPTY);
		generateAnyAllPhaseCode(assembler, stateMap);
		generateItemPhaseCode(assembler, stateMap);
		assembler.optimize(); // call explicitly for better debug output
		logger.debug("ItemFilter assembly code for {} is:\n{}",  filter, assembler);

		assembler.addMethodToClass(ctClass, "filterKeys");
		
		assembler = FilterAssembler.createForListFilter(viewpointClass, objectKeyClass, itemKeyClass, true);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEMS, ReturnAction.RETURN_EMPTY);
		generateAnyAllPhaseCode(assembler, stateMap);
		generateItemPhaseCode(assembler, stateMap);
		assembler.addMethodToClass(ctClass, "filterObjects");

		Class<?> clazz;
		try {
			clazz = ctClass.toClass();
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling generated class", e);
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledListFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledListFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledListFilter.class);

		
		try {
			return subclass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Error creating filter instance", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error creating filter instance", e);
		}
	}

	private CompiledSetFilter<K,T,KI,TI> doCompileSetFilter() {
		CtClass ctClass = makeCtClass();
		ctClass.addInterface(ctClassForClass(CompiledSetFilter.class));

		FilterStateMap stateMap = new FilterStateMap(filter, false);
		logger.debug("State map for {} is:\n{}",  filter, stateMap);
		
		FilterAssembler assembler;
		assembler = FilterAssembler.createForSetFilter(viewpointClass, objectKeyClass, itemKeyClass, false);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEMS, ReturnAction.RETURN_EMPTY);
		generateAnyAllPhaseCode(assembler, stateMap);
		generateItemPhaseCode(assembler, stateMap);
		assembler.optimize(); // call explicitly for better debug output
		logger.debug("ItemFilter assembly code for {} is:\n{}",  filter, assembler);

		assembler.addMethodToClass(ctClass, "filterKeys");
		
		assembler = FilterAssembler.createForSetFilter(viewpointClass, objectKeyClass, itemKeyClass, true);
		generateKeyPhaseCode(assembler, stateMap, ReturnAction.RETURN_ITEMS, ReturnAction.RETURN_EMPTY);
		generateAnyAllPhaseCode(assembler, stateMap);
		generateItemPhaseCode(assembler, stateMap);
		assembler.addMethodToClass(ctClass, "filterObjects");

		Class<?> clazz;
		try {
			clazz = ctClass.toClass();
		} catch (CannotCompileException e) {
			throw new RuntimeException("Error compiling generated class", e);
		}
		
		@SuppressWarnings("unchecked")
		Class<? extends CompiledSetFilter<K,T,KI,TI>> subclass = (Class<? extends CompiledSetFilter<K,T,KI,TI>>)clazz.asSubclass(CompiledSetFilter.class);

		
		try {
			return subclass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("Error creating filter instance", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error creating filter instance", e);
		}
	}

	private static synchronized int nextSerial() {
		return serial++;
	}
	
	private CtClass makeCtClass() {
		String name = "com.dumbhippo.dm.filter.GeneratedFilter" + nextSerial(); 
		return classPool.makeClass(name);
	}
	
	private CtClass ctClassForClass(Class<?> c) {
		String className = c.getName();

		try {
			return classPool.get(className);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't find the bytecode for" + className);
		}
	}
	
	private enum ReturnAction {
		RETURN_NULL,
		RETURN_ITEM,
		RETURN_KEY,
		RETURN_EMPTY,
		RETURN_ITEMS
	};
	
	private void addReturn(FilterAssembler assembler, ReturnAction action) {
		switch (action) {
		case RETURN_KEY:
			assembler.returnThis();
			break;
		case RETURN_ITEM:
			assembler.returnItem();
			break;
		case RETURN_NULL:
			assembler.returnNull();
			break;
		case RETURN_EMPTY:
			assembler.returnEmpty();
			break;
		case RETURN_ITEMS:
			assembler.returnItems();
			break;
		}
	}
	
	private String beginStateLabel(FilterState state) {
		return "BEGIN_" + state.getName();
	}
	
	private String getBranchLabel(FilterState sourceState, FilterState targetState) {
		if (sourceState.getPhase() == targetState.getPhase())
			return targetState.getName();
		else
			return beginStateLabel(targetState);
	}
	
	private int countRootStates(List<FilterState> itemStates) {
		int count = 0;
		for (FilterState state : itemStates) {
			if (!state.isRoot())
				break;
			count++;
		}
		
		return count;
	}

	private static final String TRUE_RETURN = "TRUE_RETURN";

	/**
	 * Generates the code for phase 0, the "global phase. For plain filters and 
	 * for list filters, we only process key conditions in this phase.
	 * For an item filter, all of the conditions will have been marked as "phase 0" because 
	 * singlePhase was passed to the FilterStateMap constructor, and will be processed
	 * here, but we do need to process them a little differently - see the switch 
	 * over condition.getType() below.
	 * 
	 * @param assembler
	 * @param stateMap
	 * @param onTrueReturn what to return when the filter is fully evaluated to 'true'; this
	 *   differs depending on the type of the filter 
	 * @param onFlaseReturn what to return when the filter is fully evaluated to 'false'; this
	 *   differs depending on the type of the filter 
	 * @param onFalseReturn
	 */
	private void generateKeyPhaseCode(FilterAssembler assembler, FilterStateMap stateMap, ReturnAction onTrueReturn, ReturnAction onFalseReturn) {
		boolean needTrueReturn = false;
		
		for (FilterState state : stateMap.getGlobalStates()) {
			assembler.label(state.getName(), state.getFilter().toString());
			Condition condition = state.getChildCondition(0);
			String branchLabel;
			boolean branchWhen;
			FilterState inlineState;

			// After we evaluate a condition, the action is:
			//
			//  true => go_to_other_state | return_true
			//  false => go_to_other_state | return_false
			//
			// Both results can't be return_true or return_false (or this state wouldn't
			// need to exist), so we normally can make the branch target of the condition
			// the label for a different state. In the case we have return_true and 
			// return_false, we need to generate a separate block to handle the return_true
			// case, but that block can be shared between all states.
			
			FilterState ifTrue = state.getChildState(0, true);
			FilterState ifFalse = state.getChildState(0, false);
			
			if (ifTrue.getPhase() != 3) {
				branchLabel = getBranchLabel(state, ifTrue);
				branchWhen = true;
				inlineState = ifFalse;
			} else if (ifFalse.getPhase() != 3) {
				branchLabel = getBranchLabel(state, ifFalse);
				branchWhen = false;
				inlineState = ifTrue;
			} else if (ifTrue == FilterState.TRUE_STATE) {
				branchLabel = TRUE_RETURN;
				branchWhen = true;
				inlineState = ifFalse;
				needTrueReturn = true;
			} else if (ifFalse == FilterState.TRUE_STATE) {
				branchLabel = TRUE_RETURN;
				branchWhen = false;
				inlineState = ifTrue;
				needTrueReturn = true;
			} else {
				throw new RuntimeException("Both child states are FALSE_STATE");
			}
			
			// For plain/list filters, the condition type has to be THIS in this phase, but for an item 
			// filter we can get any of the 4 condition types here. When we have only a single item,
			// item/any/all can be treated uniformly.
			//
			switch (condition.getType()) {
			case THIS:
				assembler.thisCondition(condition.getPredicateName(), condition.getPropertyName(), branchWhen, branchLabel);
				break;
			case ANY:
			case ALL:
			case ITEM:
				assembler.itemCondition(condition.getPredicateName(), condition.getPropertyName(), branchWhen, branchLabel);
				break;
			}
			
			if (inlineState.getPhase() != 3)
				assembler.jump(getBranchLabel(state, inlineState));
			else if (inlineState == FilterState.TRUE_STATE)
				addReturn(assembler, onTrueReturn);
			else
				addReturn(assembler, onFalseReturn);
		}
		
		if (needTrueReturn) {
			assembler.label(TRUE_RETURN);
			addReturn(assembler, onTrueReturn);
		}
	}
	
	private static final String ANY_ALL_LOOP_PREPARE = "ANY_ALL_LOOP_PREPARE";
	private static final String ANY_ALL_LOOP_NEXT = "ANY_ALL_LOOP_NEXT";
	private static final String ANY_ALL_LOOP_TOP = "ANY_ALL_LOOP_TOP";
	private static final String ANY_ALL_LOOP_FINISH = "ANY_ALL_LOOP_FINISH";
	private static final String RETURN_ITEMS = "RETURN_ITEMS";
	private static final String RETURN_EMPTY = "RETURN_EMPTY";

	private void generateAnyAllPhaseCode(FilterAssembler assembler, FilterStateMap stateMap) {
		List<FilterState> anyAllStates = stateMap.getAnyAllStates();
		if (anyAllStates.isEmpty())
			return;
		
		// Entry points for initializing the state variable. As we process items, the state
		// variable will evolve, reflecting the conditions that we've resolved
		
		for (FilterState state : anyAllStates) {
			if (!state.isRoot())
				break;
			
			assembler.label(beginStateLabel(state));
			assembler.setState(state.getIndex());
			assembler.jump(ANY_ALL_LOOP_PREPARE);
		}
		
		assembler.label(ANY_ALL_LOOP_PREPARE);
		
		// Iterator iter = items.iterator()
		assembler.startItems();

		// Top of the loop
		
		assembler.label(ANY_ALL_LOOP_NEXT);
		assembler.nextItem(ANY_ALL_LOOP_FINISH);
		
		// switch(state) { ... }
		
		int[] switchStates = new int[anyAllStates.size()];
		String[] switchLabels = new String[anyAllStates.size()];
		
		int j = 0;
		for (FilterState state : anyAllStates) {
			switchStates[j] = state.getIndex();
			switchLabels[j] = state.getName();
			j++;
		}
		assembler.label(ANY_ALL_LOOP_TOP);
		assembler.switchState(switchStates, switchLabels);
		
		// Some of the states below (the root states) will be targets of the switch
		// above, others are only branched from other states
		
		for (FilterState state : anyAllStates) {
			assembler.label(state.getName(), state.getFilter().toString());
			
			// From an any/all state, we can resolve any one of the unresolved
			// any/all conditions. We go through the unresolved conditions for
			// the state in order, evaluate them for the item, and take action if 
			// they produce a resolution 
			
			for (int i = 0; i < state.getChildCount(); i++) {
				Condition condition = state.getChildCondition(i);
				
				if (i != 0)
					assembler.label(state.getName() + "_" + i);

				// An any state resolves if we find an item where the condition
				// evaluates to true, an all state resolves if we find an itme
				// where the condition evaluates to false
				
				FilterState resolvedState = null;
				boolean resolveOn = false;
				
				switch (condition.getType()) {
				case ANY:
					resolveOn = true;
					resolvedState = state.getChildState(i, true);
					break;
				case ALL:
					resolveOn = false;
					resolvedState = state.getChildState(i, false);
					break;
				case THIS:
				case ITEM:
					throw new RuntimeException("Invalid condition type in any/all phase");
				}
				
				// On resolution, we either go to the next condition that could be resolved,
				// or if we've checked all conditions against this item, go to the next item.
				
				String unresolvedLabel;
				if (i == state.getChildCount() - 1)
					unresolvedLabel = ANY_ALL_LOOP_NEXT;
				else
					unresolvedLabel = state.getName() + "_" + (i + 1);
				
				assembler.itemCondition(condition.getPredicateName(), condition.getPropertyName(), !resolveOn, unresolvedLabel);
				
				// A resolution can be:
				//
				//  TRUE_STATE => return_items
				//  FALSE_STATE => return_empty
				//  go to a different any/all state => we update the state variable and start over
				//    on *this* item. Note that this may produce double evaluation of some conditions
				//    that are unresolved in both states. That is slightly inefficient but harmless.
				//  go to an item state => start the item loop in that state
				
				if (resolvedState == FilterState.TRUE_STATE) {
					assembler.returnItems();
				} else if (resolvedState == FilterState.FALSE_STATE) {
					assembler.returnEmpty();
				} else if (resolvedState.getPhase() == 1) {
					assembler.setState(resolvedState.getIndex());
					assembler.jump(ANY_ALL_LOOP_TOP);
				} else {
					assembler.jump(getBranchLabel(state, resolvedState));
				}
			}
		}
		
		assembler.label(ANY_ALL_LOOP_FINISH);
		
		// At this point, we've finished processing all items, but are still in an
		// any/all state. This means that we have still unresolved conditions - for
		// example, an "any" condition for which we haven't seen any items where
		// it was true. The state map also contains a target state for this case,
		// which must be one of:
		//
		//   TRUE_STATE - we return_items
		//   FALSE_STATE - we return_empty
		//   An item phase state - we enter the item phase with that state
		//
		// We may need to add new blocks of code for branch targets for the first
		// two actions. The third action is a branch to a label that we'll 
		// generate anyways.
		
		// switch(state) { ... }
		
		switchStates = new int[anyAllStates.size()];
		switchLabels = new String[anyAllStates.size()];
		
		boolean needReturnItems = false;
		boolean needReturnEmpty = false;
		
		j = 0;
		for (FilterState state : anyAllStates) {
			FilterState resolvedState = state.getDefaultResolvedState();
			switchStates[j] = state.getIndex();
			
			if (resolvedState == FilterState.TRUE_STATE) {
				needReturnItems = true;
				switchLabels[j]= RETURN_ITEMS;
			} else if (resolvedState == FilterState.FALSE_STATE) {
				needReturnEmpty = true;
				switchLabels[j]= RETURN_EMPTY;
			} else {
				switchLabels[j] = beginStateLabel(resolvedState);
			}
				
			j++;
		}
		
		assembler.switchState(switchStates, switchLabels);
		
		if (needReturnItems) {
			assembler.label(RETURN_ITEMS);
			assembler.returnItems();
		}
		
		if (needReturnEmpty) {
			assembler.label(RETURN_EMPTY);
			assembler.returnEmpty();
		}
	}
	
	private static final String ITEM_LOOP_PREPARE = "ITEM_LOOP_PREPARE";
	private static final String ITEM_LOOP_NEXT = "ITEM_LOOP_NEXT";
	private static final String ITEM_LOOP_FINISH = "ITEM_LOOP_FINISH";

	private void generateItemPhaseCode(FilterAssembler assembler, FilterStateMap stateMap) {
		List<FilterState> itemStates = stateMap.getItemStates();
		if (itemStates.isEmpty())
			return;

		// Entry points for initializing the state variable. Unlike the any/all state
		// machine, the state variable remains constant throughout the loop; it represents
		// the state at the *beginning* of each item; we may evolve the state during
		// processing of a particular item through branches.
		
		for (FilterState state : itemStates) {
			if (!state.isRoot())
				break;
			
			assembler.label(beginStateLabel(state));
			assembler.setState(state.getIndex());
			assembler.jump(ITEM_LOOP_PREPARE);
		}
		
		assembler.label(ITEM_LOOP_PREPARE);
		
		// List result = new ArrayList(); 
		// Iterator iter = items.iterator();
		assembler.startResult();
		assembler.startItems();
		
		// Top of loop
		
		assembler.label(ITEM_LOOP_NEXT);
		assembler.nextItem(ITEM_LOOP_FINISH);
		
		// switch(state) { ... }
		
		int rootStateCount = countRootStates(itemStates);
		int[] switchStates = new int[rootStateCount];
		String[] switchLabels = new String[rootStateCount];
		
		int j = 0;
		for (FilterState state : itemStates) {
			if (!state.isRoot())
				break;
			
			switchStates[j] = state.getIndex();
			switchLabels[j] = state.getName();
			j++;
		}
		assembler.switchState(switchStates, switchLabels);
		
		// Some of the states below (the root states) will be targets of the switch
		// above, others are only branched from other states
		
		for (FilterState state : itemStates) {
			assembler.label(state.getName(), state.getFilter().toString());
			Condition condition = state.getChildCondition(0);
				
			if (condition.getType() != ConditionType.ITEM)
				throw new RuntimeException("Invalid condition type in item phase");
			
			FilterState ifTrue = state.getChildState(0, true);
			FilterState ifFalse = state.getChildState(0, false);
			
			// When we evaluate the condition for the state, we have:
			//
			//  true => add_item | next_item | go_to_other_state
			//  false => add_item | next_item | go_to_other_state
			//
			// Both can't be add_item (or this state wouldn't exist), so at least one 
			// of the actions we take must be a simple branch, which simplifies
			// the the code generation.
			
			String branchLabel;
			boolean branchWhen;
			FilterState inlineState;

			if (ifTrue.getPhase() != 3) {
				branchLabel = ifTrue.getName();
				branchWhen = true;
				inlineState = ifFalse;
			} else if (ifFalse.getPhase() != 3) {
				branchLabel = ifFalse.getName();
				branchWhen = false;
				inlineState = ifTrue;
			} else if (ifTrue == FilterState.FALSE_STATE) {
				branchLabel = ITEM_LOOP_NEXT;
				branchWhen = true;
				inlineState = ifFalse;
			} else if (ifFalse == FilterState.FALSE_STATE) {
				branchLabel = ITEM_LOOP_NEXT;
				branchWhen = false;
				inlineState = ifTrue;
			} else {
				throw new RuntimeException("Both child states are FALSE_STATE");
			}
			
			assembler.itemCondition(condition.getPredicateName(), condition.getPropertyName(), branchWhen, branchLabel);

			if (inlineState.getPhase() != 3) {
				assembler.jump(inlineState.getName());
			} else if (inlineState == FilterState.TRUE_STATE) {
				assembler.addResultItem();
				assembler.jump(ITEM_LOOP_NEXT);
			} else {
				assembler.jump(ITEM_LOOP_NEXT);
			}
		}
		
		assembler.label(ITEM_LOOP_FINISH);
		
		// return result;
		assembler.returnResult();
	}
}
