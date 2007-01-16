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

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.PersonPage"/>

<dh:bean id="invitations" class="com.dumbhippo.web.pages.InvitePage" scope="page"/>
<jsp:setProperty name="invitations" property="email" param="invitee"/>

<c:choose>
   <c:when test='${invitations.previousInvitation != null && invitations.previousInvitation.invite.valid}'>
       <c:set var="resend" value='${invitations.email}'/>    
   </c:when>
   <c:otherwise>
       <c:set var="send" value='${invitations.email}'/> 
   </c:otherwise>
</c:choose>   

<c:set var="pageName" value="Invitations" scope="page"/>

<head>
    <title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>	
	<dht3:stylesheet name="person"/>	
	<dht3:stylesheet name="invitation"/>	
	<dht:faviconIncludes/>
	<dh:script modules="dh.invitation,dh.event"/>
	<script type="text/javascript">
		dh.invitation.initialValues = {
			'dhAddressEntry' : '${send}',
			'dhSubjectEntry' : 'Invitation from ${signin.user.nickname} to join Mugshot',
			'dhMessageEntry' : 	
	'Join me at Mugshot, it\'s an easy way to share what we\'re doing online. ' +
	'Get updates from blogs, Facebook, Flickr, MySpace, and other sites all in one place.'		
		}
		dh.invitation.resendValues = {
			'dhAddressEntry' : '${resend}',
			'dhSubjectEntry' : 'Invitation from ${signin.user.nickname} to join Mugshot',
			'dhMessageEntry' : 'Just a reminder'
		}
		dh.event.addPageLoadListener(dhInvitationInit);
	</script>
</head>
<dht3:page currentPageLink="invitation">
	<dht3:accountStatus/>
	<dht3:pageSubHeader title="Invite Friends to Mugshot">
		<dht3:randomTip isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs/> 
	</dht3:pageSubHeader>
    <dht3:shinyBox color="grey">
    <div></div> <!-- IE bug workaround, display:none as first child causes problems -->			
	<dht:messageArea/>
	<dht:suggestGroupsDialog/>	
    <c:if test="${person.invitations != 1}">
	    <c:set var="plural" value="s"/>
	</c:if>
    <div class="dh-section-header">Send an Email Invitation to a Friend <span class="dh-section-explanation">(${signin.user.account.invitations} invitation${plural} left)</span></div>
    <c:set var="disabled" value="false"/>
    <c:if test="${!signin.active || signin.user.account.invitations <= 0}">	
        <c:set var="disabled" value="true"/>
    </c:if>
    <dht:formTable>
        <dht:formTableRow label="Friend's Email Address">
            <dht:textInput id="dhAddressEntry" disabled="${disabled}"/>
        </dht:formTableRow>
        <dht:formTableRow label="Subject" altRow="true">
            <dht:textInput id="dhSubjectEntry" disabled="${disabled}"/>
        </dht:formTableRow>
        <dht:formTableRow label="Message">
            <dht:textInput id="dhMessageEntry" multiline="true" disabled="${disabled}"/>
        </dht:formTableRow>
        <dht:formTableRow label="Invite to Groups" altRow="true">
           <div id="dhSuggestedGroupsWithInvitationDiv">
               <span id="dhSuggestedGroupsWithInvitation"></span> 
               <dht:actionLink id="dhSuggestGroupsWithInvitation" disabled="${disabled}" oneLine="true" title="Invite to groups" href="javascript:dh.invitation.showSuggestGroupsPopup('dhSuggestGroupsWithInvitation', 'a new invitee', '')">
                   <c:out value="Choose Groups"/>
			   </dht:actionLink>
		    </div>    
        </dht:formTableRow>                
    </dht:formTable>
    <c:choose>
        <c:when test="${disabled}">
            <img id="dhInvitationSendButton" src="/images3/${buildStamp}/send_disabled.png"/>
        </c:when>
        <c:otherwise>
            <img id="dhInvitationSendButton" src="/images3/${buildStamp}/send.png" onclick="javascript:dh.invitation.send();"/>        
        </c:otherwise>
    </c:choose>        
    </dht3:shinyBox>    
    
    <dht3:shinyBox color="grey">
        <div class="dh-section-header">Outstanding Invitations</div>
        <c:choose>
            <c:when test="${person.outstandingInvitations.size > 0}">
                <table cellpadding="0" cellspacing="0" width="100%" class="dh-invitations-table">
                <c:forEach items="${person.outstandingInvitations.list}" var="invitee" varStatus="invitationStatus">
                    <c:set var="suggestGroupsText" value="Invite to Groups"/>
					<c:if test="${invitee.invitationView.suggestedGroupsCount > 0}">
					    <c:set var="suggestGroupsText" value="Edit Groups (${invitee.invitationView.suggestedGroupsCount})"/>
					</c:if>
					<c:choose>
					    <c:when test="${invitationStatus.count == 1}">
					        <tr>
					    </c:when>
					    <c:when test="${invitationStatus.count % 2 == 1}">
					        </tr><tr>
					    </c:when>
					</c:choose>     
					<td width="50%">   
					<table cellpadding="0" cellspacing="0" class="dh-person-item-more-info">
	                <tbody>
	                <tr valign="top">
	                <td align="left" width="65px">
                        <div class="dh-image">
	                        <dht:headshot person="${invitee}" size="60"/>
                        </div>   
                    </td>
                    <td align="left">
					    <div class="dh-person-item-name">
                            <c:out value="${invitee.name}"/>	
                        </div>
                        <div class="dh-grow-div-around-floats"></div>	
                        <div class="dh-person-item-controls">
                            <dht3:personActionLinks who="${invitee}" showHomeUrl="true">                        								|
			                    <dht:actionLink id="dhSuggestGroups${invitationStatus.count}" oneLine="true" title="Invite to groups" href="javascript:dh.invitation.showSuggestGroupsPopup('dhSuggestGroups${invitationStatus.count}', '${invitee.name}', '${invitee.invitationView.commaSeparatedSuggestedGroupIds}')">
                                    <c:out value="${suggestGroupsText}"/>
			                    </dht:actionLink> 	
			                </dht3:personActionLinks>     	 
                        </div>  
					    <div class="dh-person-header-stats">
			                <div class="dh-invitation-date">
			                    Invitation sent on <c:out value="${invitee.invitationView.invite.humanReadableDate}"/>.
			                </div>    			                
			            </div>
			        </td>
                    </tr>
                    </tbody>
                    </table>       
                    </td>                    
                </c:forEach>
                </tr>
                </table>
            </c:when>
            <c:otherwise>
            </c:otherwise>
        </c:choose>    
    </dht3:shinyBox>    	     
    <form id="dhReloadForm" action="/invitation" method="post">
	    <input id="dhReloadMessage" name="message" type="hidden"/>
    </form>
</dht3:page>    
</html>
            