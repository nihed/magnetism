<jsp:root
	version="1.2"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:dh="urn:jsptld:/jsp/dumbhippo.tld"
	xmlns:dht="urn:jsptagdir:/WEB-INF/tags/2"
	xmlns:dht3="urn:jsptagdir:/WEB-INF/tags/3">
<jsp:directive.attribute name="who" required="true" type="com.dumbhippo.server.views.EntityView"/>
<jsp:directive.attribute name="imageOnly" required="false" type="java.lang.Boolean"/>

<jsp:text><c:choose><c:when test="${imageOnly}"><dh:png src="${who.photoUrl30}" style="width: 30px; height: 30px"/></c:when><c:otherwise><c:out value="${who.name}"/></c:otherwise></c:choose></jsp:text>
</jsp:root>

