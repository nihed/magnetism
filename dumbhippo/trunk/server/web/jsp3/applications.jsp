<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="page"/>
<jsp:setProperty name="applications" property="categoryName" param="category"/>

<head>
	<title>Application Statistics</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
	<dh:script module="dh.server"/>
	<script type="text/javascript">
		function dhEnableApplicationUsage() {
			var busy = document.getElementById("dhApplicationsBusy");
			busy.style.display = "block";
	
		   	dh.server.doPOST("setapplicationusageenabled",
						     { "enabled" : 'true' },
				  	    	 function(type, data, http) {
								 busy.style.display = "none";
								 document.getElementById("dhApplicationsEnable").style.display = "none";
								 document.getElementById("dhApplicationsEnableConfirm").style.display = "inline";
				  	    	 },
				  	    	 function(type, error, http) {
								 busy.style.display = "none";
				  	    	     alert("Couldn't enable application usage sharing.");
				  	    	 });
	   	}
	</script>
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
		<img id="dhApplicationsBusy" src="/images2/${buildstamp}/feedspinner.gif" style="display: none;"/>
	    <div class="dh-page-shinybox-title-large">Open Source Application Statistics</div>
	    <div>
	    	Mugshot and Fedora developers are working on ways to browse and find popular
	    	applications. Here are the current statistics for users sharing
	    	application usage with us. 
	    	<a href="/applications-learnmore">Read the full details</a>
	    	<c:if test="${signin.valid && !signin.user.account.applicationUsageEnabledWithDefault}">
	    		|  <a id="dhApplicationsEnable" href="javascript:dhEnableApplicationUsage()">
	    			Share your own application usage with us
	       			</a>
	    		<span id="dhApplicationsEnableConfirm" style="display: none;">
					Excellent! You can change your <a href="/account">account</a> settings anytime
    			</span>
	    	</c:if>
	    </div>
	    <hr>
	    <div id="dhApplicationsCategories">
	    	<div class="dh-applications-heading">View Category:</div>
    		<table class="dh-applications-categories" cellspacing="0" cellpadding="0">
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
		    					<c:when test="${applications.category == category.category}">
				    				<c:out value="${category.category.displayName}"/>
				    			</c:when>
				    			<c:otherwise>
					    			<a href="/applications?category=${category.category.name}">
					    				<c:out value="${category.category.displayName}"/>
					    			</a>
				    			</c:otherwise>
			   				</c:choose>
		    			</td>
						<td class="dh-applications-category-bar-outer">	    			
		    				<div class="dh-applications-category-bar" style="background-color: ${category.color}; width: ${category.length}px;"/>
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
	    	<c:choose>
	    		<c:when test="${applications.applications.resultCount == 0}">
	    			<div class="dh-applications-no-applications">
	    				There are no popular applications in this category.
	    			</div>
	    		</c:when>
	    		<c:otherwise>
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
				    <div class="dh-applications-more">
					    <dht:expandablePager pageable="${applications.applications}"/>
				    </div>
		    </c:otherwise>
		    </c:choose>
	    </div>
	</dht3:shinyBox>
</dht3:page>
