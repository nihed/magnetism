<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="track" required="true" type="com.dumbhippo.server.TrackView"%>

<div class="dh-track">
	<c:if test="${!empty track.name}">
		<div class="dh-track-name"><c:out value="${track.name}"/></div>
	</c:if>
	<c:if test="${!empty track.artist}">
		<div class="dh-track-artist"><c:out value="${track.artist}"/></div>
	</c:if>
	<c:if test="${!empty track.itunesUrl}">
		<div class="dh-track-itunes-link dh-track-link">
			<a href="${track.itunesUrl}">
				<img height="15" width="61" alt="${track.name} on iTunes"
				src="http://ax.phobos.apple.com.edgesuite.net/images/badgeitunes61x15dark.gif"/>
			</a>
		</div>
	</c:if>
	<c:if test="${!empty track.yahooUrl}">
		<div class="dh-track-yahoo-link dh-track-link"><a href="${track.yahooUrl}">Yahoo!</a></div>
	</c:if>
	<c:if test="${!empty track.rhapsodyUrl}">
		<div class="dh-track-rhapsody-link dh-track-link"><a href="${track.rhapsodyUrl}">Rhapsody</a></div>
	</c:if>
</div>
