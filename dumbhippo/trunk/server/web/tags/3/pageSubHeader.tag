<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>

<div class="dh-page-title-container">
	<span class="dh-page-title"><c:out value="${title}"/></span>
	<span class="dh-page-options">	
		<jsp:doBody/>
	</span>
</div>