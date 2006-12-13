<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="java.lang.String" %>

<c:if test="${empty links}">
	<dh:bean id="links" class="com.dumbhippo.web.pages.LinksPersonPage" scope="request"/>
	<jsp:setProperty name="links" property="viewedUserId" value="${who}"/>
</c:if>
