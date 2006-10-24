<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.views.PostBlockView" %>
<%@ attribute name="offset" required="true" type="java.lang.Boolean" %>
<%@ attribute name="blockId" required="true" type="java.lang.String" %>
<%@ attribute name="showFrom" required="false" type="java.lang.Boolean" %>
<%@ attribute name="oneLine" required="true" type="java.lang.Boolean" %>

<dht3:blockContainer cssClass="${offset ? 'dh-box-grey2' : 'dh-box-grey1'}" blockId="${blockId}" expandable="{!oneLine}">
	<dht3:blockHeader icon="/images3/${buildStamp}/webswarm_icon.png" blockId="${blockId}">
		<dht3:blockHeaderLeft>
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
		    <dht3:blockHeaderDescription blockId="${blockId}">${block.postView.textAsHtml}</dht3:blockHeaderDescription>   
		</dht3:blockHeaderLeft>
		<dht3:blockHeaderRight blockId="${blockId}" from="${block.postView.poster}" showFrom="${showFrom}">
			<c:choose>
				<c:when test="${block.postView.livePost.totalViewerCount == 1}">1 view</c:when>
				<c:otherwise>${block.postView.livePost.totalViewerCount} views</c:otherwise>
			</c:choose>
			| <dht3:blockTimeAgo block="${block}"/>
			<dht3:blockHeaderControls blockId="${blockId}">
				<c:if test="${signin.valid}">
					<a href="javascript:dh.actions.postHistory('${block.postView.post.id}')">History</a>
					<c:choose>
						<c:when test="${block.postView.favorite}">
							<c:if test="${favesMode != 'add-only'}">
							 | <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', false);">Remove from Faves</a>
							</c:if>
						</c:when>
						<c:otherwise>
							| <a href="javascript:dh.actions.setPostFavorite('${block.postView.post.id}', true);">Add to Faves</a>
						</c:otherwise>
					</c:choose>
				  | <jsp:element name="a">
				  	  <jsp:attribute name="href">javascript:dh.util.openShareLinkWindow(<dh:jsString value="${block.postView.post.url}"/>, <dh:jsString value="${block.postView.post.title}"/>);</jsp:attribute>
				  	  <jsp:body>Share this</jsp:body>
				  	</jsp:element>
				</c:if>
			</dht3:blockHeaderControls>	
		</dht3:blockHeaderRight>
	</dht3:blockHeader>
	<dht3:blockContent blockId="${blockId}">
		<dht3:chatPreview block="${block}" chatId="${block.postView.post.id}" chatKind="group" chattingCount="${block.postView.livePost.chattingUserCount}"/>
	</dht3:blockContent>
</dht3:blockContainer>
   
