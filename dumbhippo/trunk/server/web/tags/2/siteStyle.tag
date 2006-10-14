<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="headerOnly" required="false" type="java.lang.Boolean" %>

<c:if test="${!headerOnly}">
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/site.css"/>
</c:if>
<c:if test="${webVersion == 3}">
	<link rel="stylesheet" type="text/css" href="/css3/${buildStamp}/header.css"/>		
</c:if>
