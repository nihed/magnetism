<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<%@ attribute name="themeId" required="true" type="java.lang.String"%>
<%@ attribute name="property" required="true" type="java.lang.String"%>
<%@ attribute name="currentValue" required="true" type="java.lang.String"%>
<%@ attribute name="label" required="true" type="java.lang.String"%>

<c:set var="nodeId" value="dh_${property}" scope="page"/>

<div>
	<span style="font-size: smaller;"><c:out value="${label}"/>:</span> <input type="text" id="${nodeId}"
		onchange="dh.nowplaying.applyValue('${themeId}', '${nodeId}', '${property}');" value="${currentValue}"/>
</div>
