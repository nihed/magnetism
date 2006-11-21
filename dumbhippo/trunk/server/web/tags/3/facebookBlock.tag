<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.FacebookBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<%-- this tag displays a single facebook event, the first one in the list of facebook events --%>
<%-- in FacebookBlockView; currently all FacebookBlockViews should have a single event associated --%>
<%-- with them --%>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${(block.thumbnails.thumbnailCount > 0) && !oneLine}">
	<dht3:blockLeft block="${block}">
	    <dht3:simpleBlockTitle block="${block}" oneLine="${oneLine}" homeStack="${homeStack}" image="/images3/${buildStamp}/lock_icon.png"/>
	    <dht3:blockDescription blockId="${blockId}">
	    </dht3:blockDescription>
	    <dht3:blockContent blockId="${blockId}">
	        <dht3:blockThumbnails block="${block}"/> 
	    </dht3:blockContent>			
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}">
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
	</dht3:blockRight>
</dht3:blockContainer>
