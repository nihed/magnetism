<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.GroupChatBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>
<%@ attribute name="chatHeader" required="true" type="java.lang.Boolean" %>

<c:set var="cssClass" value="dh-box-${chatHeader ? 'grey' : 'orange'}${offset ? 2 : 1}"/>

<dht3:blockContainer cssClass="${cssClass}" blockId="${blockId}" title="${block.groupView.name}" expandable="${!oneLine && !chatHeader}">
	<dht3:blockLeft block="${block}" chatHeader="${chatHeader}" oneLine="${oneLine}">
	    <dht3:blockTitle>
		    <c:out value="${block.typeTitle}"/>:
	    	<c:choose>
	    		<c:when test="${block.groupView.status.canChat}">
				     <dht:actionLinkChat linkText="${block.title}" oneLine="true" chatId="${block.groupView.group.id}" kind="${chatKind}"/>
				</c:when>
				<c:otherwise>
					<c:out value="${block.title}"/>
				</c:otherwise>
			</c:choose>
        </dht3:blockTitle>
        <c:if test="${chatHeader}">
        	<div class="dh-stacker-block-header-description">
	        	<c:out value="${block.groupView.group.description}"/>
	        </div>
        </c:if>
        <c:if test="${!oneLine && !chatHeader}">
	        <dht3:stackReason block="${block}" blockId="${blockId}"/>
			<dht3:blockContent blockId="${blockId}">
				<dht3:quipper block="${block}" blockId="${blockId}"/>
				<dht3:chatPreview block="${block}" blockId="${blockId}"/>
			</dht3:blockContent>
		</c:if>
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.groupView}" showFrom="${showFrom}" chatHeader="${chatHeader}" oneLine="${oneLine}">
		<c:if test="${!chatHeader}">
			<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
		</c:if>
		<dht3:blockControls blockId="${blockId}">
			&nbsp; <%-- http://bugzilla.mugshot.org/show_bug.cgi?id=1019 --%>
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>

