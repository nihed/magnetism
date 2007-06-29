package com.dumbhippo.aim;

public enum PermitDenyMode {
    PERMIT_ALL(1),
    DENY_ALL(2),
    PERMIT_SOME(3),
    DENY_SOME(4),
    PERMIT_BUDDIES(5);
    
	private int protocolValue;
	
	PermitDenyMode(int protocolValue) {
		this.protocolValue = protocolValue;
	}

	public int getProtocolValue() {
		return protocolValue;
	}
	
	public static PermitDenyMode parseInt(String value) {
		int mode = Integer.parseInt(value);
		for (PermitDenyMode m : values()) {
			if (m.getProtocolValue() == mode)
				return m;
		}
		throw new IllegalArgumentException("Invalid permit/deny mode: " + value);
	}
}
