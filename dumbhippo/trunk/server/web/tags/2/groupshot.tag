<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.server.GroupView"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>
<c:if test="${empty size}">
	<c:set var="size" value="48"/>
</c:if>
<a href="/group?who=${group.group.id}" style="text-decoration: none;"><dh:png src="/files/groupshots/${size}/${group.group.id}?v=${group.group.version}" style="width: ${size}; height: ${size};"/></a>


