<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="href" required="true" type="java.lang.String" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>

<div class="dh-action-link">
<a href="${href}" title="${title}"><jsp:doBody/></a>
</div>
