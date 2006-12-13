<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="which" required="true" type="java.lang.String" %>

<div class="dh-subcolumn dh-subcolumn-${which}">
	<jsp:doBody/>
</div>
