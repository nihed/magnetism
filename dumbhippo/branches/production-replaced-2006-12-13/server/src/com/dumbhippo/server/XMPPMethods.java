package com.dumbhippo.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ejb.Remote;

import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;

/**
 * Methods that can be invoked from any client via our XMPP hack.
 * We assume authentication has already been completed.
 */
@Remote
public interface XMPPMethods {
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})	
	public @interface XMPPRemoted {}

	@XMPPRemoted
	public void postClicked(Guid clicker, String postId) throws NotFoundException, ParseException;
}
