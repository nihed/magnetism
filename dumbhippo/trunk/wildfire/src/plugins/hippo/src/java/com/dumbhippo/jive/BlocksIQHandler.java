package com.dumbhippo.jive;

import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;

import org.dom4j.Element;
import org.jivesoftware.util.Log;
import org.xmpp.packet.IQ;

import com.dumbhippo.TypeUtils;
import com.dumbhippo.XmlBuilder;
import com.dumbhippo.identity20.Guid;
import com.dumbhippo.identity20.Guid.ParseException;
import com.dumbhippo.jive.annotations.IQHandler;
import com.dumbhippo.jive.annotations.IQMethod;
import com.dumbhippo.persistence.UserBlockData;
import com.dumbhippo.server.NotFoundException;
import com.dumbhippo.server.Pageable;
import com.dumbhippo.server.Stacker;
import com.dumbhippo.server.ViewStreamBuilder;
import com.dumbhippo.server.blocks.BlockView;
import com.dumbhippo.server.views.ObjectView;
import com.dumbhippo.server.views.UserViewpoint;
import com.dumbhippo.server.views.ViewStream;

@IQHandler(namespace=BlocksIQHandler.BLOCKS_NAMESPACE)
public class BlocksIQHandler extends AnnotatedIQHandler {
	static final String BLOCKS_NAMESPACE = "http://dumbhippo.com/protocol/blocks"; 
	
	@EJB
	private Stacker stacker;
	
	@EJB
	private ViewStreamBuilder viewStreamBuilder;

	public BlocksIQHandler() {
		super("Hippo blocks IQ Handler");
		Log.debug("creating BlocksIQHandler");
	}
	
	private String getBlocksXml(UserViewpoint viewpoint, String filter, String elementName, List<BlockView> views) {
		List<ObjectView> objectList = TypeUtils.castList(ObjectView.class, views);
		
		XmlBuilder xml = new XmlBuilder();
		
		xml.openElement(elementName,
					    "xmlns", BLOCKS_NAMESPACE,
					    "filter", filter,
				        "serverTime", Long.toString(System.currentTimeMillis()));
		
		ViewStream stream = viewStreamBuilder.buildStream(viewpoint, objectList);
		stream.writeToXmlBuilder(xml);
		
		xml.closeElement();
		
		return xml.toString();
	}

	private boolean parseBoolean(String value) throws IQException {
        if (value.equals("true"))
        	return true;
        else if (value.equals("false"))
        	return false;
        else
        	throw IQException.createBadRequest("Unrecognized boolean value '" + value + "'");
	}
	
	@IQMethod(name="blocks", type=IQ.Type.get)
	public void getBlocks(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
        String lastTimestampStr = child.attributeValue("lastTimestamp");
        if (lastTimestampStr == null)
        	throw IQException.createBadRequest("get/blocks IQ missing lastTimestamp attribute");
        
        String filter = child.attributeValue("filter");
        boolean filterProvided = (filter != null);
        if (!filterProvided) {
        	filter = stacker.getUserStackFilterPrefs(viewpoint.getViewer());
        } else {
        	stacker.setUserStackFilterPrefs(viewpoint.getViewer(), filter);
        }
        
        long lastTimestamp;
        try {
        	lastTimestamp = Long.parseLong(lastTimestampStr);
        } catch (NumberFormatException e) {
        	throw IQException.createBadRequest("get/blocks IQ lastTimestamp attribute not valid");
        }      
        
		Pageable<BlockView> pageable = new Pageable<BlockView>("stack");
		pageable.setPosition(0);
		pageable.setInitialPerPage(25);
		stacker.pageStack(viewpoint, viewpoint.getViewer(), pageable, lastTimestamp, filter, false);
		List<BlockView> views = pageable.getResults();
		String xml = getBlocksXml(viewpoint, filterProvided ? null : filter, "blocks", views);
        
		reply.setChildElement(XmlParser.elementFromXml(xml));
	}

	@IQMethod(name="blockHushed", type=IQ.Type.set)
	public void setBlockHushed(UserViewpoint viewpoint, IQ request, IQ reply) throws IQException {
		Element child = request.getChildElement();
		
        String blockId = child.attributeValue("blockId");
        if (blockId == null)
        	throw IQException.createBadRequest("missing blockId attribute");
        Guid blockGuid;
        try {
        	blockGuid = new Guid(blockId);
        } catch (ParseException e) {
        	throw IQException.createBadRequest("invalid blockId attribute");
        }
        
        UserBlockData userBlockData;
        try {
			userBlockData = stacker.lookupUserBlockData(viewpoint, blockGuid);
		} catch (NotFoundException e) {
        	throw IQException.createBadRequest("blockId attribute doesn't refer to a recognized block for this user");
		}
        
        String value = child.attributeValue("hushed");
        if (value == null)
        	throw IQException.createBadRequest("missing hushed attribute");
        
        boolean hushed = parseBoolean(value);
        
        stacker.setBlockHushed(userBlockData, hushed);
        
        BlockView blockView;
		try {
			blockView = stacker.loadBlock(viewpoint, userBlockData);
		} catch (NotFoundException e) {
			throw new RuntimeException("Can't load block view for the user's own block", e);
		}
        
		String xml = getBlocksXml(viewpoint, null, "blockHushed", Collections.singletonList(blockView));
		reply.setChildElement(XmlParser.elementFromXml(xml));
	}
}
