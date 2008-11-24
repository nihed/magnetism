<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<dh:bean id="accountTypes" class="com.dumbhippo.web.pages.OnlineAccountTypesPage" scope="request"/>
<jsp:setProperty name="accountTypes" property="accountTypeName" param="type"/>

<c:if test="${!accountTypes.accountTypeNameValid}">
	<dht:errorPage>Account with type name '<c:out value="${param['type']}"/>' does not exist.</dht:errorPage>
</c:if>

<head>
	<title>Add Account Type</title>
	<gnome:stylesheet name="site" iefixes="true"/>	
	<gnome:stylesheet name="account-types"/>	
	<dh:script modules="dh.actions"/>	      
</head>

<gnome:page currentPageLink="account-type">
		<div class="dh-page-shinybox-title-large">View Account Type</div>
		<div>
   			This page allows you to view information about an online account type.
		</div>
	    <hr>
	    <gnome:accountTypeForm accountTypeView="${accountTypes.onlineAccountType}" allowEdit="false"/> 
	</gnome:page>
</body>