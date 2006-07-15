<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="link" required="true" type="java.lang.String" %>
<%@ attribute name="value" required="true" type="java.lang.String" %>

<a href="${link}"><c:out value="${value}"/></a>
