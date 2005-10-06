/**
 * 
 */
package com.dumbhippo.jive;

import org.dom4j.Element;
import org.jivesoftware.messenger.vcard.VCardProvider;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.NotFoundException;

/**
 * @author hp
 *
 */
public class HippoVCardProvider implements VCardProvider {

	public Element loadVCard(String username) {
		// TODO Auto-generated method stub
		return null;
	}

	public void createVCard(String username, Element vCardElement)
			throws AlreadyExistsException {
		// TODO Auto-generated method stub

	}

	public void updateVCard(String username, Element vCardElement)
			throws NotFoundException {
		// TODO Auto-generated method stub

	}

	public void deleteVCard(String username) {
		// TODO Auto-generated method stub

	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

}
