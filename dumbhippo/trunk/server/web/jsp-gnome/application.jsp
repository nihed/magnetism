<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<dh:bean id="applications" class="com.dumbhippo.web.pages.ApplicationsPage" scope="request"/>
<dh:bean id="application" class="com.dumbhippo.web.pages.ApplicationPage" scope="page"/>
<jsp:setProperty name="application" property="applicationId" param="id"/>

<c:set var="appView" value="${application.application}"/>

<c:if test="${empty appView || appView.application.deleted}">
	<dht:errorPage>Application not found</dht:errorPage>
</c:if>

<head>
	<title>Application Statistics - <c:out value="${appView.application.name}"/></title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="applications"/>
	<dh:script modules="dh.control,dh.util,dh.event"/>
	<script type="text/javascript">
		dhApplicationId = <dh:jsString value="${appView.application.id}"/>;
		dhApplicationPackageNames = <dh:jsString value="${appView.application.packageNames}"/>;
		dhApplicationDesktopNames = <dh:jsString value="${appView.application.desktopNames}"/>;
		
		function dhApplicationRefresh() {
			var version = dhApplicationApplication.getVersion();
			var run = dhApplicationApplication.getCanRun();
			var install = !run && dhApplicationApplication.getCanInstall();
			if (version != null)
				version = version.replace(/(.*)-.*/, "$1")
			var nolocal = version == null && !run && !install;
			
			document.getElementById("dhApplicationNoLocal").style.display = nolocal ? "block" : "none"; 
			document.getElementById("dhApplicationVersionOuter").style.display = version != null ? "block" : "none"; 
			document.getElementById("dhApplicationRun").style.display = run ? "block" : "none"; 
			document.getElementById("dhApplicationInstall").style.display = install ? "block" : "none"; 
			
			var span = document.getElementById("dhApplicationVersion");
			dh.util.clearNode(span);
			if (version != null)
				span.appendChild(document.createTextNode(version));
		}
		
		function dhApplicationInit() {
			dh.control.createControl();
			dhApplicationApplication = dh.control.control.getOrCreateApplication(dhApplicationId,
																		         dhApplicationPackageNames,
																		         dhApplicationDesktopNames);
	        dhApplicationApplication.onChange = dhApplicationRefresh;
																		
			dhApplicationRefresh();
		}
		
		dh.event.addPageLoadListener(dhApplicationInit);
	</script>	
</head>

<body>
   	<gnome:page>
	    <dht3:applicationsTop/>
	    <hr>
	    <table id="dhApplicationsColumns">
	    <tr>
	    <dht3:applicationsLeft currentCategory="${appView.application.category}" linkifyCurrent="true"/>
	    <td id="dhApplicationsMain">
			<div id="dhApplicationsApplications">
	    		<dht3:application application="${appView}" includeStats="false" linkify="false"/>
				
	    		<div class="dh-application-more">
	    			<div class="dh-applications-application-stats-outer">
	    				<div class="dh-applications-application-stats-heading">Rank &amp; Usage</div>
		    			<dht3:applicationStats application="${appView}"/>
	    			</div>
	    			<div id="dhApplicationNoLocal">
	    				<div class="dh-application-no-local-header">No Package Information</div>
	    				<div>
	    					<a href="http://developer.mugshot.org/wiki/Package_Information">Read More</a>
	    				</div>
	    			</div>
	       			<div id="dhApplicationRun" class="dh-application-action" style="display: none;">
	    				<a href="javascript:dhApplicationApplication.run()">Run <c:out value="${appView.application.name}"/></a>
  					</div>
	   				<div id="dhApplicationInstall" class="dh-application-action" style="display: none;">
						<a href="javascript:dhApplicationApplication.install()">Install Now</a>
	    			</div>
		    		<div class="dh-application-more-details">
		    			<div id="dhApplicationVersionOuter" style="display: none;">
	    					Installed version: <span id="dhApplicationVersion"></span>
  						</div>
 					</div>
	    			<div class="dh-grow-div-around-floats"></div>
	    		</div>
	    		<div class="dh-application-description">
	    			${appView.application.descriptionAsHtml}
	    		</div>
	    		<c:if test="${signin.valid}">
					<a href="/application-edit?id=${appView.application.id}">Edit application database entry</a>
	    		</c:if>
    		</div>
	    </td>
	    <td id="dhApplicationsRight">
			<div class="dh-applications-subheading">Popular applications among <c:out value="${appView.application.name}"/> users:</div>
			<dht3:miniApplicationList apps="${application.relatedApplications}"/>
	    </td>
	    </tr>
	    </table>
	</gnome:page>
</body>

