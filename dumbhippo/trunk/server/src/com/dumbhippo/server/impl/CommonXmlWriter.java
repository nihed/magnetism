package com.dumbhippo.server.impl;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
import com.dumbhippo.persistence.User;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.UserViewpoint;

/** 
 * XML-writing methods shared (or potentially shared) between HttpMethodsBean ajax calls
 * and XMPP protocol messages.
 * 
 * @author Havoc Pennington
 */
class CommonXmlWriter {
	static public final String NAMESPACE_BLOCKS = "http://dumbhippo.com/protocol/blocks";
	
	static public void writeBlocks(XmlBuilder xml, UserViewpoint viewpoint, User user, List<UserBlockData> list, String xmlns) {
		xml.openElement("blocks",
				"xmlns", xmlns, // if null then the attribute is skipped
				"count", Integer.toString(list.size()),
				"userId", user.getId(), "serverTime", Long.toString(System.currentTimeMillis()));
		for (UserBlockData ubd : list) {
			Block block = ubd.getBlock();
			xml.openElement("block", "id", block.getId(),
					"type", block.getBlockType().name(),
					"timestamp", Long.toString(block.getTimestampAsLong()),
					"clickedCount", Integer.toString(block.getClickedCount()),
					"ignored", Boolean.toString(ubd.isIgnored()),
					"ignoredTimestamp", Long.toString(ubd.getIgnoredTimestampAsLong()),
					"clicked", Boolean.toString(ubd.isClicked()),
					"clickedTimestamp", Long.toString(ubd.getClickedTimestampAsLong()));
			
			switch (block.getBlockType()) {
			case MUSIC_PERSON:
				xml.appendTextNode("musicPerson", null, "userId", block.getData1AsGuid().toString());
				break;
			case GROUP_CHAT:
				xml.appendTextNode("groupChat", null, "groupId", block.getData1AsGuid().toString());
				break;
			case POST:
				xml.appendTextNode("post", null, "postId", block.getData1AsGuid().toString());
				break;
			case GROUP_MEMBER:
				xml.appendTextNode("groupMember", null, "groupId", block.getData1AsGuid().toString(),
									   "userId", block.getData2AsGuid().toString());
				break;
			case EXT_ACCOUNT_UPDATE:
				xml.appendTextNode("extAccountUpdate", null, "userId", block.getData1AsGuid().toString(),
				  	                "accountType", Long.toString(block.getData3()));
                break;				
				// don't add a default case, we want a warning if any cases are missing
			}
			
			xml.closeElement();
		}
		xml.closeElement();		
	}
}
