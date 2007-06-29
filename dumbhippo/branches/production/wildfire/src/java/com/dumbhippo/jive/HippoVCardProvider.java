/**
 * 
 */
package com.dumbhippo.jive;

import org.dom4j.Element;
import org.jivesoftware.wildfire.vcard.VCardProvider;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.NotFoundException;

/**
 * @author hp
 *
 */
public class HippoVCardProvider implements VCardProvider {

	public Element loadVCard(String username) {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void createVCard(String username, Element vCardElement)
			throws AlreadyExistsException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void updateVCard(String username, Element vCardElement)
			throws NotFoundException {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public void deleteVCard(String username) {
		throw new UnsupportedOperationException();		
		// TODO FIXME
	}

	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

}
