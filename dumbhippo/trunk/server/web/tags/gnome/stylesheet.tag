<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="name" required="true" type="java.lang.String"%>
<%@ attribute name="iefixes" required="false" type="java.lang.Boolean"%>

<link rel="stylesheet" href="/css-gnome/${buildStamp}/${name}.css" type="text/css" />

<c:if test="${iefixes}">
<!--[if lt IE 8]>
<link rel="stylesheet" href="/css-gnome/${buildStamp}/${name}-iefixes.css" type="text/css" />
<![endif]-->
</c:if>
