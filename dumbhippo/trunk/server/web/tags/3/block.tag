<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="block" required="true" type="com.dumbhippo.web.WebBlock" %>
<%@ attribute name="cssClass" required="true" type="java.lang.String" %>

<div class="dh-stacker-block ${cssClass}">
	<img src="/images3/${buildStamp}/${block.iconName}"/>
	<c:out value="${block.webTitle}"/>
</div>
