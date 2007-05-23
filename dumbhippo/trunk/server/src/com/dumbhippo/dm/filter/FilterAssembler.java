package com.dumbhippo.dm.filter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CtClass;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.MethodInfo;

import org.slf4j.Logger;

import com.dumbhippo.GlobalSetup;
import com.dumbhippo.dm.DMViewpoint;

/**
 * This class implements an "assembly language" that we use when translating
 * filters into bytecode. The "assembly langugage" provides two things: first
 * it raises the level of abstraction from the raw JVM bytecode concepts to
 * things like keys and items and conditions. Second, it does the standard
 * assembler task of resolving branch labels to bytecode offsets.
 * 
 * This class works by direct generation of bytecode. An alternate way of
 * doing things that would have made the code here more compact would have
 * been to generate strings of Javassist's "Java" and let Javassist translate
 * them into bytecode. The direct bytecode approach is likely faster, but
 * that may not be a good enough reason to excuse the complexity. 
 * 
 * Usage is straightforward: you create a FilterAssembler() object, call 
 * "filter opcode" methods like jump() or setState(), and then call toCode()
 * to get a Javassist CodeAttribute object.
 * 
 * "Fop" is used in the code as an abbreviation for "filter opcode". 
 * 
 * @author otaylor
 */
public class FilterAssembler {
	@SuppressWarnings("unused")
	static private final Logger logger = GlobalSetup.getLogger(FilterAssembler.class);

	private Map<String, Integer> labels = new HashMap<String, Integer>();
	private List<Fop> fops = new ArrayList<Fop>();
	private boolean dereference;
	private boolean listFilter;
	private String viewpointClassName;
	private Class<?> objectKeyClass; 
	private String objectKeyClassName;
	private Class<?> itemKeyClass; 
	private String itemKeyClassName;
	private boolean needsBadState;

	// Can be loaded/stored with a 1 byte {a,i}{load,store}_n opcode 
	private static final int VIEWPOINT_PARAM = 1;
	private static final int KEY_PARAM = 2;
	private static final int ITEM_PARAM = 3;  // createForItemFilter
	private static final int ITEMS_PARAM = 3; // createForListFilter
	// Take 2 bytes to load/store 
	private static final int STATE_LOCAL = 4;
	private static final int ITEM_LOCAL = 5;
	private static final int ITER_LOCAL = 6;
	private static final int RESULT_LOCAL = 7;
	private static final int MAX_LOCALS = RESULT_LOCAL + 1;
	
	/**
	 * Create an assembler used to produce one of the two filter methods of CompiledFilter:
	 * 
	 * 	K1 filter(DMViewpoint viewpoint, K1 key);
	 *  T1 filter(DMViewpoint viewpoint, T1 object);
	 * 
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass class of the object's key
	 * @param dereference if true, filter DMObjects, not keys
	 * @return
	 */
	public static FilterAssembler createForFilter(Class<? extends DMViewpoint> viewpointClass, Class<?> objectKeyClass, boolean dereference) {
		return new FilterAssembler(viewpointClass, objectKeyClass, null, false, dereference);
	}
	
	/**
	 * Create an assembler used to produce one of the two filter methods of CompiledItemFilter:
	 * 
	 * 	K2 filter(DMViewpoint viewpoint, K1 key, K2 item);
  	 *  T2 filter(DMViewpoint viewpoint, T1 object, T2 item);
	 * 
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass class of the object's key
	 * @param itemKeyClass class of the item's key
	 * @param dereference if true, filter DMObjects, not keys
	 * @return
	 */
	public static FilterAssembler createForItemFilter(Class<? extends DMViewpoint> viewpointClass, Class<?> objectKeyClass, Class<?> itemKeyClass, boolean dereference) {
		return new FilterAssembler(viewpointClass, objectKeyClass, itemKeyClass, false, dereference);
	}
	
	/**
	 * Create an assembler used to produce one of the two filter methods of CompiledListFilter:
	 * 
	 * 	List<K2> filter(DMViewpoint viewpoint, K1 key, List<K2> items);
 	 *  List<T2> filter(DMViewpoint viewpoint, T1 object, List<T2> items);
	 * 
	 * @param viewpointClass subclass of DMViewpoint
	 * @param objectKeyClass class of the object's key
	 * @param itemKeyClass class of the item's key
	 * @param dereference if true, filter DMObjects, not keys
	 * @return
	 */
	public static FilterAssembler createForListFilter(Class<? extends DMViewpoint> viewpointClass, Class<?> objectKeyClass, Class<?> itemKeyClass, boolean dereference) {
		return new FilterAssembler(viewpointClass, objectKeyClass, itemKeyClass, true ,dereference);
	}
	
	private FilterAssembler(Class<? extends DMViewpoint> viewpointClass, Class<?> objectKeyClass, Class<?> itemKeyClass, boolean listFilter, boolean dereference) {
		this.dereference = dereference;
		this.listFilter = listFilter;
		viewpointClassName = Descriptor.toJvmName(viewpointClass.getName());
		this.objectKeyClass = objectKeyClass;
		objectKeyClassName = Descriptor.toJvmName(objectKeyClass.getName());
		if (itemKeyClass != null) {
			this.itemKeyClass = itemKeyClass;
			itemKeyClassName = Descriptor.toJvmName(itemKeyClass.getName());
		}
		
		// Do a cast-check for the viewpoint before anything else
		checkViewpoint();
	}
	
	/**
	 * Add a label to act as a branch target
	 * @param name name of the label
	 */
	public void label(String name) {
		label(name, null);
	}
	
	/**
	 * Add a label to act as a branch target
	 * @param name name of the label
	 * @param comment comment for this label (for debugging purposes)
	 */
	public void label(String name, String comment) {
		labels.put(name, fops.size());
		fops.add(new LabelFop(name, comment));
	}
	
	/**
	 * Jump uncoditionally to a label
	 * @param label label to branch to
	 */
	public void jump(String label) {
		fops.add(new JumpFop(label));
	}
	
	/**
	 * Set the state variable to the specified value
	 * @param state new value for the state variable
	 */
	public void setState(int state) {
		fops.add(new SetStateFop(state));
	}

	/**
	 * Jump to one of a list of labels depending on the value of the state variable
	 * If the state variable isn't one of the specified values, throws IllegalStateException
	 * (The check and throw will be optimized out if there is only a single state in 
	 * the passed in value; it's just a debugging aid.)
	 * 
	 * @param states array of states, must be consecutive ascending integers
	 *    (can be fixed if needed)
	 * @param labels label to branch to for each state
	 */
	public void switchState(int[] states, String[] labels) {
		if (states.length == 1 && labels.length == 1)
			fops.add(new JumpFop(labels[0]));
		else {
			fops.add(new SwitchStateFop(states, labels));
			needsBadState = true;
		}
	}

	/**
	 * Calls a viewpoint condition function with the object's key (or a property of the key) as the argument
	 * 
	 * @param condition the name of the condition function
	 * @param property key property to use as the argument, or null to use the key itself
	 * @param branchWhen if true, branch when the condition is true, if false, branch when the condition is false
	 * @param label label to branch to
	 */
	public void thisCondition(String condition, String property, boolean branchWhen, String label) {
		fops.add(new ThisConditionFop(condition, property, branchWhen, label));
	}
	
	/**
	 * Calls a viewpoint condition function with the current item (or a property of the current item) as the argument.
	 * If the assembler was created with createForItemFilter(), then the current item is the one passed
	 * in. If the assembler was created with createForListFilter(), then the current item is the one
	 * set by nextItem().
	 * 
	 * @param condition the name of the condition function
	 * @param property item property to use as the argument, or null to use the itemitself
	 * @param branchWhen if true, branch when the condition is true, if false, branch when the condition is false
	 * @param label label to branch to
	 */
	public void itemCondition(String condition, String property, boolean branchWhen, String label) {
		if (itemKeyClass == null)
			throw new RuntimeException("Assembler was created for a plain filter, no items");

		fops.add(new ItemConditionFop(condition, property, branchWhen, label));
	}
	
	/**
	 * Start iterating through the items array (does not actually retrieve the first item, you 
	 * must call nextItem() for that.) 
	 */
	public void startItems() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");
		
		fops.add(new StartItemsFop());
	}
	
	/**
	 * Retrieve the next item from the items array and makes it the current item. If there are
	 * no more items, branches to the given label
	 * @param label label to branch to
	 */
	public void nextItem(String label) {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new NextItemFop(label));
	}	
	
	/**
	 * Return null 
	 */
	public void returnNull() {
		if (listFilter)
			throw new RuntimeException("Assembler was created for a list filter, must return a list");

		fops.add(new ReturnNullFop());
	}	

	/**
	 * Return any empty list 
	 */
	public void returnEmpty() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new ReturnEmptyFop());
	}	

	/**
	 * Return the target object (note that this *does not* return the 'this' of the generated method)
	 */
	public void returnThis() {
		if (listFilter)
			throw new RuntimeException("Assembler was created for a list filter, must return a list");
		if (itemKeyClass != null)
			throw new RuntimeException("Assembler was created for an item filter, must return an item");

		fops.add(new ReturnThisFop());
	}	

	/**
	 * Return the item passed in 
	 */
	public void returnItem() {
		if (listFilter)
			throw new RuntimeException("Assembler was created for a list filter, must return a list");
		if (itemKeyClass == null)
			throw new RuntimeException("Assembler was created for a plain filter, can't return an item");

		fops.add(new ReturnItemFop());
	}	

	/**
	 * Return the original item list
	 */
	public void returnItems() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new ReturnItemsFop());
	}	

	/**
	 * Create a list to store result items in 
	 */
	public void startResult() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new StartResultFop());
	}	
	
	/**
	 * Add an item to the result item list 
	 */
	public void addResultItem() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new AddResultItemFop());
	}	
	
	/**
	 * Return the result item list 
	 */
	public void returnResult() {
		if (!listFilter)
			throw new RuntimeException("Assembler was not created for a list filter");

		fops.add(new ReturnResultFop());
	}
	
	/**
	 * Internal: does a cast check from DMViewpoint to the viewpoint type that was passed
	 * to the FilterAssembler constructor. 
	 */
	private void checkViewpoint() {
		fops.add(new CheckViewpointFop());
	}

	/**
	 * Internal: raises an IllegalArgumentException; used when a bad state value is found
	 * in a switchState() filter opcode.
	 */
	private void badState() {
		fops.add(new BadStateFop());
	}

	/**
	 * Generates the code and adds the filter method to the given class.
	 * 
	 * @param ctClass the class to add the method to
	 */
	public void addMethodToClass(CtClass ctClass) {
		ClassFile classFile = ctClass.getClassFile();
		String elementType = dereference ? "Lcom/dumbhippo/dm/DMObject;" : "Ljava/lang/Object;"; 
		String arg1 = "Lcom/dumbhippo/dm/DMViewpoint;";
		String arg2 = elementType;
		String arg3 = "";
		String result;
		
		if (listFilter) {
			arg3 = "Ljava/util/List;";
			result = "Ljava/util/List;";
		} else if (itemKeyClass != null) {
			arg3 = elementType;
			result = elementType;
		} else {
			result = elementType;
		}

		String descriptor = "(" + arg1 + arg2 + arg3 + ")" + result;
		
		MethodInfo methodInfo = new MethodInfo(classFile.getConstPool(), "filter", descriptor);
		methodInfo.setAccessFlags(AccessFlag.PUBLIC);
		methodInfo.setCodeAttribute(toCode(classFile.getConstPool()));

		try {
			classFile.addMethod(methodInfo);
		} catch (DuplicateMemberException e) {
			throw new RuntimeException("Filter method of this type has already been added", e);
		}
	}
	
	private CodeAttribute toCode(ConstPool constPool) {
		if (needsBadState) {
			label("BAD_STATE");
			badState();
		}
		
		// "Optimization" stage; we remove all unconditional jumps to labels immediately following
		optimize();
		
		// First pass, assign offsets to all the fops; we need the offsets to compute the
		// offsets for the branches that we emit in the second pass.
		int offset = 0;
		for (Fop fop : fops) {
			if (fop == null)
				continue;
			offset = fop.setOffset(offset);
		}
		
		int totalLength = offset;
		
		// We let the Byecode class figure out the stack size for us
		Bytecode bytecode = new Bytecode(constPool, 0, MAX_LOCALS);

		// Second pass, emit the code
		Fop previousFop = null;
		for (Fop fop : fops) {
			if (fop == null)
				continue;
			// This check makes sure that our size computations in the setOffset() methods match what
			// Javassist Bytecode's object actually generates
			if (fop.offset != bytecode.currentPc())
				throw new RuntimeException("Bad offset " + fop.offset + " should have been " + bytecode.currentPc() + " previous fop was:\n" + previousFop);
			fop.emit(bytecode);
			previousFop = fop;
		}

		// Check the size computation for the last opcode 
		if (totalLength != bytecode.currentPc())
			throw new RuntimeException("Bad total length " + totalLength + " should have been " + bytecode.currentPc() + " previous fop was:\n" + previousFop);
		
		return bytecode.toCodeAttribute();
	}
	
	/**
	 * Does basic optimization like eliminating unconditional jumps to labels immediately
	 * following. Calling addMethodToClass() automatically runs this; it's public so you
	 * can call it explicitly to get better debugging output of what code is actually
	 * generated, at a little cost in efficiency. (It's idempotent, so harmless if it's
	 * called twice.)
	 */
	public void optimize() {
		JumpFop lastJumpFop = null;
		int lastJumpIndex = -1;
		
		int i = 0;
		for (Fop fop : fops) {
			if (fop instanceof JumpFop) {
				lastJumpFop = (JumpFop)fop;
				lastJumpIndex = i;
			} else if (fop instanceof LabelFop) {
				if (lastJumpFop != null && ((LabelFop)fop).name.equals(lastJumpFop.label)) {
					fops.set(lastJumpIndex, null);
					lastJumpFop = null;
				}
			} else {
				lastJumpFop = null;
			}
			i++;
		}
	}
	
	// Compute the offset from the given base offset to the offset of the label
	private int labelOffset(int base, String label) {
		Integer labelIndex = labels.get(label);
		if (labelIndex == null)
			throw new RuntimeException("Can't resolve label " + label);
		
		Fop labelFop = fops.get(labelIndex);
		return labelFop.offset - base;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (Fop fop : fops) {
			if (fop != null)
				sb.append(fop.toString());
		}
		
		return sb.toString();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private abstract static class Fop {
		protected int offset = -1;
		
		// Adds the bytecode for this fop to the bytecode object 
		public abstract void emit(Bytecode bytecode);
		// Set the offset of this fop to the given value, return the next offset
		public abstract int setOffset(int offset);
		
		protected String prefix() {
			if (offset < 0)
				return "";
			else
				return String.format("%3d", offset);
		}
	}
	
	private class LabelFop extends Fop {
		private String name;
		private String comment;

		public LabelFop(String name, String comment) {
			this.name = name; 
			this.comment = comment;
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
		}

		@Override
		public String toString() {
			return prefix() + " " + name + ":" + (comment != null ? " // " + comment : "") + "\n";
		}
	}

	private class JumpFop extends Fop {
		private String label;

		public JumpFop(String label) {
			this.label = label; 
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 3;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addOpcode(Bytecode.GOTO);             // 1 byte
			bytecode.addIndex(labelOffset(offset, label)); // 2 bytes
		}

		@Override
		public String toString() {
			return prefix() + "    jump() => " + label + "\n";
		}
	}

	private class SetStateFop extends Fop {
		private int state;

		public SetStateFop(int state) {
			this.state = state; 
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;

			int iconstLength;
			if (state >= -1 && state <= 5)              // ICONST_<n>
				iconstLength = 1;
			else if (state >= -128 && state <= 127)     // BIPUSH
				iconstLength = 2;
			else if (state >= -65536 && state <= 65535) // SIPUSH
				iconstLength = 3;
			else
				throw new RuntimeException("State out of range");
			
			return offset + iconstLength + 2;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addIconst(state);        // 1-3 bytes
			bytecode.addIstore(STATE_LOCAL);  // 2 bytes
		}

		@Override
		public String toString() {
			return prefix() + "    setState(" + state + ")\n";
		}
	}

	private class SwitchStateFop extends Fop {
		private int states[];
		private String labels[];

		public SwitchStateFop(int[] states, String[] labels) {
			this.states = states;
			this.labels = labels;
			
			if (states.length != labels.length)
				throw new RuntimeException("Number of states doesn't match the number of labels");

			if (states.length == 0)
				throw new RuntimeException("Must be at least once state in switch");

			int min = states[0];
			for (int i = 0; i < states.length; i++) {
				if (states[i] != min + i)
					throw new RuntimeException("States don't form an ascending sequence");
			}
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			
			int afterGap = offset + 3;
			afterGap = (afterGap + 3) & ~3;
			
			return afterGap + 4 * (3 + states.length);
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			int low = states[0];
			int high = low + states.length - 1;
			
			int afterGap = offset + 3;
			afterGap = (afterGap + 3) & ~3;
			int gapSize = afterGap - (offset + 3);

			bytecode.addIload(STATE_LOCAL);                      // 2 bytes
			int base = bytecode.currentPc();
			bytecode.addOpcode(Bytecode.TABLESWITCH);            // 1 byte
			bytecode.addGap(gapSize);                            // 0-3 bytes
			bytecode.add32bit(labelOffset(base, "BAD_STATE"));   // 4 bytes
			bytecode.add32bit(low);                              // 4 bytes
			bytecode.add32bit(high);                             // 4 bytes
			for (int i = 0; i < states.length; i++) {
				bytecode.add32bit(labelOffset(base, labels[i])); // 4 bytes
			}
		}
		
		@Override
		public String toString() {
			String prefix = prefix();
			String labelPrefix = "".equals(prefix) ? "" : "   ";
			StringBuilder sb = new StringBuilder(prefix + "    switchState()\n");
			for (int i = 0; i < states.length; i++) {
				sb.append(labelPrefix + "        " + states[i] + ": " + labels[i] + "\n");
			}
			return sb.toString();
		}
	}

	private abstract class AbstractConditionFop extends Fop {
		private String condition;
		private boolean branchWhen;
		private String property;
		private String propertyMethodName;
		private String propertyClassName;
		private String label;

		public AbstractConditionFop(String condition, String property, Boolean branchWhen, String label) {
			this.condition = condition;
			this.branchWhen = branchWhen;
			this.label = label;
			
			this.property = property;
			if (property != null) {
				propertyMethodName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
				try {
					Method propertyMethod = getArgumentClass().getMethod(propertyMethodName, new Class<?>[] {});
					Class<?> propertyReturnClass = propertyMethod.getReturnType();
					if (propertyReturnClass == null)
						throw new RuntimeException("property " + property + " in key class " + objectKeyClass.getName() + " doesn't have a return value");
					propertyClassName = Descriptor.toJvmName(propertyReturnClass.getName());
				} catch (SecurityException e) {
					throw new RuntimeException("Can't find property '" + property + "' in key class " + objectKeyClass.getName(), e);
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Can't find property '" + property + "' in key class " + objectKeyClass.getName(), e);
				}
			}
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 11 + 
				(getArgumentLocalIndex() > 3 ? 1 : 0) + 
				(dereference ? 3 : 0) + 
				(dereference && listFilter ? 3 : 0) + 
				(propertyMethodName != null ? 3 : 0);
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			String argumentClassName = getArgumentClassName();
			bytecode.addAload(VIEWPOINT_PARAM);                                                           // 1 byte
			bytecode.addAload(getArgumentLocalIndex());                                                   // 1-2 bytes
			if (dereference) {
				if (listFilter)
					bytecode.addCheckcast("com/dumbhippo/dm/DMObject");                                   // 3 bytes
				bytecode.addInvokevirtual("com/dumbhippo/dm/DMObject", "getKey", "()Ljava/lang/Object;"); // 3 bytes
			}
			bytecode.addCheckcast(argumentClassName);                                                     // 3 bytes
			if (propertyMethodName != null) {
				String propertySignature = "()L" + propertyClassName + ";";
				bytecode.addInvokevirtual(argumentClassName, propertyMethodName, propertySignature);      // 3 bytes
				argumentClassName = propertyClassName;
			}
			bytecode.addInvokevirtual(viewpointClassName, condition, "(L" + argumentClassName + ";)Z");   // 3 bytes
			int base = bytecode.currentPc();
			bytecode.addOpcode(branchWhen ? Bytecode.IFNE : Bytecode.IFEQ);                               // 1 byte
			bytecode.addIndex(labelOffset(base, label));                                                  // 2 bytes
		}

		protected abstract int getArgumentLocalIndex();
		protected abstract Class<?> getArgumentClass();
		protected abstract String getArgumentClassName();
		protected abstract String getName();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(prefix() + "    " + getName() + "(");
			if (!branchWhen)
				sb.append("!");
			sb.append(condition);
			if (property != null) {
				sb.append(", ");
				sb.append(property);
			}
			sb.append(") => " + label + "\n");
			return sb.toString();
		}
	}

	private class ThisConditionFop extends AbstractConditionFop {
		public ThisConditionFop(String condition, String property, boolean branchWhen, String label) {
			super(condition, property, branchWhen, label);
		}

		@Override
		protected int getArgumentLocalIndex() {
			return KEY_PARAM;
		}
		
		@Override
		protected Class<?> getArgumentClass() {
			return objectKeyClass;
		}

		@Override
		protected String getArgumentClassName() {
			return objectKeyClassName;
		}
		
		@Override
		protected String getName() {
			return "thisCondition";
		}
	}

	private class ItemConditionFop extends AbstractConditionFop {
		public ItemConditionFop(String condition, String property, boolean branchWhen, String label) {
			super(condition, property, branchWhen, label);
		}

		@Override
		protected int getArgumentLocalIndex() {
			return listFilter ? ITEM_LOCAL : ITEM_PARAM;
		}
		
		@Override
		protected Class<?> getArgumentClass() {
			return itemKeyClass;
		}

		@Override
		protected String getArgumentClassName() {
			return itemKeyClassName;
		}
		
		@Override
		protected String getName() {
			return "itemCondition";
		}
	}

	private class StartItemsFop extends Fop {
		public StartItemsFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 8;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(ITEMS_PARAM);                                                               // 1 byte
			bytecode.addInvokeinterface("java/util/Collection", "iterator", "()Ljava/util/Iterator;", 1); // 5 bytes
			bytecode.addAstore(ITER_LOCAL);                                                               // 2 bytes
		}
		
		@Override
		public String toString() {
			return prefix() + "    startItems()\n";
		}
	}

	private class NextItemFop extends Fop {
		private String label;

		public NextItemFop(String label) {
			this.label = label;
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 19;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(ITER_LOCAL);                                                          // 2 bytes
			bytecode.addInvokeinterface("java/util/Iterator", "hasNext", "()Z", 1);                 // 5 bytes
			int base = bytecode.currentPc();
			bytecode.addOpcode(Bytecode.IFEQ);                                                      // 1 byte
			bytecode.addIndex(labelOffset(base, label));                                            // 2 bytes
			bytecode.addAload(ITER_LOCAL);                                                          // 2 bytes
			bytecode.addInvokeinterface("java/util/Iterator", "next", "()Ljava/lang/Object;", 1);   // 5 bytes
			bytecode.addAstore(ITEM_LOCAL);                                                         // 2 bytes
		}
		
		@Override
		public String toString() {
			return prefix() + "    nextItem() => " + label + "\n";
		}
	}

	private class ReturnNullFop extends Fop {
		public ReturnNullFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 2;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addOpcode(Bytecode.ACONST_NULL); // 1 byte
			bytecode.addOpcode(Bytecode.ARETURN);     // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnNull()\n";
		}
	}

	private class ReturnEmptyFop extends Fop {
		public ReturnEmptyFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 4;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addGetstatic("java/util/Collections", "EMPTY_LIST", "Ljava/util/List;"); // 3 bytes
			bytecode.addOpcode(Bytecode.ARETURN);                                             // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnEmpty()\n";
		}
	}

	private class ReturnThisFop extends Fop {
		public ReturnThisFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 2;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(KEY_PARAM);         // 1 byte
			bytecode.addOpcode(Bytecode.ARETURN); // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnThis()\n";
		}
	}

	private class ReturnItemFop extends Fop {
		public ReturnItemFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 2;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(ITEM_PARAM);        // 1 byte
			bytecode.addOpcode(Bytecode.ARETURN); // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnItem()\n";
		}
	}

	private class ReturnItemsFop extends Fop {
		public ReturnItemsFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 2;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(ITEMS_PARAM );      // 1 byte
			bytecode.addOpcode(Bytecode.ARETURN); // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnItems()\n";
		}
	}

	private class StartResultFop extends Fop {
		public StartResultFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 9;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addNew("java/util/ArrayList");                            // 3 bytes
			bytecode.addOpcode(Bytecode.DUP);                                  // 1 byte
			bytecode.addInvokespecial("java/util/ArrayList", "<init>", "()V"); // 3 bytes
			bytecode.addAstore(RESULT_LOCAL);                                  // 2 bytes
		}
		
		@Override
		public String toString() {
			return prefix() + "    startResult()\n";
		}
	}

	private class AddResultItemFop extends Fop {
		public AddResultItemFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 10;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(RESULT_LOCAL);                                                        // 2 bytes
			bytecode.addAload(ITEM_LOCAL);                                                          // 2 bytes
			bytecode.addInvokeinterface("java/util/Collection", "add", "(Ljava/lang/Object;)Z", 2); // 5 bytes
			bytecode.addOpcode(Bytecode.POP);                                                       // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    addResultItem()\n";
		}
	}

	private class ReturnResultFop extends Fop {
		public ReturnResultFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 3;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(RESULT_LOCAL);      // 2 bytes
			bytecode.addOpcode(Bytecode.ARETURN); // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    returnResult()\n";
		}
	}

	private class CheckViewpointFop extends Fop {
		public CheckViewpointFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 5;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addAload(VIEWPOINT_PARAM);         // 1 byte
			bytecode.addCheckcast(viewpointClassName);  // 3 bytes
			bytecode.addAstore(VIEWPOINT_PARAM);        // 1 byte
		}

		@Override
		public String toString() {
			return prefix() + "    checkViewpoint()\n";
		}
	}

	private class BadStateFop extends Fop {
		public BadStateFop() {
		}
		
		@Override
		public int setOffset(int offset) {
			this.offset = offset;
			return offset + 10;
		}
		
		@Override
		public void emit(Bytecode bytecode) {
			bytecode.addNew("java/lang/IllegalStateException");                                              // 3 bytes
			bytecode.addOpcode(Bytecode.DUP);                                                                // 1 byte
			bytecode.addLdc("Bad state in compiled filter");                                                 // 2 bytes
			bytecode.addInvokespecial("java/lang/IllegalStateException", "<init>", "(Ljava/lang/String;)V"); // 3 bytes
			bytecode.addOpcode(Bytecode.ATHROW);                                                             // 1 byte
		}
		
		@Override
		public String toString() {
			return prefix() + "    badState()\n";
		}
	}
}
