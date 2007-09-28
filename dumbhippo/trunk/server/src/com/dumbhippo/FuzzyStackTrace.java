package com.dumbhippo;


/** 
 * Fuzzy stack trace defines hashCode() and equals() in terms of only the first 
 * few stack frames, which is useful when you want to print a stack trace whenever
 * some code is hit, but avoid printing the same trace over and over. 
 */
public class FuzzyStackTrace {
	// obviously this is a lame magic number. It has to get through at least one big pile of 
	// AOP proxy goo. a nicer solution might be to specify the proxy method name.
	// hashCode() looks at a string per element to hash, which is pretty harsh...
	static private final int ELEMENTS_TO_HASH = 30; 
	
	private Throwable throwable;
	
	public FuzzyStackTrace(Throwable t) {
		throwable = t;
	}

	public Throwable getThrowable() {
		return throwable;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		StackTraceElement[] elements = throwable.getStackTrace();
		int i;
		for (i = 0; i < ELEMENTS_TO_HASH; ++i) {
			// to increase "fuzziness" we only consider file name and line number
			String fileName = elements[i].getFileName();
			if (fileName != null)
				result = prime * result + fileName.hashCode();
			result = prime * result + elements[i].getLineNumber();
			++i;
		}
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final FuzzyStackTrace other = (FuzzyStackTrace) obj;

		StackTraceElement[] thisElements = this.throwable.getStackTrace();
		StackTraceElement[] otherElements = other.throwable.getStackTrace();
		
		int i;
		for (i = 0; i < ELEMENTS_TO_HASH; ++i) {
			// to increase "fuzziness" we only consider file name and line number
			StackTraceElement thisElem = thisElements[i];
			StackTraceElement otherElem = otherElements[i];
			if (thisElem.getLineNumber() != otherElem.getLineNumber())
				return false;
			if (!thisElem.getFileName().equals(otherElem.getFileName()))
				return false;
			
			++i;
		}
		
		return true;
	}	
}
