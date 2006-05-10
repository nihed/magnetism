<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="track" required="true" type="com.dumbhippo.server.TrackView"%>
<%@ attribute name="oneLine" required="false" type="java.lang.Boolean"%>
<%@ attribute name="albumArt" required="false" type="java.lang.Boolean"%>
<%@ attribute name="linkifySong" required="false" type="java.lang.Boolean"%>
<%@ attribute name="playItLink" required="false" type="java.lang.Boolean"%>
<%-- when we display a track as an album track, we display a track number, omit artist, --%>
<%-- and display download links even if they are all disabled --%>
<%@ attribute name="displayAsAlbumTrack" required="false" type="java.lang.Boolean"%>

<c:if test="${empty albumArt}">
	<c:set var="albumArt" value="false"/>
</c:if>

<c:if test="${empty oneLine}">
	<c:set var="oneLine" value="false"/>
</c:if>

<c:if test="${empty linkifySong}">
	<c:set var="linkifySong" value="true"/>
</c:if>

<c:if test="${empty playItLink}">
	<c:set var="playItLink" value="true"/>
</c:if>

<c:if test="${empty displayAsAlbumTrack}">
	<c:set var="displayAsAlbumTrack" value="false"/>
</c:if>

<c:url value="/artist" var="albumlink">
	<c:param name="artist" value="${track.artist}"/>
	<c:param name="album" value="${track.album}"/>
</c:url>

<c:url value="/artist" var="artistlink">
	<c:param name="artist" value="${track.artist}"/>
</c:url>

<c:choose>
	<c:when test="${albumArt}">
		<c:set var="songClass" value="dh-song dh-song-with-art" scope="page"/>
	</c:when>
	<c:when test="${oneLine}">
		<c:set var="songClass" value="dh-song dh-one-line-song" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="songClass" value="dh-song" scope="page"/>
	</c:otherwise>
</c:choose>

<div class='${songClass}'>
	<c:if test="${albumArt}">
		<div class="dh-song-image">
			<a href="${albumlink}">
				<img src="${track.smallImageUrl}" width="${track.smallImageWidth}" height="${track.smallImageHeight}"/>
			</a>
		</div>
	</c:if>
	<div class="dh-song-info">
		<c:if test="${!empty track.name}">
			<div class="dh-song-name">
				<c:if test="${displayAsAlbumTrack}">
                    <c:out value="${track.trackNumber} "/>
                </c:if> 			
				<c:if test="${linkifySong}">
					<c:url value="/artist" var="songlink">
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
		<c:if test="${!empty track.artist && !displayAsAlbumTrack}">
			<div class="dh-song-artist">
				by
				<a href="${artistlink}">
					<c:out value="${track.artist}"/>
				</a>
			</div>
		</c:if>
		<c:if test="${playItLink}">
		    <c:if test="${!empty track.itunesUrl || !empty track.yahooUrl || !empty track.rhapsodyUrl || displayAsAlbumTrack}">
				<c:set var="itunesDisabled" value='${empty track.itunesUrl ? "disabled" : ""}'/>
				<c:set var="yahooDisabled" value='${empty track.yahooUrl ? "disabled" : ""}'/>
				<c:set var="rhapsodyDisabled" value='${empty track.rhapsodyUrl ? "disabled" : ""}'/>
				<div class="dh-song-links">Play at 
					<c:choose>
						<c:when test="${!empty track.itunesUrl}">
							<a class="dh-music-source-link" href="${track.itunesUrl}">iTunes</a>,
						</c:when>
						<c:otherwise>
							<span class="dh-music-source-link-disabled">iTunes</span>,
						</c:otherwise>
					</c:choose>
					<c:choose>
						<c:when test="${!empty track.yahooUrl}">
							<a class="dh-music-source-link" href="${track.yahooUrl}">Yahoo! Music</a>
						</c:when>
						<c:otherwise>
							<span class="dh-music-source-link-disabled">Yahoo! Music</span>
						</c:otherwise>
					</c:choose>
				</div>
			</c:if>
		</c:if>		
	</div>
	<c:if test="${albumArt}">
		<div class="dh-grow-div-around-floats"></div>
	</c:if>
</div>
