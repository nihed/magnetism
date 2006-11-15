<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.ThumbnailsBlockView" %>

<c:set var="thumbnails" value="${block.thumbnails}" scope="page"/>

<div class="dh-thumbnail-block-border">
	<div class="dh-thumbnail-block-thumbs">
		<c:forEach items="${thumbnails.thumbnails}" end="4" var="thumbnail" varStatus="status">
			<c:choose>
				<c:when test="${status.first}">
					<c:set var="css" value="dh-thumbnail-first" />
				</c:when>
				<c:when test="${status.last}">
					<c:set var="css" value="dh-thumbnail-last" />
				</c:when>
				<c:otherwise>
					<c:set var="css" value="" />
				</c:otherwise>
			</c:choose>
			<div class="dh-thumbnail-block-thumb ${css}">
				<a title="${thumbnail.thumbnailTitle}" href="${thumbnail.thumbnailHref}"><img src="${thumbnail.thumbnailSrc}" width="${thumbnails.thumbnailWidth}" height="${thumbnails.thumbnailHeight}" /></a>
				<div class="dh-thumbnail-title"><a href="${thumbnail.thumbnailHref}"><c:out value="${thumbnail.thumbnailTitle}" /></a></div>
			</div>
		</c:forEach>
		<div class="dh-thumbnail-block-more">
			<jsp:element name="a">
				<jsp:attribute name="href"><c:out value="${block.moreThumbnailsLink}"/></jsp:attribute>
				<jsp:attribute name="title"><c:out value="${block.moreThumbnailsTitle}"/></jsp:attribute>
				<jsp:body>More...</jsp:body>
			</jsp:element>
		</div>
		<div class="dh-grow-div-around-floats"><div></div></div>
	</div>
</div>
