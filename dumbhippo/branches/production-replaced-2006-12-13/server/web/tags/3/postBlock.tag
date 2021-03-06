<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.PostBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="${!oneLine}">
	<dht3:blockLeft block="${block}">
		<dht3:blockTitle>
		    <%-- TODO: it is nicer to do this with display "none" in css, but for some reason could not detect --%> 
		    <%-- node.style.display == "none" in getTextFromHtmlNode() and truncateTextInHtmlNode() util.js, so --%>
		    <%-- this was messing up the length of text we can display in one line; also currently we are setting --%>
		    <%-- display = "none" for all the items we want to display in one line, so that we only display them --%>
		    <%-- when we fix up their length. --%>
		    <c:if test="${!oneLine}"> 
			    <span class="dh-stacker-block-title-type">Web Swarm:</span>
			</c:if>      
			<span class="dh-stacker-block-title-title">
				<jsp:element name="a">
					<jsp:attribute name="class">dh-underlined-link</jsp:attribute>
					<jsp:attribute name="href">/visit?post=${block.postView.identifyingGuid}</jsp:attribute>
					<jsp:body><c:out value="${block.postView.title}"/></jsp:body>
				</jsp:element>		
			</span>
		</dht3:blockTitle>
	    <dht3:blockDescription blockId="${blockId}">${block.postView.textAsHtml}</dht3:blockDescription>   
		<dht3:blockContent blockId="${blockId}">
			<dht3:chatPreview block="${block}" chatId="${block.postView.post.id}" chatKind="group" chattingCount="${block.postView.livePost.chattingUserCount}"/>
		</dht3:blockContent>		    
	</dht3:blockLeft>
	<dht3:blockRight blockId="${blockId}" from="${block.postView.poster}" showFrom="${showFrom}">
	    <c:if test="${!oneLine}"> 
		    <c:choose>
			    <c:when test="${block.postView.livePost.totalViewerCount == 1}">1 view</c:when>
			    <c:otherwise>${block.postView.livePost.totalViewerCount} views</c:otherwise>
		    </c:choose>
		    | 
		</c:if>
		<dht3:blockTimeAgo block="${block}"/>
		<dht3:blockControls blockId="${blockId}">
			<c:if test="${signin.valid}">
				<jsp:element name="a">
			  	  <jsp:attribute name="href">javascript:dh.util.openShareLinkWindow(<dh:jsString value="${block.postView.post.url}"/>, <dh:jsString value="${block.postView.post.title}"/>);</jsp:attribute>
			  	  <jsp:body>Share this</jsp:body>
			  	</jsp:element>
			</c:if>
			<dht3:blockSentTo blockId="${blockId}" who="${block.postView.recipients}"/>				
		</dht3:blockControls>				
	</dht3:blockRight>
</dht3:blockContainer>
   
