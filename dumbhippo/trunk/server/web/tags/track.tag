<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="track" required="true" type="com.dumbhippo.server.TrackView"%>

<div class="dh-track">
	<div class="dh-track-image">
		<c:choose>
			<c:when test="${!empty track.smallImageUrl}">
				<img src="${track.smallImageUrl}" width="${track.smallImageWidth}" height="${track.smallImageHeight}"/>
			</c:when>
			<c:otherwise>
				<img src="/images/no_image_available75x75light.gif" width="75" height="75"/>
			</c:otherwise>
		</c:choose>
	</div>
	<div class="dh-track-info">
		<c:if test="${!empty track.name}">
			<div class="dh-track-name"><c:out value="${track.name}"/></div>
		</c:if>
		<c:if test="${!empty track.artist}">
			<div class="dh-track-artist"><c:out value="${track.artist}"/></div>
		</c:if>
		<div class="dh-track-links">
		<c:if test="${!empty track.itunesUrl}">
			<div class="dh-track-itunes-link dh-track-link">
				<a href="${track.itunesUrl}">
					<img height="15" width="61" alt="${track.name} on iTunes"
					src="http://ax.phobos.apple.com.edgesuite.net/images/badgeitunes61x15dark.gif"/>
				</a>
			</div>
		</c:if>
		<c:if test="${!empty track.yahooUrl}">
			<div class="dh-track-yahoo-link dh-track-link"><a href="${track.yahooUrl}">
				<img height="15" width="61" alt="${track.name} on Yahoo!"
					src="/images/badgeyahoo61x17light.gif"/></a></div>
		</c:if>
		<c:if test="${!empty track.rhapsodyUrl}">
			<div class="dh-track-rhapsody-link dh-track-link"><a href="${track.rhapsodyUrl}">Rhapsody</a></div>
		</c:if>
		</div>
	</div>
</div>
