<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Web Swarm</title>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/links.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
	<dht:requireLinksPersonBean who="${signin.user.id}"/>
	<dht:sidebarPerson who="${signin.user.id}">
		<dht:sidebarBoxControls title="WEB SWARM CONTROLS">
			<div class="dh-public-shares-toggle">
				<c:choose>
					<%-- this is duplicated so we can set the checked attribute --%>
					<c:when test="${links.notifyPublicShares}">
						<input id="notifyPublicShares" type="checkbox" checked="true" onclick="dh.actions.setNotifyPublicShares(false);">
					</c:when>
					<c:otherwise>
						<input id="notifyPublicShares" type="checkbox" onclick="dh.actions.setNotifyPublicShares(true);">
					</c:otherwise>
				</c:choose>
			<label for="notifyPublicShares">Get All "World" Bubbles</label>			   
			</div>
		</dht:sidebarBoxControls>		
	</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxWeb>
			<c:choose>
				<c:when test="${signin.valid}">
					<dht:requireLinksPersonBean who="${signin.user.id}"/>
					<c:if test="${links.favoritePosts.resultCount > 0}">
						<dht:zoneBoxTitle a="dhFavoritePosts">FAVES</dht:zoneBoxTitle>
						<dht:postList posts="${links.favoritePosts.results}" format="full"/>
						<dht:expandablePager pageable="${links.favoritePosts}" anchor="dhFavoritePosts"/>
						<dht:zoneBoxSeparator/>
					</c:if>

					<dht:zoneBoxTitle a="dhReceivedPosts">SHARED WITH YOU</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${links.receivedPosts.resultCount > 0}">
							<dht:postList posts="${links.receivedPosts.results}" format="full" favesMode="add-only"/>
							<dht:expandablePager pageable="${links.receivedPosts}" anchor="dhReceivedPosts"/>
						</c:when>
						<c:otherwise>
							Nothing shared with you yet!
						</c:otherwise>
					</c:choose>
					<dht:zoneBoxSeparator/>

					<dht:requireLinksGlobalBean/>
					<dht:zoneBoxTitle a="dhRecentlyShared">RECENTLY SHARED</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${linksGlobal.hotPosts.resultCount > 0}">
							<dht:postList posts="${linksGlobal.hotPosts.results}" format="full" favesMode="add-only"/>
	 					    <dht:expandablePager pageable="${linksGlobal.hotPosts}" anchor="dhRecentlyShared"/>
						</c:when>
						<c:otherwise>
							Nobody anywhere has ever shared anything!
						</c:otherwise>
					</c:choose>
					<dht:zoneBoxSeparator/>
										
					<dht:zoneBoxTitle a="dhSentPosts">SHARED BY YOU</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${links.sentPosts.resultCount > 0}">
							<dht:postList posts="${links.sentPosts.results}" format="full" favesMode="add-only"/>
							<dht:expandablePager pageable="${links.sentPosts}" anchor="dhSentPosts"/>
						</c:when>
						<c:otherwise>
							You've never shared anything!
						</c:otherwise>
					</c:choose>
				</c:when>
				<c:otherwise><%-- not signed in case --%>
					<div id="dhLinkSwarmTag">
						<img src="/images2/${buildStamp}/linkswarmtag.gif"/>
					</div>
					<div id="dhLinkSwarmSample">
						<img src="/images2/${buildStamp}/bubblesample.gif"/>
					</div>
					<div>
					<span class="dh-option-list">
					<a class="dh-option-list-option" href="/links-learnmore">Learn More</a>
					</span>
					</div>					
					<dht:zoneBoxSeparator/>
					<dht:requireLinksGlobalBean/>
					<dht:zoneBoxTitle a="dhRecentlyShared">RECENTLY SHARED</dht:zoneBoxTitle>
					<dht:postList posts="${linksGlobal.hotPosts.results}" format="full"/>
					<dht:expandablePager pageable="${linksGlobal.hotPosts}" anchor="dhRecentlyShared"/>
					<%-- not implemented yet 	
					<dht:zoneBoxTitle>QUIPS</dht:zoneBoxTitle>
					FIXME
					<dht:moreExpander open="false"/>
					--%>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
