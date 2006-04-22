<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="posts" required="true" type="java.util.List" %>

<c:forEach items="${posts}" var="post">
	<dht:simplePostLink post="${post}"/>
</c:forEach>
