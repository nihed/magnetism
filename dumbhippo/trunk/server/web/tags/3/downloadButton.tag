<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="url" required="true" type="java.lang.String" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>

<c:choose>
	<c:when test="${!disabled}">
		<a id="dhDownloadButton" class="dh-download-button" href="${url}"><img id="dhDownloadImage" src="/images3/${buildStamp}/download_now_button.gif"/></a>
	</c:when>
	<c:otherwise>
		<a id="dhDownloadButton" class="dh-download-button"><img id="dhDownloadImage" src="/images3/${buildStamp}/download_now_disabled.gif"/></a>
	</c:otherwise>
</c:choose>
