<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<c:if test="${empty oneLine}">
	<c:set var="oneLine" value="false"/>
</c:if> 

<c:if test="${oneLine}">
    <div class="dh-stacker-block-one-line"/>
</c:if>   
<c:choose>
	<c:when test="${dh:enumIs(block.blockType, 'POST')}">
		<dht3:postBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'MUSIC_PERSON')}">	
   		<dht3:musicPersonBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_CHAT')}">
   		<dht3:groupChatBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_MEMBER')}">
   		<dht3:groupMemberBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'FACEBOOK_EVENT')}">
	   	<dht3:facebookBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}" homeStack="${homeStack}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'BLOG_PERSON')}">
	   	<dht3:blogBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'FLICKR_PERSON')}">
	   	<dht3:flickrPersonBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'FLICKR_PHOTOSET')}">
	   	<dht3:flickrPhotosetBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
</c:choose>
<c:if test="${oneLine}">
    </div>
</c:if>
