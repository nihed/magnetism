<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="name" required="true" type="java.lang.String"%>
<%@ attribute name="iefixes" required="false" type="java.lang.Boolean"%>
<%@ attribute name="lffixes" required="false" type="java.lang.Boolean"%>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>

<link rel="stylesheet" href="/css3/${buildStamp}/${name}.css" type="text/css" />

<c:if test="${iefixes}">
<!--[if lt IE 8]>
<link rel="stylesheet" href="/css3/${buildStamp}/${name}-iefixes.css" type="text/css" />
<![endif]-->
</c:if>

<c:if test="${lffixes && browser.linux && browser.gecko}">
<link rel="stylesheet" href="/css3/${buildStamp}/${name}-lffixes.css" type="text/css" />
</c:if>
