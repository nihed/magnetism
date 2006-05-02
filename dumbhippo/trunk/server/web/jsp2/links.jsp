<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Link Swarm</title>
	<link rel="stylesheet" type="text/css" href="/css2/links.css">
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:choose>
	<c:when test="${signin.valid}">
	<dht:requireLinksPersonBean who="${signin.user.id}"/>
	<dht:sidebarPerson who="${signin.user.id}">
		<dht:sidebarBoxControls title="LINK SWARM CONTROLS" more="/spammers-and-freaks">
			<div>
				<c:choose>
					<%-- this is duplicated so we can set the checked attribute --%>
					<c:when test="${links.notifyPublicShares}">
						<input id="notifyPublicShares" type="checkbox" checked="true" onclick="dh.actions.setNotifyPublicShares(false);">
					</c:when>
					<c:otherwise>
						<input id="notifyPublicShares" type="checkbox" onclick="dh.actions.setNotifyPublicShares(true);">
					</c:otherwise>
				</c:choose>
			<label for="notifyPublicShares">Receive publicly shared links</label>			   
			</div>
			<dht:sidebarBoxSeparator/>
			<dht:sidebarBoxTitle>FREAK LIST</dht:sidebarBoxTitle>
			<div>
				<input type="checkbox"/> Spammer McSpammy
			</div>
			<div>
				<input type="checkbox"/> Spams McSpam
			</div>
		</dht:sidebarBoxControls>		
	</dht:sidebarPerson>
	</c:when>
	<c:otherwise>
		FIXME
	</c:otherwise>
	</c:choose>
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
					
					<dht:zoneBoxTitle a="dhReceivedPosts">SHARED WITH ME</dht:zoneBoxTitle>
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
					<dht:zoneBoxTitle a="dhSentPosts">SHARED BY ME</dht:zoneBoxTitle>
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
					<div>
					<b>LINK SWARM.</b> Stop spending lonely nights alone with nothing to do. Now you can be 
					alone sharing links with strangers! Live the dream.
					</div>
					<div><a href="">GET IT NOW</a></div>
					<div>FIXME screenshot goes here</div>
					<dht:zoneBoxSeparator/>
					<dht:requireLinksGlobalBean/>
					<dht:zoneBoxTitle>RECENTLY SHARED</dht:zoneBoxTitle>
					<c:choose>
						<c:when test="${linksGlobal.hotPosts.size > 0}">
							<dht:postList posts="${linksGlobal.hotPosts.list}" format="full" favesMode="add-only"/>
							<dht:moreExpander open="false"/>
						</c:when>
						<c:otherwise>
							Nobody anywhere has ever shared anything!
						</c:otherwise>
					</c:choose>					
					<dht:zoneBoxTitle>QUIPS</dht:zoneBoxTitle>
					FIXME
					<dht:moreExpander open="false"/>
				</c:otherwise>
			</c:choose>
		</dht:zoneBoxWeb>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
