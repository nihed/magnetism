<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<head>
	<title>Mugshot TV Party</title>
	<dht:siteStyle/>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/tv.css">
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
			<dht:zoneBoxTitle a="dhReceivedPosts">COMING SOON!</dht:zoneBoxTitle>
			<p>
			TV Party is one of our next steps, we're working on it right now.  And we're not creating this behind a magic curtain or anything but right here out in the exhibitionist open.  Check out our <a href="http://blog.mugshot.org/">blog</a> and keep an eye on this page for more!
			</p>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle a="dhSentPosts">PROGRESS</dht:zoneBoxTitle>
			<p>Take a look at some of these blog entries for more details.</p>
			<ul>
			  <li><a href="http://blog.mugshot.org/?p=124">TV Dinner... Date?</a></li>
			  <li><a href="http://blog.mugshot.org/?p=107">Researching TV</a></li>
			  <li><a href="http://blog.mugshot.org/?p=84">Stick your TV shows on the wall</a></li>
			  <li><a href="http://blog.mugshot.org/?cat=12">All TV Party Blog Entries</a></li>
			</ul>
			<dht:zoneBoxSeparator/>
			<dht:zoneBoxTitle a="dhSentPosts">DEVELOPMENT</dht:zoneBoxTitle>
			<p>
			Want to make sure we're doing this the right way?  Check out the <a href="http://developer.mugshot.org/wiki/TV_Party">TV Party Development</a> area to see what you can do.
			</p>
		</dht:zoneBoxTv>
	</dht:contentColumn>
</dht:twoColumnPage>
</html>
