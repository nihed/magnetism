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
	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Applications</div>
	    <c:forEach items="${applications.popularApplications}" var="category">
	    	<div>
		    	<div>
		    		<c:out value="${category.category.name}"/>
		    	</div>
			    <c:forEach items="${category.applications}" var="application">
			    	<div>
						<dh:png src="${application.icon.url}" 
							style="width: ${application.icon.displayWidth}; height: ${application.icon.displayHeight}; overflow: hidden;"/>
			    		<c:out value="${application.application.name}"/>:&nbsp
			    		<c:out value="${application.usageCount}"/>
		    		</div>
			    </c:forEach>
		    </div>
	    </c:forEach>
	</dht3:shinyBox>
</dht3:page>
