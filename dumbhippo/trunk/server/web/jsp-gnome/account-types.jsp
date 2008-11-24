<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<dh:bean id="accountTypes" class="com.dumbhippo.web.pages.OnlineAccountTypesPage" scope="request"/>

<head>
	<title>Existing Account Types</title>
	<gnome:stylesheet name="site" iefixes="true"/>	
</head>

<body>
	<gnome:page currentPageLink="account-types">
	    <h3>Existing Account Types</h3>
   		<div class="gnome-learn-more-text">
		    <p>		    
		      Click on the account type to view or edit information about it.	
		    </p>
        </div> 
        <hr>
        <c:forEach items="${accountTypes.allOnlineAccountTypes.list}" var="accountType">
		    <div><a href="/account-type?type=${accountType.onlineAccountType.name}"><c:out value="${accountType.onlineAccountType.fullName}"/></a></div>
		</c:forEach>
	</gnome:page>
</body>
</html>