<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="who" required="true" type="com.dumbhippo.server.views.PersonView" %>

<a class="dh-person-link dh-underlined-link" href="/person?who=${who.viewPersonPageId}"><c:out value="${who.name}"/></a>