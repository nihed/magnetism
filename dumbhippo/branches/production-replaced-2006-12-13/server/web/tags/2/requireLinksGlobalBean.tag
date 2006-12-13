<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${empty linksGlobal}">
	<dh:bean id="linksGlobal" class="com.dumbhippo.web.pages.LinksGlobalPage" scope="request"/>
</c:if>
