<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="name" required="false" type="java.lang.String"%>
<%@ attribute name="iefixes" required="false" type="java.lang.Boolean"%>

<c:choose>
	<c:when test="${empty name}">
		<link rel="stylesheet" href="/css3/${buildStamp}/site.css" type="text/css" />
	</c:when>
	<c:otherwise>
		<link rel="stylesheet" href="/css3/${buildStamp}/${name}.css" type="text/css" />
	</c:otherwise>
</c:choose>

<!--[if lt IE 8]>
<link rel="stylesheet" href="/css3/${buildStamp}/site-iefixes.css" type="text/css" />
<c:if test="${!empty name}">
	<link rel="stylesheet" href="/css3/${buildStamp}/${name}-iefixes.css" type="text/css" />
</c:if>
<![endif]-->

