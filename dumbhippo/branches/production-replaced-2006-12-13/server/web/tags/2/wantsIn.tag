<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="buttonText" required="false" type="java.lang.String" %>

<c:if test="${empty buttonText}">
	<c:set var="buttonText" value="Want In?"/>
</c:if>

<dh:script module="dh.actions"/>
<form method="post" action="/wantsin" onsubmit="return dh.actions.validateWantsIn('dhWantsInEmailEntry');">
    <input type="text" id="dhWantsInEmailEntry" name="address"/>
    <input type="submit" value="${buttonText}"/>
</form>