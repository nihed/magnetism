<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:set var="pageName" value="Mugshot Verification for Facebook" scope="page"/>
    
<head>
    <title><c:out value="${pageName}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>			
	<dht:faviconIncludes/>
</head>
<dht3:page currentPageLink="facebook-add">
    <dht3:shinyBox color="grey">
        <div class="dh-page-shinybox-title-large">Thank you for trying out Mugshot application for Facebook!</div>
        <c:choose>
            <c:when test="${!empty param['error_message']}">
                <c:out value="${param['error_message']}"/>
            </c:when> 
            <c:when test="${!signin.valid}">
                <a href="/who-are-you">Log In</a> or <a href="/signup">Sign Up</a>, then try verifying your Mugshot account again on Facebook.   
            </c:when>
            <c:otherwise>
                You are all set! Check your <a href="/account">Mugshot account page</a> to make sure you've added all the services you use. 
            </c:otherwise>
        </c:choose>    
    </dht3:shinyBox>
</dht3:page>		
</html>