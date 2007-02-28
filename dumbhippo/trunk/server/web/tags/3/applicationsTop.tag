<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

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