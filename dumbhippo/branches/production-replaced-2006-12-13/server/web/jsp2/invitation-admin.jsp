<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="invitationAdmin" class="com.dumbhippo.web.pages.InvitationAdminPage" scope="request"/>
<jsp:setProperty name="invitationAdmin" property="countToInvite" param="countToInvite"/>

<c:if test="${!invitationAdmin.valid}">
	<dht:errorPage>Permission Denied</dht:errorPage>
</c:if>

<head>
	<title>Invitation Admin</title>
	<dht:siteStyle/>	
    <dht:faviconIncludes/>
		<dh:script module="dh.invitationadmin"/>
</head>
<dht:body>
    Number of people who want in and were not yet invited: 
    <c:out value="${invitationAdmin.wantsInCount}"/>
	<div id="dhCountToInvite">
        <form action="/invitation-admin" method="get">
            <input type="text" name="countToInvite" size="5" value="${invitationAdmin.countToInvite}"/>
            <input type="submit" value="Preview Wants In"/>            
		</form>
	    Previewing <c:out value="${invitationAdmin.wantsInList.size}"/> wants in:
	    <table>
	        <c:forEach items="${invitationAdmin.wantsInList.list}" var="wantIn">
		        <tr>
                    <td><c:out value="${wantIn.address}"/></td>
                    <td><c:out value="${wantIn.count}"/></td>
                    <td><c:out value="${wantIn.creationDate}"/></td>
			    </tr>
		    </c:forEach>
	    </table>	
	    <br/>
	 
	    Invite to groups:
	    <br/>
	    <c:set var="count" value="1"/>  
	    <c:choose>
	        <c:when test="${invitationAdmin.groups.size < 1}">
	            <c:out value="Mugshot has no groups."/>
	        </c:when>
	        <c:otherwise>        
	            <c:forEach items="${invitationAdmin.groups.list}" var="group">
	                <input id="groupCheckbox${count}" type="checkbox" value="${group.group.id}"/> 
	                <c:out value="${group.group.name}"/>
	                <br/>
                    <c:set var="count" value="${count+1}"/>
                </c:forEach>
            </c:otherwise>
        </c:choose>
        <br/>
        
	    <dht:formTable>
            <dht:formTableRow label="Subject">        
                <input id="dhSubjectEntry" size="30" maxlength="64" value="Your Mugshot Invitation"/>
            </dht:formTableRow>
            <dht:formTableRow label="Message">
                <textarea id="dhMessageEntry" rows="5" cols="36">Hey! Click here to get the Mugshot Music Radar and Web Swarm.</textarea>    
            </dht:formTableRow>
            <tr>
                <td><input type="button" value="Let Them In" onclick="return dh.invitationadmin.invite(${invitationAdmin.wantsInList.size});"/></td>
            </tr>
        </dht:formTable>
        <dht:messageArea/>
	</div>
	<br/>
	<a href="http://developer.mugshot.org/wiki/WantsIn_Invitations" target="_blank">What do I need to know about this page?</a>
</dht:body>
</html>