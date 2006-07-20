<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="a" required="false" type="java.lang.String" %>
<%@ attribute name="title" required="false" type="java.lang.String" %>

<c:if test="${a != null}"><a name="${a}"></a></c:if>
<div class="dh-title dh-color-${zoneName}-foreground" title="${title}"><jsp:doBody/></div>
