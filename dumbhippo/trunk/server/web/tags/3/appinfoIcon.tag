<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="upload" required="true" type="com.dumbhippo.server.applications.AppinfoUploadView" %>
<%@ attribute name="icon" required="true" type="com.dumbhippo.server.applications.AppinfoIcon" %>

<c:choose>
	<c:when test="${icon.size == 'scalable'}">
		<embed src="/files/appinfo-icon/${upload.upload.id}${icon.queryString}" type="image/svg+xml" width="64" height="64"
		    pluginspage="http://www.adobe.com/svg/viewer/install">
		</embed>
	</c:when>
	<c:when test="${icon.nominalSize != -1}">
		<img src="/files/appinfo-icon/${upload.upload.id}${icon.queryString}" width="${icon.nominalSize}" height="${icon.nominalSize}"/>
	</c:when>
	<c:otherwise>
		<img src="/files/appinfo-icon/${upload.upload.id}${icon.queryString}"/>
	</c:otherwise>
</c:choose>
