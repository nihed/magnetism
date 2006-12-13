<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.PostBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${block.postView.viewerHasViewed}">
		<c:set var="linkClass" value="dh-underlined-link-visited"/>
	</c:when>
	<c:otherwise>
		<c:set var="linkClass" value="dh-underlined-link"/>
	</c:otherwise>
</c:choose>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${!oneLine}">
	<dht3:blockLeft block="${block}">
		<dht3:simpleBlockTitle block="${block}" oneLine="${oneLine}" homeStack="false"/>
		<dht3:stackReason block="${block}" blockId="${blockId}"/>
	    <dht3:blockDescription blockId="${blockId}">${block.postView.textAsHtml}</dht3:blockDescription>   
		<dht3:blockContent blockId="${blockId}">
			<dht3:chatPreview block="${block}" chatId="${block.postView.post.id}" chatKind="group" chattingCount="${block.postView.chattingUserCount}"/>
		</dht3:blockContent>		    
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.postView.poster}" showFrom="${showFrom}">
	    <c:if test="${!oneLine}"> 
		    <c:choose>
			    <c:when test="${block.postView.totalViewers == 1}">1 view</c:when>
			    <c:otherwise>${block.postView.totalViewers} views</c:otherwise>
		    </c:choose>
		    | 
		</c:if>
		<dht3:blockTimeAgo blockId="${blockId}" block="${block}"/>
		<dht3:blockControls blockId="${blockId}">
			<c:if test="${signin.valid}">
				<jsp:element name="a">
			  	  <jsp:attribute name="href">javascript:dh.util.openShareLinkWindow(<dh:jsString value="${block.postView.post.url}"/>, <dh:jsString value="${block.postView.post.title}"/>);</jsp:attribute>
			  	  <jsp:body>Share this</jsp:body>
			  	</jsp:element>
			</c:if>
			<dht3:blockSentTimeAgo>${block.postTimeAgo}</dht3:blockSentTimeAgo>
			<dht3:blockSentTo blockId="${blockId}" who="${block.postView.recipients}"/>				
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>
   
