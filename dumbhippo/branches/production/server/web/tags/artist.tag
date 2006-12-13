<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="artist" required="true" type="com.dumbhippo.server.views.ArtistView"%>
<%@ attribute name="linkifyArtist" required="false" type="java.lang.Boolean"%>

<c:if test="${empty linkifyArtist}">
	<c:set var="linkifyArtist" value="true"/>
</c:if>

<c:url value="/artist" var="artistlink">
	<c:param name="artist" value="${artist.name}"/>
</c:url>

<div class="dh-artist">
	<div class="dh-artist-info">
		<c:if test="${!empty artist.name}">
			<div class="dh-artist-zone-title">
				<c:if test="${linkifyArtist}">
					<a href="${artistlink}">
				</c:if>
				<c:out value="${artist.name}"/>
				<c:if test="${linkifyArtist}">
					</a>
				</c:if>
			</div>
		</c:if>
	</div>
</div>
