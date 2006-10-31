<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.views.PersonView"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>
<c:if test="${empty size}">
	<c:set var="size" value="48"/>
</c:if>
<c:if test="${!empty person.user}">
<a href="/person?who=${person.user.id}" style="text-decoration: none;"><dh:png klass="dh-headshot" src="/files/headshots/${size}/${person.user.id}?v=${person.user.version}" style="width: ${size}; height: ${size};" id="dhPhoto-${size}"/></a>
</c:if>


