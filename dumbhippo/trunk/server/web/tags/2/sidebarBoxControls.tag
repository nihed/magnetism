<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="more" required="false" type="java.lang.String" %>

<dht:sidebarBox title="${title}" more="${more}" boxClass="dh-controls-box">
	<jsp:doBody/>
</dht:sidebarBox>
