<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${!signin.valid}">
	<%-- should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>
<%-- TODO: should be able to remove this once the sidebar with only some members shown is not used anywhere --%>
<jsp:setProperty name="group" property="allMembers" value="true"/>

<c:if test="${empty group.viewedGroup}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<c:if test="${!group.viewedGroup.status.canShare}">
	<dht:errorPage>Only members and followers can invite people to a group</dht:errorPage>
</c:if>

<c:set var="pageName" value="Invitations to ${group.viewedGroup.name}" scope="page"/>


<head>
	<title><c:out value="${pageName}"/> - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>	
	<dht3:stylesheet name="person"/>	
	<dht3:stylesheet name="invitation"/>	
	<dht:faviconIncludes/>
	<dh:script modules="dh.groupinvitation,dh.event"/>
	<script type="text/javascript">
		dh.groupinvitation.groupId = <dh:jsString value="${group.viewedGroupId}"/>
		dh.groupinvitation.inviteeId = <dh:jsString value="${param['invitee']}"/>
		dh.groupinvitation.initialValues = {
			'dhAddressEntry' : '',
			'dhSubjectEntry' : <dh:jsString value="${group.shareSubject}"/>,
			'dhMessageEntry' : <dh:jsString value="${!empty param['body'] ? param['body'] : group.viewedGroup.group.description}"/>
		}
		dh.event.addPageLoadListener(dhGroupInvitationInit);
	</script>
</head>		
<dht3:page currentPageLink="group-invitation">

    <dht3:pageSubHeader title="${group.viewedGroup.name} - ${group.private ? 'Private Group' : 'Public Group'}" privatePage="${group.private}">
		<dht3:groupRelatedPagesTabs group="${group.viewedGroup}"/>
	</dht3:pageSubHeader>
    
    <dht3:shinyBox color="grey">
        <div></div> <!-- IE bug workaround, display:none as first child causes problems -->			
	    <dht:messageArea/>
		<div class="dh-section-header">
			<c:choose>
			    <c:when test="${group.canAddMembers}">				
				    Invite a Friend to This Group
			    </c:when>
			    <c:otherwise>
				    Invite a Friend to Follow This Group
			    </c:otherwise>
			</c:choose>
		</div>
		<c:if test="${signin.user.account.invitations == 0}">
		    <div class="dh-warning-note">
				Since you currently don't have any invitations to Mugshot to give out,
				you can only invite someone to a group if they already have a Mugshot
				account.
			</div>
		</c:if>
		<dht:formTable>
            <dht:formTableRow label="User or Email Address">
                <dht:textInput id="dhAddressEntry"/>
                <img id="dhAddressButton" src="/images2/${buildStamp}/dropdownarrow.gif"/>
            </dht:formTableRow>
		    <dht:formTableRow label="Subject" altRow="true">
				<dht:textInput id="dhSubjectEntry"/>
			</dht:formTableRow>
			<dht:formTableRow label="Message">
				<dht:textInput id="dhMessageEntry" multiline="true"/>
			</dht:formTableRow>
		</dht:formTable>
		<img id="dhInvitationSendButton" class="dh-shinybox-bottom-content" src="/images3/${buildStamp}/send.png" onclick="javascript:dh.groupinvitation.send();"/> 	
    </dht3:shinyBox>   
    
    <c:if test="${group.viewedGroup.status.participant}">
        <dht3:invitedMembers forInvitationPage="true"/>
    </c:if>
    
    <%-- Only public groups can have followers --%>
    <c:if test="${group.public}">
        <dht3:invitedFollowers forInvitationPage="true"/>
    </c:if>
    
    <form id="dhReloadForm" action="/group-invitation?group=${group.viewedGroupId}" method="post">
	    <input id="dhReloadMessage" name="message" type="hidden"/>
	    <input id="dhReloadBody" name="body" type="hidden"/>	
    </form>
      
</dht3:page>    
</html>			
			            
            
            