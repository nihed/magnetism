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
	    <dht3:applicationsTop/>
	    <hr>
	    <div id="dhApplicationsRight">
	    	<form>
		    	<table id="dhApplicationEdit">
		    		<dht3:applicationEditRow id="dhApplicationName" name="name" label="Name" value="${appView.application.name}">
		    			<jsp:attribute name="help">
		    				Name displayed in the user interface.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDescription" name="description" label="Description" value="${appView.application.description}" multiline="true">
		    			<jsp:attribute name="help">
		    				Detailed description of the application (one or two sentences).
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationTitlePatterns" name="titlePatterns" label="Title Patterns" value="${appView.application.titlePatterns}">
		    			<jsp:attribute name="help">
							Regular expressions to match window titles and identify this application (generally should be empty unless multiple applications share the same window class.)
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationDesktopNames" name="desktopNames" label="Desktop Names" value="${appView.application.desktopNames}">
		    			<jsp:attribute name="help">
		    				Names used when finding a desktop file to launch this application.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<dht3:applicationEditRow id="dhApplicationCategories" name="categories" label="Categories" value="${appView.application.rawCategories}">
		    			<jsp:attribute name="help">
		    				Categories that this application belongs to.
		    			</jsp:attribute>
		    		</dht3:applicationEditRow>
		    		<tr>
		    			<td></td>
		    			<td class="dh-application-edit-save"><input type="submit" value="Save"></input></td>
		    		</tr>
		    	</table>
	    	</form>
	    </div>
	</dht3:shinyBox>
</dht3:page>
