<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="application" required="true" type="com.dumbhippo.server.applications.ApplicationView" %>

<div class="dh-applications-application-stats">
	<div class="dh-applications-rank"><c:out value="${application.application.rank}"/></div>
	<div class="dh-applications-usage"><c:out value="${dh:format1('%,d', application.application.usageCount)}"/></div>
</div>
