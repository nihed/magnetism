<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="href" required="false" type="java.lang.String"%>
<%@ attribute name="iehref" required="false" type="java.lang.String"%>

<c:choose>
	<c:when test="${empty href}">
		<link rel="stylesheet" href="/css/${buildStamp}/sitewide.css" type="text/css" />
	</c:when>
	<c:otherwise>
		<link rel="stylesheet" href="/css/${buildStamp}/${href}" type="text/css" />
	</c:otherwise>
</c:choose>

<!--[if lt IE 8]>
<link rel="stylesheet" href="/css/${buildStamp}/sitewide-iefixes.css" type="text/css" />
<c:if test="${!empty iehref}">
	<link rel="stylesheet" href="/css/${buildStamp}/${iehref}" type="text/css" />
</c:if>
<![endif]-->

<!-- using a "permalink" on purpose here (no build stamp) -->
<link rel="icon" href="/images2/favicon.ico" />
<link rel="shortcut icon" href="/images2/favicon.ico" />
