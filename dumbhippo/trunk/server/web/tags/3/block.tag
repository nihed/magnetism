<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.BlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean" %>
<%@ attribute name="homeStack" required="false" type="java.lang.Boolean" %>

<dh:default var="oneLine" value="false"/>
<dh:default var="chatHeader" value="false"/>

<c:if test="${oneLine}">
    <div class="dh-stacker-block-one-line"/>
</c:if>   
<c:choose>
	<c:when test="${dh:enumIs(block.blockType, 'POST')}">
		<dht3:postBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
	</c:when>
	<c:when test="${dh:enumIs(block.blockType, 'MUSIC_CHAT')}">	
   		<dht3:musicBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
   	</c:when>	
	<c:when test="${dh:enumIs(block.blockType, 'MUSIC_PERSON')}">	
   		<dht3:musicBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_CHAT')}">
   		<dht3:groupChatBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_MEMBER')}">
   		<dht3:groupMemberBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'FACEBOOK_EVENT')}">
	   	<dht3:facebookBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" oneLine="${oneLine}" homeStack="${homeStack}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'FLICKR_PHOTOSET')}">
	   	<dht3:flickrPhotosetBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'GROUP_REVISION')}">
	   	<dht3:groupRevisionBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
   	</c:when>
   	<c:when test="${dh:enumIs(block.blockType, 'NETFLIX_MOVIE') && !oneLine}">
	   	<dht3:movieBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}"/>
   	</c:when>
   	<c:when test="${(dh:enumIs(block.blockType, 'AMAZON_REVIEW') || dh:enumIs(block.blockType, 'AMAZON_WISH_LIST_ITEM')) && !oneLine}">
	   	<dht3:amazonBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}"/>
   	</c:when>   	
   	
   	<%-- These next instanceof tests have to be in order of most to least specific, so we use the most elaborate display engine
   		 we know how to use for a block --%>  	
   	
   	<c:when test="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.ExternalThumbnailedPersonBlockView')}">
   		<%-- this covers e.g. PICASA and YOUTUBE and FLICKR _PERSON --%>
		<dht3:externalThumbnailedPersonBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>   	
   	</c:when>    	
   	<c:when test="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.TitleBlockView')}">
   		<%-- This covers BLOG_ENTRY, MYSPACE_PERSON, DELICIOUS_PUBLIC_BOOKMARK, etc. and oneLine NETFLIX_MOVIE, --%>
   		<%-- AMAZON_REVIEW, and AMAZON_WISH_LIST_ITEM --%>
	   	<dht3:simpleTitleDescriptionBlock block="${block}" blockId="${blockId}" offset="${offset}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}"/>
   	</c:when>  	
</c:choose>
<c:if test="${oneLine}">
    </div>
</c:if>
