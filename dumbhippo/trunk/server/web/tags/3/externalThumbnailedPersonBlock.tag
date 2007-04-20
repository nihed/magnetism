<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.ExternalThumbnailedPersonBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" title="${block.title}" expandable="${(block.thumbnails.thumbnailCount > 0) && !oneLine && !chatHeader}">
	<dht3:blockLeft block="${block}" chatHeader="${chatHeader}">
		<dht3:simpleBlockTitle block="${block}" oneLine="${oneLine}" homeStack="false"/>
        <c:if test="${!chatHeader}">
			<dht3:blockDescription blockId="${blockId}" literalBody="true" hideOnExpand="true">
				<span style="color: blue">View thumbnails</span>
			</dht3:blockDescription>			
		    <dht3:blockContent blockId="${blockId}">
		        <dht3:blockThumbnails block="${block}"/>
    	        <c:if test="${!empty block.chatId}">
					<dht3:quipper block="${block}" blockId="${blockId}"/>
					<dht3:chatPreview block="${block}" blockId="${blockId}"/>
				</c:if>
		    </dht3:blockContent>	
	    </c:if>
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.personSource}" showFrom="${showFrom}" chatHeader="${chatHeader}">
		<c:if test="${!chatHeader}">
			<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
		</c:if>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>
