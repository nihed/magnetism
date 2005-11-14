<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="href" required="false" type="java.lang.String"%>
<%@ attribute name="iehref" required="false" type="java.lang.String"%>

<c:choose>
	<c:when test="${empty href}">
		<link rel="stylesheet" href="/css/sitewide.css" type="text/css" />
	</c:when>
	<c:otherwise>
		<link rel="stylesheet" href="${href}" type="text/css" />
	</c:otherwise>
</c:choose>

<!--[if lt IE 7]>
<link rel="stylesheet" href="/css/sitewide-lt-ie7.css" type="text/css" />
<c:if test="${!empty iehref}">
	<link rel="stylesheet" href="${iehref}" type="text/css" />
</c:if>
<![endif]-->
