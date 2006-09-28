package com.dumbhippo.jms;

/**
 * This enumeration identifies the properties of the connection to make 
 * the JMS server. It is used instead of a pair of booleans mostly to increase 
 * clarity, and also because there is no TRANSACTED_IN_CLIENT possibility
 * (it doesn't really make sense for us to use JTA in a client.) 
 *  
 * @author otaylor
 */
public enum JmsConnectionType {
	/**
	 * A connection made from an application server, with operations
	 * tied to the session of the caller. Messages won't be sent out
	 * until the current transaction commits succesfully. 
	 */
	TRANSACTED_IN_SERVER {
		public boolean isTransacted() { return true; }
		public boolean isInServer() { return true; }
	},
	/**
	 * A connection made from an application server, without transaction
	 * handling. Messages are sent out immediately. 
	 */
	NONTRANSACTED_IN_SERVER {
		public boolean isTransacted() { return false; }
		public boolean isInServer() { return true; }
	},
	/**
	 * A connection made from a client, without transaction
	 */
	NONTRANSACTED_IN_CLIENT {
		public boolean isTransacted() { return false; }
		public boolean isInServer() { return false; }
	};
	
	public abstract boolean isTransacted();
	public abstract boolean isInServer();
	
}
