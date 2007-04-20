<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.TitleBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="true" type="java.lang.Boolean" %>

<c:set var="hasDescription" value="${dh:myInstanceOf(block, 'com.dumbhippo.server.blocks.TitleDescriptionBlockView') && block.description != ''}"/>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" title="${block.title}" expandable="${!oneLine && hasDescription && !chatHeader}">
	<dht3:blockLeft block="${block}" chatHeader="${chatHeader}">
		<dht3:simpleBlockTitle block="${block}" oneLine="${oneLine}" homeStack="false" spanClass="dh-stacker-block-title-generic"/>
		<c:if test="${!oneLine && hasDescription}">
			<dht3:blockDescription blockId="${blockId}" literalBody="${chatHeader}">${block.descriptionAsHtml}</dht3:blockDescription>
		</c:if>
        <c:if test="${!chatHeader && !empty block.chatId}">
			<dht3:blockContent blockId="${blockId}">
				<dht3:quipper block="${block}" blockId="${blockId}"/>
				<dht3:chatPreview block="${block}" blockId="${blockId}"/>
			</dht3:blockContent>		    
		</c:if>
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.entitySource}" showFrom="${showFrom}" chatHeader="${chatHeader}">
		<c:choose>
			<c:when test="${chatHeader}">
				<dht3:blockSentTimeAgo chatHeader="true">${block.sentTimeAgo}</dht3:blockSentTimeAgo>
			</c:when>
			<c:otherwise>
				<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
			</c:otherwise>
		</c:choose>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>
