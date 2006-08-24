<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="track" required="true" type="com.dumbhippo.server.TrackView"%>
<%@ attribute name="linkifySong" required="false" type="java.lang.Boolean"%>
<%@ attribute name="playItLink" required="false" type="java.lang.Boolean"%>
<%@ attribute name="displayTrackNumber" required="false" type="java.lang.Boolean"%>

<c:if test="${empty linkifySong}">
	<c:set var="linkifySong" value="true"/>
</c:if>

<c:if test="${empty playItLink}">
	<c:set var="playItLink" value="true"/>
</c:if>

<c:if test="${empty displayTrackNumber}">
	<c:set var="displayTrackNumber" value="false"/>
</c:if>

<c:url value="/album" var="albumlink">
	<c:param name="track" value="${track.name}"/>
	<c:param name="artist" value="${track.artist}"/>
	<c:param name="album" value="${track.album}"/>
</c:url>

<c:url value="/artist" var="artistlink">
	<c:param name="track" value="${track.name}"/>
	<c:param name="artist" value="${track.artist}"/>
	<c:param name="album" value="${track.album}"/>
</c:url>

<div class="dh-track">
	<div class="dh-track-image">
		<a href="${albumlink}">
			<img src="${track.smallImageUrl}" width="${track.smallImageWidth}" height="${track.smallImageHeight}"/>
		</a>
	</div>
	<div class="dh-track-info">  	    
		<c:if test="${!empty track.name}">
			<div class="dh-track-name">
				<c:if test="${displayTrackNumber}">
                    <c:out value="${track.trackNumber} "/>
                </c:if>  
				<c:if test="${linkifySong}">
					<c:url value="/song" var="songlink">
						<c:param name="track" value="${track.name}"/>
						<c:param name="artist" value="${track.artist}"/>
						<c:param name="album" value="${track.album}"/>
					</c:url>
					<a href="${songlink}">
				</c:if>
					<c:out value="${track.name}"/>
				<c:if test="${linkifySong}">
					</a>
				</c:if>
			</div>
		</c:if>
		<c:if test="${!empty track.artist}">
			<div class="dh-track-artist">
				<a href="${artistlink}">
					<c:out value="${track.artist}"/>
				</a>
			</div>
		</c:if>
	<c:if test="${playItLink}">
		<c:if test="${!empty track.itunesUrl || !empty track.yahooUrl || !empty track.rhapsodyUrl}">
			<div class="dh-track-links">Play It: 
			<c:if test="${!empty track.itunesUrl}">
				<div class="dh-track-link dh-track-itunes-link">
					<a class="dh-track-link dh-track-itunes-link" href="${track.itunesUrl}">
						<img height="15" width="61" alt="${track.name} on iTunes"
						src="http://ax.phobos.apple.com.edgesuite.net/images/badgeitunes61x15dark.gif"/>
					</a>
				</div>
			</c:if>
			<c:if test="${!empty track.yahooUrl}">
				<div class="dh-track-link dh-track-yahoo-link">
				  <a class="dh-track-link dh-track-yahoo-link" href="${track.yahooUrl}">
					<img height="15" width="61" alt="${track.name} on Yahoo!"
						src="/images/badgeyahoo61x17light.gif"/></a></div>
			</c:if>
			<c:if test="${!empty track.rhapsodyUrl}">
				<div class="dh-track-link dh-track-rhapsody-link"><a class="dh-track-link dh-track-rhapsody-link" href="${track.rhapsodyUrl}">Rhapsody</a></div>
			</c:if>
			</div>
		</c:if>
	</c:if>
	</div>
</div>
