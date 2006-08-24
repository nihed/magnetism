<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="group" required="true" type="com.dumbhippo.persistence.Group"%>
<%@ attribute name="size" required="false" type="java.lang.String" %>
<c:if test="${empty size}">
	<c:set var="size" value="48"/>
</c:if>
<a href="/group?who=${group.id}" style="text-decoration: none;"><dh:png klass="dh-headshot" src="/files/groupshots/${size}/${group.id}?v=${group.version}"  style="width: ${size}; height: ${size};" id="dhPhoto-${size}"/></a>
