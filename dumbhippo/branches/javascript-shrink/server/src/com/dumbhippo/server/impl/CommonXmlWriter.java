package com.dumbhippo.server.impl;

import java.util.List;

import com.dumbhippo.XmlBuilder;
import com.dumbhippo.persistence.Block;
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
			Block block = bv.getBlock();
			xml.openElement("block", "id", block.getId(),
					"type", block.getBlockType().name(),
					"timestamp", Long.toString(block.getTimestampAsLong()),
					"clickedCount", Integer.toString(block.getClickedCount()),
					"ignored", Boolean.toString(bv.getUserBlockData().isIgnored()),
					"ignoredTimestamp", Long.toString(bv.getUserBlockData().getIgnoredTimestampAsLong()),
					"clicked", Boolean.toString(bv.getUserBlockData().isClicked()),
					"clickedTimestamp", Long.toString(bv.getUserBlockData().getClickedTimestampAsLong()));
			
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
			case EXTERNAL_ACCOUNT_UPDATE:
				xml.appendTextNode("extAccountUpdate", null, "userId", block.getData1AsGuid().toString(),
				  	                "accountType", Long.toString(block.getData3()));
                break;				
			case EXTERNAL_ACCOUNT_UPDATE_SELF:
				// external account update for self and others are represented with the same xml structure
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
