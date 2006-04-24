<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${empty linksAnon}">
	<dh:bean id="linksAnon" class="com.dumbhippo.web.LinksAnonymousPage" scope="request"/>
</c:if>
