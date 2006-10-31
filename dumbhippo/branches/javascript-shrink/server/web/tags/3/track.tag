<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="track" required="true" type="com.dumbhippo.server.views.TrackView" %>

<c:url value="/artist" var="songlink">
	<c:param name="track" value="${track.name}"/>
	<c:param name="artist" value="${track.artist}"/>
	<c:param name="album" value="${track.album}"/>
</c:url>
<a href="${songlink}"><c:out value="${track.truncatedName}"/></a>
