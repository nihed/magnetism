<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="name" required="false" type="java.lang.String"%>
<%@ attribute name="iefixes" required="false" type="java.lang.Boolean"%>
<%@ attribute name="noSite" required="false" type="java.lang.Boolean"%>

<c:if test="${!noSite}">
	<link rel="stylesheet" href="/css3/${buildStamp}/site.css" type="text/css" />
</c:if>
<c:if test="${!empty name}">
	<link rel="stylesheet" href="/css3/${buildStamp}/${name}.css" type="text/css" />
</c:if>

<!--[if lt IE 8]>
<c:if test="${!empty name}">
	<c:if test="${!noSite}">
		<link rel="stylesheet" href="/css3/${buildStamp}/site-iefixes.css" type="text/css" />
	</c:if>
	<link rel="stylesheet" href="/css3/${buildStamp}/${name}-iefixes.css" type="text/css" />
</c:if>
<![endif]-->

