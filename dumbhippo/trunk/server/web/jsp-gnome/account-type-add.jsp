<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>
<%@ taglib tagdir="/WEB-INF/tags/gnome" prefix="gnome" %>

<head>
	<title>Add Account Type</title>
	<gnome:stylesheet name="site" iefixes="true"/>	
	<gnome:stylesheet name="account-types"/>	
	<dh:script modules="dh.textinput,dh.util"/>	
	<script type="text/javascript">
	    function dhOnLoad() {
	        siteName = document.getElementById('dhAccountTypeSiteName');
	        userInfoType = document.getElementById('dhUserInfoType');	    
		    siteNameEntry = new dh.textinput.Entry(siteName, "", "");   
		    userInfoTypeEntry = new dh.textinput.Entry(userInfoType, "username", "username");  
	        siteNameEntry.onkeyup = function(event) {
	            userInfoTypeEntry.setValue(siteNameEntry.getValue() + " username"); 
	        }
	    }	    
    </script>	            
</head>

<body onload="dhOnLoad()">
<gnome:page currentPageLink="account-type-add">
		<div class="dh-page-shinybox-title-large">Add Account Type</div>
		<div>
   			This page allows you to create a new online account type.
			<a href="/account-types">View existing types</a>
		</div>
	    <hr>
         <gnome:accountTypeForm allowEdit="true"/> 
	</gnome:page>
</body>