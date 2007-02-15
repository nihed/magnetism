<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="page"/>
<jsp:setProperty name="applications" property="categoryName" param="category"/>

<head>
	<title>Applications</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Open Source Application Statistics</div>
	    <div>
	    	Mugshot and Fedora developers are working on ways to browse and find popular
	    	applications. Hear are the current statistics for users sharing anonymous
	    	application usage with us. 
	    </div>
	    <hr>
	    <div id="dhApplicationsCategories">
	    	<div class="dh-applications-heading">View Category:</div>
    		<table cellspacing="0" cellpadding="0">
	    		<tr class="dh-applications-category">
	    			<td>
	    				<c:choose>
	    					<c:when test="${empty applications.category}">
			    				All
			    			</c:when>
			    			<c:otherwise>
				    			<a href="/applications">All</a>
			    			</c:otherwise>
		   				</c:choose>

	    			</td>
    			</tr>
		    	<c:forEach items="${applications.categories}" var="category">
		    		<tr class="dh-applications-category">
		    			<td class="dh-applications-category-name">
		    				<c:choose>
		    					<c:when test="${applications.category == category}">
				    				<c:out value="${category.displayName}"/>
				    			</c:when>
				    			<c:otherwise>
					    			<a href="/applications?category=${category.name}">
					    				<c:out value="${category.displayName}"/>
					    			</a>
				    			</c:otherwise>
			   				</c:choose>
		    			</td>
						<td class="dh-applications-category-bar-outer">	    			
		    				<div class="dh-applications-category-bar" style="background-color: #a45ac6; width: 60px;"/>
		    			</td>
	    			</tr>
		    	</c:forEach>
	    	</table>
	    </div>
	    <div id="dhApplicationsApplications">
	    	<div class="dh-applications-heading">Popularity of 
	    		<c:choose>
	    			<c:when test="${!empty applications.category}">
	    				<c:out value="${applications.category.displayName}"/>
	    			</c:when>
	    			<c:otherwise>
	    				All
	    			</c:otherwise>
	    		</c:choose>
	    	Applications</div>
	    	<div id="dhApplicationsHeader">
	    		<div id="dhApplicationsStatsHeader">Rank & Usage</div>
	    		<div id="dhApplicationsApplicationsHeader">Applications</div>
	    	</div>
		    <c:forEach items="${applications.popularApplications}" var="application">
	    		<div class="dh-applications-application-separator"></div>
		    	<div class="dh-applications-application">
		    		<div class="dh-applications-application-stats-outer">
			    		<div class="dh-applications-application-stats">
			    			<div class="dh-applications-rank"><c:out value="${application.rank}"/></div>
			    			<div class="dh-applications-usage"><c:out value="${application.usageCount}"/></div>
			    		</div>
		    		</div>
		    		<div class="dh-applications-application-icon">
						<dh:png src="${application.icon.url}" 
							style="width: ${application.icon.displayWidth}; height: ${application.icon.displayHeight}; overflow: hidden;"/>
		    		</div>
		    		<div class="dh-applications-application-details">
		    			<div class="dh-applications-application-name">
		    				<c:out value="${application.application.name}"/>
			    		</div>
		    			<div class="dh-applications-application-category">
			    			<a href="/applications?category=${application.application.category.name}">
	    						<c:out value="${application.application.category.displayName}"/>
	    					</a>
			    		</div>
		    		</div>
		    		<div class="dh-applications-application-separator"></div>
    			</div>
		    </c:forEach>
	    </div>
	</dht3:shinyBox>
</dht3:page>
