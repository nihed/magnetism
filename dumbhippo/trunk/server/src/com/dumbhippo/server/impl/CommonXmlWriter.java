package com.dumbhippo.server.impl;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.User;
import com.dumbhippo.server.views.BlockView;
import com.dumbhippo.server.views.UserViewpoint;

/** 
 * XML-writing methods shared (or potentially shared) between HttpMethodsBean ajax calls
 * and XMPP protocol messages.
 * 
 * @author Havoc Pennington
 */
class CommonXmlWriter {
	static public final String NAMESPACE_BLOCKS = "http://dumbhippo.com/protocol/blocks";
	
	static public void writeBlocks(XmlBuilder xml, UserViewpoint viewpoint, User user, List<BlockView> list, String xmlns) {
		xml.openElement("blocks",
				"xmlns", xmlns, // if null then the attribute is skipped
				"count", Integer.toString(list.size()),
				"userId", user.getId(), "serverTime", Long.toString(System.currentTimeMillis()));
		for (BlockView bv : list) {
			bv.writeToXmlBuilder(xml);
		}
		xml.closeElement();
	}
}
