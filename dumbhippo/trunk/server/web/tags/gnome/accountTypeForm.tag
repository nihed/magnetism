<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<%@ attribute name="accountTypeView" required="false" type="com.dumbhippo.server.views.OnlineAccountTypeView" %>
<%@ attribute name="allowEdit" required="true" type="java.lang.Boolean" %>
<%@ attribute name="allowRemoval" required="false" type="java.lang.Boolean" %>

<c:if test="${(accountTypeView == null) && !allowEdit}">
	<%-- the user should never see this error message, but we should check to make sure this tag is used correctly --%>
	<dht:errorPage>Have to either submit an account type to view or allow editing for adding a new account type.</dht:errorPage>
</c:if>

<dh:script modules="dh.actions"/>	

<script type="text/javascript">
	    function dhLowerCaseAccountTypeName() {
            name = document.getElementById("dhAccountTypeName").value;
            document.getElementById("dhAccountTypeName").value=name.toLowerCase();
        }	
</script>
        
<c:set var="name" value=""/>
<c:set var="fullName" value=""/>
<c:set var="siteName" value=""/>
<c:set var="site" value=""/>
<c:set var="userInfoType" value=""/>    
<c:set var="publicCheckedAttr" value=""/>
<c:set var="privateCheckedAttr" value="checked"/>
<c:set var="disabledAttr" value=""/>
<c:if test="${accountTypeView != null}">
    <c:set var="name" value="${accountTypeView.onlineAccountType.name}"/>
    <c:set var="fullName" value="${accountTypeView.onlineAccountType.fullName}"/>
    <c:set var="siteName" value="${accountTypeView.onlineAccountType.siteName}"/>
    <c:set var="site" value="${accountTypeView.onlineAccountType.site}"/>
    <c:set var="userInfoType" value="${accountTypeView.onlineAccountType.userInfoType}"/>  
    <c:if test="${accountTypeView.onlineAccountType.supported}">
        <c:set var="publicCheckedAttr" value="checked"/>
        <c:set var="privateCheckedAttr" value=""/>
    </c:if>      
</c:if>

<c:if test="${!allowEdit}">
    <c:set var="disabledAttr" value="disabled"/> 
</c:if>

<div>
    <h3>Account Type Information</h3>
	<table class="dh-application-edit">
	    <%-- we never allow changing an existing account type name --%>
        <dht3:applicationEditRow id="dhAccountTypeName" disabled="${accountTypeView != null}" name="name" label="Name" value="${name}" onkeyup="dhLowerCaseAccountTypeName()">
		    <jsp:attribute name="help">
		        A short name uniquely identifying the account type. Can only contain lower-case letters and underscores. (e.g. twitter, remember_the_milk, google_reader_rss)
		    </jsp:attribute>
		</dht3:applicationEditRow>
		<dht3:applicationEditRow id="dhAccountTypeFullName" disabled="${!allowEdit}" name="fullName" label="Full Name" value="${fullName}">
		    <jsp:attribute name="help">
		        A full name for the account type. (e.g. Twitter, Remember the Milk, Netflix RSS Feed)
		    </jsp:attribute>
		</dht3:applicationEditRow>
		<dht3:applicationEditRow id="dhAccountTypeSiteName" disabled="${!allowEdit}" name="siteName" label="Site Name" value="${siteName}">
		    <jsp:attribute name="help">
		    	The name of the web site where the user can get this account type. (e.g. Twitter, Remember the Milk)
		    </jsp:attribute>
		</dht3:applicationEditRow>
		<dht3:applicationEditRow id="dhAccountTypeSite" disabled="${!allowEdit}" name="site" label="Site" value="${site}">
		    <jsp:attribute name="help">
	            The url of the web site where the user can get this account type. (e.g. twitter.com, rememberthemilk.com)
		    </jsp:attribute>
		</dht3:applicationEditRow>
		<dht3:applicationEditRow id="dhUserInfoType" disabled="${!allowEdit}" name="userInfoType" label="User Info Type" value="${userInfoType}">
		    <jsp:attribute name="help">
			    What is the type of user information being requested. (e.g. Twitter username, e-mail used for Flickr, Rhapsody "Recently Played Tracks" RSS feed URL)
		    </jsp:attribute>
		</dht3:applicationEditRow>	    	
		<tr>
	        <td class="dh-application-edit-label">
	    	    Public:
	    	</td>
	        <td>
	    	    <input type="radio" ${disabledAttr} name="dhAccountTypeStatus" id="dhAccountTypePublic" ${publicCheckedAttr}"> <label for="dhAccountTypePublic">Yes</label>
			    <input type="radio" ${disabledAttr} name="dhAccountTypeStatus" id="dhAccountTypePrivate"  ${privateCheckedAttr}"> <label for="dhAccountTypePrivate">No</label>		
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
		    <td>
		        <c:choose>
		            <c:when test="${accountTypeView == null}">
		                <input type="button" value="Save" onclick="dh.actions.createAccountType()"></input>
		            </c:when>
		            <c:when test="${allowEdit}">
		                <input type="button" value="Save Changes" onclick="dh.actions.updateAccountType()"></input>
		            </c:when>
		        </c:choose>
		        <c:if test="${allowRemoval}">
		            <input type="button" value="Remove Account Type" onclick="dh.actions.removeAccountType()"></input>
		        </c:if>
		    </td>        
		</tr>
    </table>
</div>