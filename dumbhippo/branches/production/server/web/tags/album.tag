<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="album" required="true" type="com.dumbhippo.server.views.AlbumView"%>

<c:url value="/album" var="albumlink">
	<c:param name="artist" value="${album.artist}"/>
	<c:param name="album" value="${album.title}"/>
</c:url>

<c:url value="/artist" var="artistlink">
	<c:param name="artist" value="${album.artist}"/>
	<c:param name="album" value="${album.title}"/>
</c:url>

<div class="dh-album">
	<div class="dh-album-image">
		<a href="${albumlink}">
			<img src="${album.smallImageUrl}" width="${album.smallImageWidth}" height="${album.smallImageHeight}"/>
		</a>
	</div>
	<div class="dh-album-info">
		<c:if test="${!empty album.title}">
			<div class="dh-album-title">
				<a href="${albumlink}">
					<c:out value="${album.title}"/>
				</a>
			</div>
		</c:if>
		<c:if test="${!empty album.artist}">
			<div class="dh-album-artist">
				<a href="${artistlink}">
					<c:out value="${album.artist}"/>
				</a>
			</div>
		</c:if>
	</div>
</div>
