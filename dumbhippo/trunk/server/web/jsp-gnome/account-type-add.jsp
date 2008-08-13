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
	<dh:script modules="dh.actions,dh.textinput,dh.util"/>	
	<script type="text/javascript">
	    function dhLowerCaseAccountTypeName() {
            name = document.getElementById("dhAccountTypeName").value;
            document.getElementById("dhAccountTypeName").value=name.toLowerCase();
        }	
        
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
	    <div id="dhMessage">
	    </div>
	    <div>
		    <h3>Account Type Information</h3>
		    <table class="dh-application-edit">
		        <dht3:applicationEditRow id="dhAccountTypeName" name="name" label="Name" onkeyup="dhLowerCaseAccountTypeName()">
		    		<jsp:attribute name="help">
		    			A short name uniquely identifying the account type. Can only contain lower-case letters and underscores. (e.g. twitter, remember_the_milk, google_reader_rss)
		    		</jsp:attribute>
		    	</dht3:applicationEditRow>
		    	<dht3:applicationEditRow id="dhAccountTypeFullName" name="fullName" label="Full Name">
		    		<jsp:attribute name="help">
		    			A full name for the account type. (e.g. Twitter, Remember the Milk, Netflix RSS Feed)
		    		</jsp:attribute>
		    	</dht3:applicationEditRow>
		    	<dht3:applicationEditRow id="dhAccountTypeSiteName" name="siteName" label="Site Name">
		    		<jsp:attribute name="help">
		    			The name of the web site where the user can get this account type. (e.g. Twitter, Remember the Milk)
		    		</jsp:attribute>
		    	</dht3:applicationEditRow>
		    	<dht3:applicationEditRow id="dhAccountTypeSite" name="site" label="Site">
		    		<jsp:attribute name="help">
						The url of the web site where the user can get this account type. (e.g. twitter.com, rememberthemilk.com)
		    		</jsp:attribute>
		    	</dht3:applicationEditRow>
		    	<dht3:applicationEditRow id="dhUserInfoType" name="userInfoType" label="User Info Type">
		    		<jsp:attribute name="help">
						What is the type of user information being requested. (e.g. Twitter username, e-mail used for Flickr, Rhapsody "Recently Played Tracks" RSS feed URL)
		    		</jsp:attribute>
		    	</dht3:applicationEditRow>	    	
		    	<tr>
	    			<td class="dh-application-edit-label">
	    			    Public:
	    			</td>
	    			<td>
	    			    <input type="radio" name="dhAccountTypeStatus" id="dhAccountTypePublic"> <label for="dhAccountTypePublic">Yes</label>
					    <input type="radio" name="dhAccountTypeStatus" id="dhAccountTypePrivate"  checked="true"> <label for="dhAccountTypePrivate">No</label>		
	    			</td>
	    		</tr>
	    		<tr>
		            <td></td>
		            <td class="dh-application-edit-help">
			            Should this account type be listed on online.gnome.org or are desktop features for it still under development.       
		            </td>
	            </tr>		   	    				    		
		    	<tr>		    	
		    		<td></td>
		    		<td><input type="button" value="Save" onclick="dh.actions.createAccountType()"></input></td>
		    	</tr>
		    </table>
	    </div>
	</gnome:page>
</body>