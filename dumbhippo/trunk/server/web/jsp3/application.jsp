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
			var separator = version != null && (install || run);
			if (version != null)
				version = version.replace(/(.*)-.*/, "$1")
			
			document.getElementById("dhApplicationVersionOuter").style.display = version != null ? "inline" : "none"; 
			document.getElementById("dhApplicationSeparator").style.display = separator ? "inline" : "none"; 
			document.getElementById("dhApplicationRun").style.display = run ? "inline" : "none"; 
			document.getElementById("dhApplicationInstall").style.display = install ? "inline" : "none"; 
			
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

<dht3:page currentPageLink="application">
   	<dht3:shinyBox color="grey">
	    <dht3:applicationsTop/>
	    <hr>
	    <table id="dhApplicationsColumns">
	    <tr>
	    <dht3:applicationsLeft/>
	    <td id="dhApplicationsRight">
			<div id="dhApplicationsApplications">
		    	<div class="dh-applications-application">
		    		<div class="dh-applications-application-stats-outer">
	    				<div class="dh-applications-application-stats">
		    				<div class="dh-applications-rank"><c:out value="${appView.application.rank}"/></div>
			    			<div class="dh-applications-usage"><c:out value="${dh:format1('%,d', appView.application.usageCount)}"/></div>
			    		</div>
	    			</div>
		    		<div class="dh-applications-application-icon">
						<dh:png src="${appView.icon.url}" 
							style="width: ${appView.icon.displayWidth}; height: ${appView.icon.displayHeight}; overflow: hidden;"/>
		    		</div>
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
		    			<div class="dh-applications-application-local">
			    			<span id="dhApplicationVersionOuter" style="display: none;">
			    				Currently installed: <span id="dhApplicationVersion"></span>
		   					</span>
				    		<span id="dhApplicationSeparator" style="display: none;">&nbsp;|&nbsp;</span>
		    				<span id="dhApplicationInstall" style="display: none;">
	   							<a href="javascript:dhApplicationApplication.install()">Install</a>
			    			</span>
			    			<span id="dhApplicationRun" style="display: none;">
			    				<a href="javascript:dhApplicationApplication.run()">Run</a>
		   					</span>
	  					</div>
		    		</div>
		   		</div>
    		</div>
			<div class="dh-applications-subheading">Popular applications among <c:out value="${appView.application.name}"/> users:</div>
			<dht3:miniApplicationList apps="${application.relatedApplications}"/>
	    </td>
	    </tr>
	    </table>
	</dht3:shinyBox>
</dht3:page>
