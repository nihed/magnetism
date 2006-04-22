<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot Link Swarm</title>
	<link rel="stylesheet" type="text/css" href="/css2/links.css"/>
</head>
<dht:twoColumnPage>
	<dht:sidebar who="${signin.user.id}">
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
	</dht:sidebar>
	<dht:contentColumn>
		<dht:zoneBoxWeb>
			<dh:bean id="links" class="com.dumbhippo.web.LinksPage" scope="page"/>
			<dht:zoneBoxTitle>FAVES</dht:zoneBoxTitle>
			FIXME
			<dht:moreExpander open="false"/>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SHARED WITH ME</dht:zoneBoxTitle>

			<c:choose>
				<c:when test="${links.receivedPosts.size > 0}">
					<dht:postList posts="${links.receivedPosts.list}" format="full"/>
				</c:when>
				<c:otherwise>
					Nothing shared with you yet!
				</c:otherwise>
			</c:choose>
			
			<dht:moreExpander open="false"/>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle>SHARED BY ME</dht:zoneBoxTitle>

			FIXME
			
			<dht:moreExpander open="false"/>
		</dht:zoneBoxWeb>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
