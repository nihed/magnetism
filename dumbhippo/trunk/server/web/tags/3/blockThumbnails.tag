<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.server.blocks.ThumbnailsBlockView" %>

<c:set var="thumbnails" value="${block.thumbnails}" scope="page"/>

<c:if test="${thumbnails.thumbnailCount > 0}">
    <div class="dh-thumbnail-block-border">
	    <div class="dh-thumbnail-block-thumbs">
		    <c:forEach items="${thumbnails.thumbnails}" end="${(475 / (block.thumbnails.thumbnailWidth + 20)) - 1}" var="thumbnail" varStatus="status">
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
			        <%-- not specifying width and height for the image doesn't make it stretch the image unnecessarily --%>
			        <%-- and still looks good, if necessary, we can wrap the image in a div of a set height and width --%>
				    <a title="${thumbnail.thumbnailTitle}" href="${thumbnail.thumbnailHref}"><img src="${thumbnail.thumbnailSrc}"/></a>
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
</c:if>
