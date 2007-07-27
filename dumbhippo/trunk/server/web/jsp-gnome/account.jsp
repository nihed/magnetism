<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht2" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<c:if test="${!signin.valid}">
	<%-- this is a bad error message but should never happen since we require signin to get here --%>
	<dht2:errorPage>Not signed in</dht2:errorPage>
</c:if>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.PersonPage"/>

<dh:bean id="account" class="com.dumbhippo.web.pages.AccountPage" scope="page"/>

<head>
    <title><c:out value="${person.viewedPerson.name}"/>'s Account - GNOME Online</title>
	<gnome:stylesheet name="site"/>			
	<gnome:stylesheet name="account"/>	
	<gnome:faviconIncludes/>
</head>
<body>
	Account!
</body>
</html>
