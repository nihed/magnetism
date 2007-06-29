package com.dumbhippo.validator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Class that scans class files and checks stuff about them - set it up to "Run As..." with your 
 * Eclipse output folder or ant Java output folder as argument
 * 
 * @author hp
 *
 */
class Scanner implements ClassVisitor {

	static private class MethodInfo {
		private String name;
		private String descriptor;
		
		MethodInfo(String name, String descriptor) {
			this.name = name;
			this.descriptor = descriptor;
		}
		
		String getName() {
			return name;
		}
		
		String getDescriptor() {
			return descriptor;
		}
		
		String makeFullyScopedString(String ownerClassName) {
			return makeFullyScopedMethodString(ownerClassName, getName(), getDescriptor());
		}
	}
	
	static private class MethodReferenceInfo extends MethodInfo {
		private String ownerClassName;
		
		MethodReferenceInfo(String ownerClassName, String name, String descriptor) {
			super(name, descriptor);
			this.ownerClassName = ownerClassName;
		}

		String getOwnerClassName() {
			return ownerClassName;
		}
		 
		String makeFullyScopedString() {
			return super.makeFullyScopedString(ownerClassName);
		}
	}
	
	static private String makeFullyScopedMethodString(String ownerClassName, String name, String descriptor) {
		return ownerClassName + ":" + name + ":" + descriptor;
	}
	
	static private class ClassInfo {
		private File fromFile;
		private String name;
		private List<MethodReferenceInfo> methodReferences;
		private List<MethodInfo> methodDefinitions;
		private String superClassName;
		private String[] ifaceNames;
		
		ClassInfo(File fromFile, String name, String superClassName, String[] ifaceNames) {
			this.fromFile = fromFile;
			this.name = name;
			this.superClassName = superClassName;
			this.ifaceNames = ifaceNames;
		}
	
		String getFromFile() { 
			return fromFile.getAbsolutePath();
		}
		
		String getName() {
			return name;
		}
		
		void addMethodReference(MethodReferenceInfo info) {
			if (methodReferences == null)
				methodReferences = new ArrayList<MethodReferenceInfo>();
			methodReferences.add(info);
		}
		
		void addMethodDefinition(MethodInfo info) {
			if (methodDefinitions == null)
				methodDefinitions = new ArrayList<MethodInfo>();
			methodDefinitions.add(info);
		}

		public List<MethodInfo> getMethodDefinitions() {
			if (methodDefinitions == null)
				return Collections.emptyList();
			return methodDefinitions;
		}

		public List<MethodReferenceInfo> getMethodReferences() {
			if (methodReferences == null)
				return Collections.emptyList();
			return methodReferences;
		}

		public String[] getIfaceNames() {
			return ifaceNames;
		}

		public String getSuperClassName() {
			return superClassName;
		}
	}
	
	private Map<String,ClassInfo> classes;
	private List<ClassInfo> classStack;
	private File currentFile;
	
	Scanner() {
		classes = new HashMap<String,ClassInfo>();
		classStack = new ArrayList<ClassInfo>();
	}
	
	static private void debug(String s, Object... args) {
		for (Object a : args) {
			int i = s.indexOf("{}");
			if (i < 0)
				throw new RuntimeException("wrong number of args to debug()");
			s = s.substring(0, i) + (a != null ? a.toString() : "null") + s.substring(i + 2);
		}
		System.err.println(s);
	}
	
	private ClassInfo getCurrentClass() {
		return classStack.get(classStack.size() - 1);
	}
	
	private void pushClass(ClassInfo info) {
		classStack.add(info);
	}
	
	private void popClass() {
		ClassInfo info = getCurrentClass();
		classStack.remove(classStack.size() - 1);
		
		ClassInfo old = classes.get(info.getName());
		
		if (old != null)
			throw new RuntimeException("class " + info.getName() + " defined twice? " + old.getFromFile() + " and " + info.getFromFile()); 
		classes.put(info.getName(), info);
	}
	
	private static class RecordReferencesMethodVisitor implements MethodVisitor {

		private ClassInfo klass;
		
		public RecordReferencesMethodVisitor(ClassInfo klass) {
			this.klass = klass;
		}

		public AnnotationVisitor visitAnnotationDefault() {
			// TODO Auto-generated method stub
			return null;
		}

		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1, boolean arg2) {
			// TODO Auto-generated method stub
			return null;
		}

		public void visitAttribute(Attribute arg0) {
			// TODO Auto-generated method stub
			
		}

		public void visitCode() {
			// TODO Auto-generated method stub
			
		}

		public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
			// TODO Auto-generated method stub
			
		}

		public void visitInsn(int arg0) {
			// TODO Auto-generated method stub
			
		}

		public void visitIntInsn(int arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitVarInsn(int arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitTypeInsn(int arg0, String arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
			// TODO Auto-generated method stub
			
		}

		public void visitMethodInsn(int opcode, String ownerClassName, String methodName, String methodDescriptor) {
			//debug("{} reference to {} on {}", klass.getName(), methodName, ownerClassName);
			klass.addMethodReference(new MethodReferenceInfo(ownerClassName, methodName, methodDescriptor));
		}

		public void visitJumpInsn(int arg0, Label arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitLabel(Label arg0) {
			// TODO Auto-generated method stub
			
		}

		public void visitLdcInsn(Object arg0) {
			// TODO Auto-generated method stub
			
		}

		public void visitIincInsn(int arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label[] arg3) {
			// TODO Auto-generated method stub
			
		}

		public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
			// TODO Auto-generated method stub
			
		}

		public void visitMultiANewArrayInsn(String arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String arg3) {
			// TODO Auto-generated method stub
			
		}

		public void visitLocalVariable(String arg0, String arg1, String arg2, Label arg3, Label arg4, int arg5) {
			// TODO Auto-generated method stub
			
		}

		public void visitLineNumber(int arg0, Label arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitMaxs(int arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}

		public void visitEnd() {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public void visit(int version, int access, String name, String signature, String superName, String[] ifaces) {
		//debug("class name {}", name);
		pushClass(new ClassInfo(currentFile, name, superName, ifaces));
	}

	public void visitSource(String source, String debug) {
		//debug("visitSource {} {}", source, debug);
	}

	public void visitOuterClass(String owner, String name, String desc) {
		//debug("visitOuterClass owner={} name={} desc=" + desc, owner, name);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visibleAtRuntime) {
		//debug("visiting annotation {}", desc);
		return null;
		/*
		AnnotationVisitor visitor;
		
		visitor = new NoopAnnotationVisitor(0);
		
		return visitor;
		*/
	}

	public void visitAttribute(Attribute attr) {
		//debug("visitAttribute");
	}

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		//debug("visitInnerClass name={} outerName={} innerName=" + innerName, name, outerName);
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		//debug("visitField name={} desc={} signature=" + signature, name, desc);
		return null;
	}

	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		//debug("visitMethod name={} desc={} signature=" + signature, name, descriptor);
		getCurrentClass().addMethodDefinition(new MethodInfo(name, descriptor));
		return new RecordReferencesMethodVisitor(getCurrentClass());
	}

	public void visitEnd() {
		//debug("visitEnd");
		popClass();
	}
	
	boolean isMethodDeclaredInSuper(ClassInfo info, String methodName, String methodDescriptor) {
		if (isMethodDeclaredInClass(info.getSuperClassName(), methodName, methodDescriptor))
			return true;
		for (String iface : info.getIfaceNames()) {
			if (isMethodDeclaredInClass(iface, methodName, methodDescriptor))
				return true;
		}
		return false;
	}
	
	static private String removeReturnValueFromDescriptor(String methodDescriptor) {
		// a descriptor is like "(argtypes)returntype"
		int i = methodDescriptor.lastIndexOf(')');
		if (i < 0)
			throw new RuntimeException("method descriptor has no parens in it");
		return methodDescriptor.substring(0, i + 1);
	}
	
	boolean isMethodDeclaredInClass(String className, String methodName, String methodDescriptor) {
		ClassInfo info = classes.get(className);
		if (info == null)
			return false;
		for (MethodInfo methodInfo : info.getMethodDefinitions()) {
			if (methodInfo.getName().equals(methodName)) {
				String noReturnDescriptor = removeReturnValueFromDescriptor(methodDescriptor);
				String s = removeReturnValueFromDescriptor(methodInfo.getDescriptor()); 
				if (s.equals(noReturnDescriptor))
					return true;
			}
		}

		return isMethodDeclaredInSuper(info, methodName, methodDescriptor);
	}
	
	void printUnusedMethods() {
		
		// This is a cheesy algorithm to find completely unreferenced stuff. To be more useful, 
		// we need a set of rules to find "gc roots" (starting points: in the web tier, annotated
		// as an EJB hook, has HttpMethods type annotations, etc.) and then run a mark and sweep.
		
		Set<String> referencedMethods = new HashSet<String>();  
		for (ClassInfo classInfo : classes.values()) {
			for (MethodReferenceInfo methodInfo : classInfo.getMethodReferences()) {
				referencedMethods.add(methodInfo.makeFullyScopedString());
			}
			
			// Each method that is an override or implementation counts as used; the version 
			// in the interface or superclass should show up as unused if appropriate
			for (MethodInfo methodInfo : classInfo.getMethodDefinitions()) {
				if (isMethodDeclaredInSuper(classInfo, methodInfo.getName(), methodInfo.getDescriptor()))
					referencedMethods.add(methodInfo.makeFullyScopedString(classInfo.getName()));
			}
		}
		
		int usedCount = 0;
		int unusedCount = 0;
		
		for (ClassInfo classInfo : classes.values()) {
			// we only care about unused session bean interfaces for now
			if (!classInfo.getName().startsWith("com/dumbhippo/server/"))
				continue;
			
			if (classInfo.getName().substring("com/dumbhippo/server/".length()).contains("/"))
				continue;
			
			for (MethodInfo methodInfo : classInfo.getMethodDefinitions()) {
				if (methodInfo.getName().equals("<init>") ||
						methodInfo.getName().equals("<clinit>"))
					continue;
				
				if (referencedMethods.contains(methodInfo.makeFullyScopedString(classInfo.getName()))) {
					usedCount += 1;
				} else {
					unusedCount += 1;
					debug("{} in class {} appears unused, signature={}", methodInfo.getName(), classInfo.getName(), methodInfo.getDescriptor());
				}
			}
		}
		
		debug("{} methods used, {} methods not used", usedCount, unusedCount);
	}
	
	void processClassFile(File classFile) throws IOException {
		InputStream is;
		is = new FileInputStream(classFile);
		ClassReader reader;
		reader = new ClassReader(is);

		// the feature scanner code isn't very paranoid, and can probably 
		// throw all kinds of exceptions with weird input; so we just 
		// catch them all
		try {
			currentFile = classFile;
			reader.accept(this, ClassReader.SKIP_DEBUG);
		} catch (Throwable t) {
			throw new RuntimeException(t);
			//throw new IOException("Error reading class file " + classFile + ": " +  t.getClass().getName() + ": " + t.getMessage());
		} finally {
			currentFile = null;
		}
	}
	
	void processClassDirectory(File classDirectory) throws IOException {
		File[] listing = classDirectory.listFiles();
		for (File f : listing) {
			if (f.isDirectory())
				processClassDirectory(f);
			else if (f.getName().endsWith(".class"))
				processClassFile(f);
		}
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.err.println("Give a directory of .class files as an argument");
			System.exit(1);
		}
		Scanner scanner = new Scanner();
		for (String a : args) {
			File f = new File(a);
			if (f.isDirectory()) {
				scanner.processClassDirectory(f);
			} else {
				System.err.println("Not a directory: " + f.getAbsolutePath());
			}
		}
		scanner.printUnusedMethods();
	}
}
