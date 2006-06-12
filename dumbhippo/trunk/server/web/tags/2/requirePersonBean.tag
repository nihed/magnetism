<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="who" required="true" type="java.lang.String" %>

<c:if test="${empty person}">
	<dh:bean id="person" class="com.dumbhippo.web.pages.PersonPage" scope="request"/>
	<jsp:setProperty name="person" property="viewedUserId" value="${who}"/>
</c:if>
