<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${empty person}">
	<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.StackedPersonPage"/>
	<jsp:setProperty name="person" property="needExternalAccounts" value="true"/>
</c:if>
