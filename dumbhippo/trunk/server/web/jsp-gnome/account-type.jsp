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
	<title>View Account Type</title>
	<gnome:stylesheet name="site" iefixes="true"/>	
	<gnome:stylesheet name="account-types"/>	
	<dh:script modules="dh.actions"/>	      
</head>

<gnome:page currentPageLink="account-type">
		<h3>View Account Type</h3>
		<div>
   			<c:choose>
   			    <c:when test="${accountTypes.allowEdit}">
   			        This page allows you to view and edit information about an online account type.   
   			    </c:when>
   			    <c:otherwise>
   			        This page allows you to view information about an online account type. Feel free to use <a href="http://mail.gnome.org/mailman/listinfo/online-desktop-list">online-desktop-list@gnome.org mailing list</a>
                    or irc.gnome.org #online-desktop channel to request a change in the settings for this account type. 
   			    </c:otherwise>
   			</c:choose>    
   			<c:if test="${accountTypes.allowRemoval}">
   			    You can remove this account type because no accounts of this type have been created so far.
   			</c:if>
		</div>
	    <hr>
	    <gnome:accountTypeForm accountTypeView="${accountTypes.onlineAccountType}" allowEdit="${accountTypes.allowEdit}" allowRemoval="${accountTypes.allowRemoval}"/> 
	</gnome:page>
</body>