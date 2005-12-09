<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="person" required="true" type="com.dumbhippo.server.PersonView"%>
<c:if test="${!empty person.user}">
<a href="/viewperson?personId=${person.user.id}" style="text-decoration: none;"><dh:png klass="dh-headshot" src="/files/headshots/${person.user.id}?v=${person.user.version}"/></a>
</c:if>


