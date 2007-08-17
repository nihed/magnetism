package com.dumbhippo.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.jivesoftware.stringprep.IDNA;
import org.jivesoftware.stringprep.IDNAException;
import org.jivesoftware.stringprep.Stringprep;
import org.jivesoftware.stringprep.StringprepException;

@Entity
public class XmppResource extends Resource {
	private static final long serialVersionUID = 0L;
	
	private String jid;
	
	/**
	 * The canonical form of a JID is defined by RFC 3920, which defines how to prepare each 
	 * part of the JID in terms of the "stringprep" RFC 3454. We don't allow specification 
	 * of a JID with a resource, so our first step in canonicalization is to strip
	 * off a resource if that is found.
	 * 
	 * Note that we don't handle escaping here ... if we get a JID like 
	 * "Joe Smith@example.com" as a JID, we don't canonicalize that to the escaped form
	 * "Joe\20Smith@example.com", since it would be ambiguous what to do when got 
	 * "Joe\20Smith@exanple.com" as input. Should that be escaped to 
	 * "Joe\5c\20Smith@example.com"?
	 * 
	 * We do however handle the IDN conversion of non-ascii domain names to ASCII, since
	 * that is idempotent.
	 * 
	 * @param str the name to validate and normalize
	 * @return normalized version
	 * @throws ValidationException
	 */
	public static String canonicalize(String str) throws ValidationException {
		if (str == null)
			return null;
		
		int slash = str.indexOf('/');
		if (slash >= 0)
			str = str.substring(slash + 1);
		
		// A JID without a node is acceptable in some circumstances, but not for us
		int at = str.indexOf('@');
		if (at < 0)
			throw new ValidationException("XMPP address must be of the form name@example.com");

		String node = str.substring(0, at);
		String domain = str.substring(at + 1);
		
		String preppedNode;
		try {
			preppedNode = Stringprep.nodeprep(node);
		} catch (StringprepException e) {
			throw new ValidationException("Node in XMPP address is not valid");
		}

		String preppedDomain;
		try {
			preppedDomain = Stringprep.nameprep(IDNA.toASCII(domain), false);
		} catch (IDNAException e) {
			throw new ValidationException("Domain in XMPP address is not valid");
		} catch (StringprepException e) {
			throw new ValidationException("Domain in XMPP address is not valid");
		}
	    
	    return preppedNode + "@" + preppedDomain;
	}
	
	protected XmppResource() {}

	public XmppResource(String jid) throws ValidationException {
		internalSetJid(jid);
	}
	
	@Column(unique=true, nullable=false)
	public String getJid() {
		return jid;
	}

	private void internalSetJid(String jid) throws ValidationException {
		if (jid != null) {
			jid = canonicalize(jid);
		}
		this.jid = jid;
	}
	
	protected void setJid(String jid) {
		try {
			internalSetJid(jid);
		} catch (ValidationException e) {
			throw new RuntimeException("Database contained invalid screen name", e);
		}
	}

	@Transient
	private String getNode() {
		int at = jid.indexOf('@');
		return jid.substring(0, at);
	}
	
	@Transient
	private String getDomain() {
		int at = jid.indexOf('@');
		return jid.substring(at + 1);
	}
	
	@Override
	@Transient
	public String getHumanReadableString() {
		// FIXME: we should possibly unescape the node here
		return getNode() + "@" + IDNA.toUnicode(getDomain());
	}
	
	@Override
	@Transient
	public String getDerivedNickname() {
		// FIXME: we should possibly unescape here
		return getNode();
	}
}
