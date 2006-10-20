<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.BlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${dh:enumIs(block.blockType, 'POST')}">
		<dht3:postBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'MUSIC_PERSON')}">	
   		<dht3:musicPersonBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_MEMBER')}">
   		<dht3:groupMemberBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'EXTERNAL_ACCOUNT_UPDATE') || dh:enumIs(block.blockType, 'EXTERNAL_ACCOUNT_UPDATE_SELF')}">
   		<c:choose>
   			<c:when test="${dh:enumIs(block.accountType, 'BLOG')}">
		   		<dht3:blogBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
		   	</c:when>
   			<c:when test="${dh:enumIs(block.accountType, 'FACEBOOK')}">
		   		<dht3:facebookBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
		   	</c:when>
		</c:choose>
   	</c:when>   	
</c:choose>
