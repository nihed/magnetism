<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<jsp:setProperty name="applications" property="categoryName" param="category"/>
<jsp:setProperty name="applications" property="search" param="q"/>

<head>
	<title>Application Statistics</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>	
</head>

<dht3:page currentPageLink="application-search">
   	<dht3:shinyBox color="grey">
	    <dht3:applicationsTop/>
	    <hr>
	    <table id="dhApplicationsColumns">
	    <tr>
	    <dht3:applicationsLeft currentCategory="${applications.category}"/>
	    <td id="dhApplicationsMain">
	    	<div class="dh-applications-heading">
		    	<c:choose>
		    		<c:when test="${empty applications.search}">
				    	Popularity of 
				    		<c:choose>
				    			<c:when test="${!empty applications.category}">
				    				<c:out value="${applications.category.displayName}"/>
				    			</c:when>
				    			<c:otherwise>
				    				All
				    			</c:otherwise>
				    		</c:choose>
				    	Applications
		    		</c:when>
		    		<c:otherwise>
		    			Results for <b><c:out value="${applications.search}"/></b>
		    		</c:otherwise>
		    	</c:choose>
    		</div>
	    	<c:choose>
	    		<c:when test="${applications.applications.resultCount == 0}">
	    			<div class="dh-applications-no-applications">
				    	<c:choose>
		    				<c:when test="${empty applications.search}">
			    				There are no popular applications in this category.
				    		</c:when>
				    		<c:otherwise>
			    				There are no matching applications.
		    				</c:otherwise>
				    	</c:choose>
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
				    		<dht3:application application="${application}"/>
				    		<c:if test="${!empty applications.search}">
								<div class="dh-application-description dh-application-description-in-list">
									${application.application.descriptionAsHtml}
								</div>
							</c:if>
					    </c:forEach>
					    <div class="dh-applications-more">
						    <dht:expandablePager pageable="${applications.applications}"/>
					    </div>
				    </div>
			    </c:otherwise>
		    </c:choose>
	    </td>
	    </tr>
	    </table>
	</dht3:shinyBox>
</dht3:page>
