package com.dumbhippo.dm.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;

/**
 * FilterStateMap is used to convert the parsed tree of a filter into a state machine.
 * 
 * The states of the state machine fall into one of four phases:
 * 
 * 0) Computing the value of conditions that are global to all list items.
 * 1) Computing the value of "any/all" conditions (first scan through the list)
 * 2) Computing the value of "item" conditions for each item (second scan through the list)
 * 3) All conditions computed (not really a phase...)
 * 
 * But we may be able to short-circuit processing before we've computed all conditions
 * if the end-result is uniquely determined. For exampel 'true && (true || ?)) == true'.
 * 
 * The state-machine representation isn't particular useful if we only had the initial
 * global phase, since we could just evaluate the filter tree directly, but in the any/all
 * phase, we need to be able to take the conditions in whatever order they end up being
 * resolved as we go through the items. 
 * 
 * The states we work with correspond to reduced version filter expressions from
 * eliminating the conditions in the filter expression one by one.
 * 
 * For example, if we start with "(viewer.k(this) || viewer.a(all)) && viewer.i(item)"
 * and set the left-most condition  'true', then we'd get 
 * "(true || viewer.a(all)) && viewer.i(item)", but that reduces immediately,
 * to "viewer.i(item)", which is our next state.
 * 
 * The states form an acyclic directed graph - a particular state may be reachable
 * through multiple different combinatons of reduction. All graphs paths eventually
 * end up at one of two special states FilterState.TRUE_STATE or FilterState.FALSE_STATE.
 * 
 * The handling of TRUE_STATE/FALSE_STATE varies depending on the phase. For example, in 
 * the global phase, TRUE_STATE means "return the original list of items", while in 
 * the item phase, TRUE_STATE means "add the item to the list, reset the state machine
 * to the start of the item state and go to the next item. That difference is
 * handled as we generate the filter method from the state machine.
 * 
 * The main deficiency of this code is that it doesn't consider duplicate conditionals
 * to be the same thing. Duplicated conditionals of the form (a && b) || (!a && c)
 * are actually useful to combine two distinct visibility rules, so it 
 * would be nice to support them, though certainly not essential. (Another alternative
 * to achieve the same thing would be to support the ternary operator a ? b : c,
 * which is way easier, so we should look into that first if the need arises.)   
 * 
 * Handling merging all identical conditionals wouldn't be hard: you'd do a prepass 
 * first to identify and substitute duplication conditionals. Beyond that, makeState() 
 * needs to uniquify the list of returned conditionals. There are more interesting
 * optimizations of an expression tree with duplicated conditions For example, 
 * (a && c) || (b && !c) reduces to (c && !c) when a and b are evaluated
 * whch can then be recognized to false. It's not likely to be useful to implement more
 * than the simplest of such optimizations here, since we can assume that the people
 * writing the filters won't do anything too stupid. 
 */
public class FilterStateMap {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterStateMap.class);
	
	private FilterState root;
	private Map<Filter, FilterState> states = new HashMap<Filter, FilterState>();
	private List<FilterState> globalStates = new ArrayList<FilterState>();
	private List<FilterState> anyAllStates = new ArrayList<FilterState>();
	private List<FilterState> itemStates = new ArrayList<FilterState>();
	private boolean singlePhase;
	
	/**
	 * Create a new FilterStateMap for the given filter and computes the states
	 * 
	 * @param filter the filter that this map is for.
	 * @param singlePhase if we should treat all conditions as part of the
	 *    first (global) phase. This is the correct handling when we are filtering
	 *    a single item and not a list. Note that in this case there is 
	 *    still a distinction between conditions that act on the 'this' and 
	 *    conditions that act on the item, but we don't care here ... a condition
	 *    is just something that is true or false. 
	 */
	public FilterStateMap(Filter filter, boolean singlePhase) {
		this.singlePhase = singlePhase;
		
		// Sticking the singleton true false states into the state table
		// avoids us needing to handle them specially.
		states.put(TrueFilter.getInstance(), FilterState.TRUE_STATE);
		states.put(FalseFilter.getInstance(), FilterState.FALSE_STATE);
		
		root = makeState(filter);
		root.setRoot(true);
		
		// Add the root state - causes all child states to be recursively traversed
		addState(root);
		
		// Sort the different classes of states so that the root 
		// classes come first in each class, otherwise leaving them in depth-first
		// order.
		Comparator<FilterState> compare = new Comparator<FilterState>() {
			public int compare(FilterState a, FilterState b) {
				if (a.isRoot() && !b.isRoot())
					return -1;
				else if (b.isRoot() && !a.isRoot())
					return 1;
				else
					return 0;
			}			
		};
		
		Collections.sort(globalStates, compare);
		Collections.sort(anyAllStates, compare);
		Collections.sort(itemStates, compare);
		
		// Now label all the states with individual indices
		int i = 0;
		for (FilterState state : globalStates)
			state.setIndex(i++);
		for (FilterState state : anyAllStates)
			state.setIndex(i++);
		for (FilterState state : itemStates)
			state.setIndex(i++);
	}
	
	private FilterState makeState(Filter filter) {
		List<Condition> conditions = new ArrayList<Condition>();
		filter.appendConditions(conditions);
		
		if (!singlePhase) {
			Collections.sort(conditions, new Comparator<Condition>() {
				public int compare(Condition a, Condition b) {
					int phaseA = a.getType().getProcessingPhase();
					int phaseB = b.getType().getProcessingPhase();
					
					return phaseA < phaseB ? -1 : (phaseA == phaseB ? 0 : 1);
				}
			});
		}
		
		return new FilterState(filter, conditions.toArray(new Condition[conditions.size()]), singlePhase);
	}

	private void addState(FilterState newState) {
		states.put(newState.getFilter(), newState);
		
		switch (newState.getPhase()) {
		case 0:
			globalStates.add(newState);
			break;
		case 1:
			anyAllStates.add(newState);
			break;
		case 2:
			itemStates.add(newState);
			break;
		}
	
		computeChildStates(newState);
	}
	
	private FilterState computeChildState(FilterState state, Filter newFilter) {
		FilterState newState = states.get(newFilter);
		if (newState == null) {
			newState = makeState(newFilter);
			addState(newState);
		}
		
		if (newState.getPhase() != state.getPhase())
			newState.setRoot(true);
				
		return newState;
	}

	private void computeChildStates(FilterState state) {
		for (int i = 0; i < state.getChildCount(); i++) {
			state.setChildState(i, true, computeChildState(state, state.getFilter().reduce(state.getChildCondition(i), true)));
			state.setChildState(i, false, computeChildState(state, state.getFilter().reduce(state.getChildCondition(i), false)));
		}
		
		// For the any/all phase, we have to also compute the state that results if we resolve
		// all conditions to their defaults values ... this is what happens if we are still
		// in the state when we get to the end of the item list
		if (state.getPhase() == 1) {
			Filter newFilter = state.getFilter();
			
			for (int i = 0; i < state.getChildCount(); i++) {
				Condition condition = state.getChildCondition(i);
				if (condition.getType() == ConditionType.ANY)
					newFilter = newFilter.reduce(condition, false);
				else // ALL
					newFilter = newFilter.reduce(condition, true);
			}
			
			state.setDefaultResolvedState(computeChildState(state, newFilter));
		}
	}

	/**
	 * Get the root state of the tree (corresponds to the original filter passed in)
	 * 
	 * @return the root state
	 */
	public FilterState getRoot() {
		return root;
	}
	
	/**
	 * Get states for the global-condition evaluation phase. The root state will be the
	 * first state in the list.
	 * 
	 * @return the list of global states.
	 */
	public List<FilterState> getGlobalStates() {
		return globalStates;
	}

	/**
	 * Get states for the any/all-condition evaluation phase. Root states will be
	 * first in the list and are flagged as such (See StateFilter.isRoot()). A
	 * root state is one that you can enter without already being in the any/all
	 * state.
	 * 
	 * @return the list of any/all states.
	 */
	public List<FilterState> getAnyAllStates() {
		return anyAllStates;
	}

	/**
	 * Get states for the item-condition evaluation phase. Root states will be
	 * first in the list and are flagged as such (See StateFilter.isRoot()). A
	 * root state is one that you can enter without already being in the item
	 * phase.
	 * 
	 * @return the list of item states.
	 */
	public List<FilterState> getItemStates() {
		return itemStates;
	}
	
	private void appendState(StringBuilder sb, FilterState state) {
		sb.append("    ");
		sb.append(state.getName());
		sb.append(": ");
		sb.append(state.getFilter());
		if (state.isRoot())
			sb.append(" [r]");
		sb.append("\n");
		for (int i = 0; i < state.getChildCount(); i++) {
			sb.append("        ");
			sb.append(state.getChildCondition(i));
			sb.append(" => (");
			sb.append(state.getChildState(i, true).getName());
			sb.append(", ");
			sb.append(state.getChildState(i, false).getName());
			sb.append(")\n");
		}
		if (state.getDefaultResolvedState() != null) {
			sb.append("        defaults => ");
			sb.append(state.getDefaultResolvedState().getName());
			sb.append("\n");
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("GlobalPhase:\n");
		for (FilterState state : globalStates)
			appendState(sb, state);
		
		sb.append("AnyAllPhase:\n");
		for (FilterState state : anyAllStates)
			appendState(sb, state);

		sb.append("ItemPhase:\n");
		for (FilterState state : itemStates)
			appendState(sb, state);
		
		return sb.toString();
	}
}
