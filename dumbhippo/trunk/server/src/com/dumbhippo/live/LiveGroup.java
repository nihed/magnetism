package com.dumbhippo.live;

import com.dumbhippo.identity20.Guid;

/**
 * Live state for a Group (currently unused)
 * 
 * @author Havoc Pennington
 *
 */
public class LiveGroup extends LiveObject {

	public LiveGroup(Guid guid) {
		super(guid);
		
	}
	
	public void discard() {
	}
	
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// note this is only OK because LiveObject is abstract, if 
	// concrete LiveObject existed this would break transitivity
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof LiveGroup))
			return false;
		LiveGroup group = (LiveGroup) arg;
		return super.equals(group);
	}	
}
