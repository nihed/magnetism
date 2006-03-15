<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Invite Sent</title>
	<dht:stylesheets />
	<dht:scriptIncludes/>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/>

		<h3>${note}</h3>
                
		<c:choose>
			<c:when test="${remaining > 0}">
				<p>You can invite ${remaining} more people. <a href="/invite">Invite someone else now.</a></p>
			</c:when>
			<c:otherwise>
				<p>You can't invite anyone else for now. But don't worry, 
					we'll expand the site soon.</p>
			</c:otherwise>
		</c:choose>
		<p><a class="manage-invites" href="/invites">Manage Invites</a></p>
		<p><a href="/home">Home</a></p>
		<p><a href="/main">Main</a></p>
	</dht:mainArea>
</dht:bodyWithAds>
</html>
