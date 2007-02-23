<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<dh:bean id="application" class="com.dumbhippo.web.pages.ApplicationPage" scope="page"/>
<jsp:setProperty name="application" property="applicationId" param="id"/>

<c:set var="appView" value="${application.application}"/>

<c:if test="${empty appView}">
	<dht:errorPage>Application not found</dht:errorPage>
</c:if>

<head>
	<title>Application Statistics - <c:out value="${appView.application.name}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
</head>

<dht3:page currentPageLink="applications">
   	<dht3:shinyBox color="grey">
	    <div class="dh-page-shinybox-title-large">Open Source Application Statistics</div>
	    <hr>
	    <dht3:applicationCategories currentCategory="${appView.application.category}" linkifyCurrent="true"/>
	    <div id="dhApplicationsRight">
    		<div class="dh-applications-application-icon">
				<dh:png src="${appView.icon.url}" 
					style="width: ${appView.icon.displayWidth}; height: ${appView.icon.displayHeight}; overflow: hidden;"/>
    		</div>
			<div class="dhApplicationsApplications">
	    		<div class="dh-applications-application-details">
	    			<div class="dh-applications-application-name">
	    				<c:out value="${appView.application.name}"/>
		    		</div>
	    			<div class="dh-applications-application-category">
		    			<a href="/applications?category=${appView.application.category.name}">
							<c:out value="${appView.application.category.displayName}"/>
						</a>
		    		</div>
	    			<div class="dh-applications-application-description">
	    				<c:out value="${appView.application.description}"/>
		    		</div>
	    		</div>
    		</div>
			<div class="dh-applications-subheading">Popular applications among <c:out value="${appView.application.name}"/> users:</div>
			<div id="dhApplicationsRelated">
			    <c:forEach items="${application.relatedApplications.results}" var="app">
		    		<div class="dh-applications-application-separator"></div>
			    	<div class="dh-applications-application">
			    		<div class="dh-applications-application-stats-outer">
				    		<div class="dh-applications-application-stats">
				    			<div class="dh-applications-rank"><c:out value="${app.rank}"/></div>
				    		</div>
			    		</div>
			    		<div class="dh-applications-application-icon">
							<dh:png src="${app.icon.url}" 
								style="width: ${app.icon.displayWidth}; height: ${app.icon.displayHeight}; overflow: hidden;"/>
			    		</div>
			    		<div class="dh-applications-application-details">
			    			<div class="dh-applications-application-name">
				    			<a href="/application?id=${app.application.id}">
				    				<c:out value="${app.application.name}"/>
			    				</a>
				    		</div>
			    			<div class="dh-applications-application-category">
				    			<a href="/applications?category=${app.application.category.name}">
		    						<c:out value="${app.application.category.displayName}"/>
		    					</a>
				    		</div>
			    		</div>
			    		<div class="dh-applications-application-separator"></div>
	    			</div>
			    </c:forEach>
			    <div class="dh-applications-more">
				    <dht:expandablePager pageable="${application.relatedApplications}"/>
			    </div>
		    </div>
	    </div>
	</dht3:shinyBox>
</dht3:page>
