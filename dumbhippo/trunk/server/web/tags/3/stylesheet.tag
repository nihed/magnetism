<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ attribute name="name" required="true" type="java.lang.String"%>
<%@ attribute name="iefixes" required="false" type="java.lang.Boolean"%>

<link rel="stylesheet" href="/css3/${buildStamp}/${name}.css" type="text/css" />

<!--[if lt IE 8]>
<c:if test="${iefixes}">
<link rel="stylesheet" href="/css3/${buildStamp}/${name}-iefixes.css" type="text/css" />
</c:if>
<![endif]-->

