<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="selected" required="true" type="java.lang.String" %>

<c:if test="${!empty param['who']}">
	<c:set var="whoParam" value="?who=${param['who']}" scope="page"/>
</c:if>

<div>
	<dht3:pageOptionLink name="Overview" selected="${selected}" link="/person${whoParam}"/> |
	<dht3:pageOptionLink name="History" selected="${selected}" link="/history${whoParam}"/> | 
	<dht3:pageOptionLink name="Friends" selected="${selected}" link="/friends${whoParam}"/> | 	
	<dht3:pageOptionLink name="Groups" selected="${selected}" link="/groups${whoParam}"/>
</div>