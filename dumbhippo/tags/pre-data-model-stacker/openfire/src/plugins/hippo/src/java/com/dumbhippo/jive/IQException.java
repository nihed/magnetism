package com.dumbhippo.jive;

import org.xmpp.packet.PacketError;

/**
 * This exception is used for returning errors from IQ handlers
 * deriving from AnnotatedIQHandler; its fields correspond pretty
 * much one-to-one to those of PacketError. There are convenience
 * static methods to create exceptions of several common types.
 * 
 * @author otaylor
 */
public class IQException extends Exception {
	private static final long serialVersionUID = 1L;
	
	PacketError.Condition condition;
	PacketError.Type type;
	
	public IQException(PacketError.Condition condition, PacketError.Type type, String message) {
		super(message);
		this.condition = condition;
		this.type = type;
	}
	
	public static IQException createBadRequest(String message) {
		return new IQException(PacketError.Condition.bad_request, 
				 			   PacketError.Type.modify, 
				               message);
	}

	public static IQException createForbidden() {
		return new IQException(PacketError.Condition.forbidden, 
				 			   PacketError.Type.auth, 
				               "Not allowed to call IQ");
	}

	public PacketError.Condition getCondition() {
		return condition;
	}

	public PacketError.Type getType() {
		return type;
	}
}
