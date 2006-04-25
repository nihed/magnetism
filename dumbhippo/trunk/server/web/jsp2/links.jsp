<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Link Swarm</title>
	<link rel="stylesheet" type="text/css" href="/css2/links.css"/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${signin.user.id}">
		<dht:sidebarBoxControls title="LINK SWARM CONTROLS" more="/spammers-and-freaks">
			<div>
				<input type="checkbox"/> Receive publicly shared links
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
	<dht:contentColumn>
		<dht:zoneBoxWeb>
			<c:choose>
				<c:when test="${signin.valid}">
					<dht:requireLinksPersonBean who="${signin.user.id}"/>
					<c:if test="${links.favoritePosts.size > 0}">
						<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
						<dht:postList posts="${links.favoritePosts.list}" format="full"/>
						<dht:moreExpander open="false"/>
						<dht:zoneBoxSeparator/>
					</c:if>
					<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>
		
					<c:choose>
						<c:when test="${links.receivedPosts.size > 0}">
							<dht:postList posts="${links.receivedPosts.list}" format="full" favesMode="add-only"/>
							<dht:moreExpander open="false"/>
						</c:when>
						<c:otherwise>
							Nothing shared with you yet!
						</c:otherwise>
					</c:choose>
					
					<dht:zoneBoxSeparator/>
					<dht:zoneBoxTitle>SHARED BY ME</dht:zoneBoxTitle>
		
					<c:choose>
						<c:when test="${links.sentPosts.size > 0}">
							<dht:postList posts="${links.sentPosts.list}" format="full" favesMode="add-only"/>
							<dht:moreExpander open="false"/>
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
