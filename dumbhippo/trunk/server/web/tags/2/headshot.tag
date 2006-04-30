<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="person" required="false" type="com.dumbhippo.server.PersonView"%>
<%@ attribute name="user" required="false" type="com.dumbhippo.persistence.User"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>

<c:if test="${empty user && !empty person}">
	<c:set var="user" value="${person.user}" scope="page"/>
</c:if>

<c:if test="${empty size}">
	<c:set var="size" value="48"/>
</c:if>
<c:if test="${!empty user}">
<a href="/person?who=${user.id}" style="text-decoration: none;"><dh:png src="/files/headshots/${size}/${user.id}?v=${user.version}" style="width: ${size}; height: ${size};"/></a>
</c:if>

