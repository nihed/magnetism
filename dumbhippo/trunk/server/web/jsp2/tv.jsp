<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot TV Party</title>
	<link rel="stylesheet" type="text/css" href="/css2/tv.css">
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
</head>
<dht:twoColumnPage>
	<c:if test="${signin.valid}">
	<dht:requireLinksPersonBean who="${signin.user.id}"/>
	<dht:sidebarPerson who="${signin.user.id}">
	</dht:sidebarPerson>
	</c:if>
	<dht:contentColumn>
		<dht:zoneBoxTv>
			<dht:zoneBoxTitle a="dhReceivedPosts">Coming Soon!</dht:zoneBoxTitle>
			<p>
			TV Party is our next step, we're working on it right now.  And we're not creating this behind a magic curtain or anything but right here out in the exhibitionist open.  Check out the links below for our progress and join the <a href="">TV Party Development Group</a> to see more!
			</p>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle a="dhSentPosts">Links</dht:zoneBoxTitle>
			<ul>
				<li><a href="">Our Blog</a> with <a href="">TV Party Posts</a></li>
				<li><a href="">The <a href="">Design Process</a> we use to create new stuff like this</li>
			</ul>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
