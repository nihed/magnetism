<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="account" required="true" type="com.dumbhippo.persistence.ExternalAccount" %>

<div class="dh-thumbnail-box-border">
	<div class="dh-thumbnail-box">
		<div class="dh-thumbnail-meta">
			<div class="dh-thumbnail-service">
				<dht:whereAtItem label="${account.siteName}" linkText="${account.linkText}" linkTarget="${account.link}"/>
			</div>
			<div class="dh-thumbnail-status">
				<c:out value="${account.totalThumbnailItemsString}" />
			</div>
		</div>
		<div class="dh-thumbnail-photos-border">
			<div class="dh-thumbnail-photos">
				<c:forEach items="${account.thumbnails}" end="4" var="thumbnail" varStatus="status">
					<c:choose>
						<c:when test="${status.first}">
							<c:set var="css" value="dh-thumbnail-photo-first" />
						</c:when>
						<c:when test="${status.last}">
							<c:set var="css" value="dh-thumbnail-photo-last" />
						</c:when>
						<c:otherwise>
							<c:set var="css" value="" />
						</c:otherwise>
					</c:choose>
					<div class="dh-thumbnail-photo ${css}">
						<a title="${thumbnail.thumbnailTitle}" href="${thumbnail.thumbnailHref}"><img src="${thumbnail.thumbnailSrc}" width="${account.thumbnailWidth}" height="${account.thumbnailHeight}" /></a>
						<div class="dh-thumbnail-title"><a href="${thumbnail.thumbnailHref}"><c:out value="${thumbnail.thumbnailTitle}" /></a></div>
					</div>
				</c:forEach>
				<div class="dh-grow-div-around-floats"><div></div></div>
			</div>
		</div>
	</div>
</div>
