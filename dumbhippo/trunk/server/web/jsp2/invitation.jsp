<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<head>
	<title>Your Invitations</title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/invitation.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.invitation")
		dh.invitation.initialValues = {
			'dhAddressEntry' : '',
			'dhSubjectEntry' : 'Invitation from ${signin.user.nickname} to join Mugshot',
			'dhMessageEntry' : 	
	'Mugshot makes it easy to instantly share web pages and music playlists ' +
	'with friends and family -- or the world!'
		}
		dh.invitation.resendValues = {
			'dhAddressEntry' : '',
			'dhSubjectEntry' : 'Invitation from ${signin.user.nickname} to join Mugshot',
			'dhMessageEntry' : 'Just a reminder'
		}
		dojo.event.connect(dojo, "loaded", dj_global, "dhInvitationInit");
	</script>
</head>
<dht:twoColumnPage>
	<dht:sidebarPerson who="${signin.user.id}"/>
	<dht:contentColumn>
		<dht:zoneBoxInvitation back='true'>
			<div></div> <!-- IE bug workaround, display:none as first child causes problems -->			
			<dht:messageArea/>
			<dht:suggestGroupsDialog/>	
			<c:choose>
			    <c:when test="${signin.user.account.invitations > 0}">
				    <dht:zoneBoxTitle>INVITE A FRIEND</dht:zoneBoxTitle>
                    <c:set var="disabled" value="false"/>
			    </c:when>
			    <c:otherwise>
				    <dht:zoneBoxTitle>NO INVITATIONS REMAINING</dht:zoneBoxTitle>							
                    <c:set var="disabled" value="true"/>
                    <c:set var="disabledAttribute" value="disabled"/>
			    </c:otherwise>
			</c:choose>
			
			<dht:formTable>
                <dht:formTableRow label="Friend's Email Address">
                    <dht:textInput id="dhAddressEntry" disabled="${disabled}"/>
                </dht:formTableRow>
                <dht:formTableRow label="Subject">
                    <dht:textInput id="dhSubjectEntry" disabled="${disabled}"/>
                </dht:formTableRow>
                <dht:formTableRow label="Message">
                    <dht:textInput id="dhMessageEntry" multiline="true" disabled="${disabled}"/>
                </dht:formTableRow>
                <dht:formTableRow label="Invite to Groups">
                    <div id="dhSuggestedGroupsWithInvitationDiv">
                        <span id="dhSuggestedGroupsWithInvitation"></span> 
                        <dht:actionLink id="dhSuggestGroupsWithInvitation" disabled="${disabled}" oneLine="true" title="Invite to groups" href="javascript:dh.invitation.showSuggestGroupsPopup('dhSuggestGroupsWithInvitation', 'a new invitee', '')">
                            <c:out value="Choose Groups"/>
			            </dht:actionLink>
			        </div>    
                </dht:formTableRow>                
                <tr>
                    <td></td>
                    <td class="dh-control-cell"><input ${disabledAttribute} id="dhInvitationSendButton" type="button" value="Send" onclick="dh.invitation.send()"/></td>
                </tr>
            </dht:formTable>
            
			<c:if test="${person.outstandingInvitations.size > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>PENDING INVITATIONS</dht:zoneBoxTitle>
				<table>
				    <c:set var="count" value="1"/>  
					<c:forEach items="${person.outstandingInvitations.list}" var="invitation">
					    <c:set var="suggestGroupsText" value="Invite to Groups"/>
					    <c:if test="${invitation.suggestedGroupsCount > 0}">
					        <c:set var="suggestGroupsText" value="Edit Groups (${invitation.suggestedGroupsCount})"/>
					    </c:if>     
						<tr class="dh-invitation">
							<td class="dh-address">
								<c:out value="${invitation.invite.humanReadableInvitee}"/>
							</td>
							<td class="dh-sent">
								<c:out value="${invitation.invite.humanReadableAge}"/>
							</td>
							<td>
								<c:set var="addressJs" scope="page"><dh:jsString value="${invitation.invite.humanReadableInvitee}"/></c:set>
								<dht:actionLink oneLine="true" title="Send the invitation again" href="javascript:dh.invitation.resend(${addressJs})">Resend</dht:actionLink>
								|
			                    <dht:actionLink id="dhSuggestGroups${count}" oneLine="true" title="Invite to groups" href="javascript:dh.invitation.showSuggestGroupsPopup('dhSuggestGroups${count}', '${invitation.invite.humanReadableInvitee}', '${invitation.commaSeparatedSuggestedGroupIds}')">
                                    <c:out value="${suggestGroupsText}"/>
			                    </dht:actionLink> 		 		
							</td>
						</tr>
						<c:set var="count" value="${count+1}"/>
					</c:forEach>
				</table>
			</c:if>
		</dht:zoneBoxInvitation>
	</dht:contentColumn>
</dht:twoColumnPage>
<form id="dhReloadForm" action="/invitation" method="post">
	<input id="dhReloadMessage" name="message" type="hidden"/>
</form>
</html>
