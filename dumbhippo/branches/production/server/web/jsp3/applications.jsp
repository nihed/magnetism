<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<jsp:setProperty name="applications" property="categoryName" param="category"/>

<head>
	<title>Application Statistics</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
	    <dht3:applicationsTop/>
	    <hr>
	    <dht3:applicationsLeft/>
	    <div id="dhApplicationsRight">
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
	    	<c:choose>
	    		<c:when test="${applications.applications.resultCount == 0}">
	    			<div class="dh-applications-no-applications">
	    				There are no popular applications in this category.
	    			</div>
	    		</c:when>
	    		<c:otherwise>
			    	<div id="dhApplicationsApplications">
				    	<div id="dhApplicationsHeader">
				    		<div id="dhApplicationsStatsHeader">Rank & Usage</div>
				    		<div id="dhApplicationsApplicationsHeader">Applications</div>
				    	</div>	    		
					    <c:forEach items="${applications.applications.results}" var="application">
				    		<div class="dh-applications-application-separator"></div>
					    	<div class="dh-applications-application">
					    		<div class="dh-applications-application-stats-outer">
						    		<div class="dh-applications-application-stats">
						    			<div class="dh-applications-rank"><c:out value="${application.rank}"/></div>
						    			<div class="dh-applications-usage"><c:out value="${dh:format1('%,d', application.usageCount)}"/></div>
						    		</div>
					    		</div>
					    		<div class="dh-applications-application-icon">
									<dh:png src="${application.icon.url}" 
										style="width: ${application.icon.displayWidth}; height: ${application.icon.displayHeight}; overflow: hidden;"/>
					    		</div>
					    		<div class="dh-applications-application-details">
					    			<div class="dh-applications-application-name">
						    			<a href="/application?id=${application.application.id}">
						    				<c:out value="${application.application.name}"/>
			    						</a>
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
					    <div class="dh-applications-more">
						    <dht:expandablePager pageable="${applications.applications}"/>
					    </div>
				    </div>
			    </c:otherwise>
		    </c:choose>
	    </div>
	</dht3:shinyBox>
</dht3:page>
