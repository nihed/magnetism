<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="page"/>

<head>
	<title>Applications</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="applications">
    <c:forEach items="${applications.popularApplications}" var="category">
	   	<dht3:shinyBox color="grey">
		    <div class="dh-page-shinybox-title-large"><c:out value="${category.category.name}"/></div>
		    <c:forEach items="${category.applications}" var="application">
		    	<div class="dh-applications-application">
					<dh:png src="${application.icon.url}" 
						style="width: ${application.icon.displayWidth}; height: ${application.icon.displayHeight}; overflow: hidden;"/>
		    		<span class="dh-applications-application-name">
		    			<c:out value="${application.application.name}"/>
		    		</span>
		    		<span class="dh-applications-usage-count">
		    			<c:choose>
			    			<c:when test="${application.usageCount == 1}">
				    			1 use
				    		</c:when>
				    		<c:otherwise>
				    			<c:out value="${application.usageCount}"/> uses
				    		</c:otherwise>
				    	</c:choose>
	    			</span>
	    		</div>
		    </c:forEach>
		</dht3:shinyBox>
    </c:forEach>
</dht3:page>
