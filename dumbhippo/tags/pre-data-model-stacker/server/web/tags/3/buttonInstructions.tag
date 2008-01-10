<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>

<div class="dh-buttons-instructions">
	<div class="dh-buttons-instructions-title"><c:out value="${title}"/></div>
	<div class="dh-buttons-instructions-body">
		<jsp:doBody/>
	</div>
</div>