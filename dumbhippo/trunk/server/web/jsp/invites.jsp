<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="invites" class="com.dumbhippo.web.InvitesPage" scope="request"/>
<jsp:setProperty name="invites" property="invitationToDelete" param="invitationToDelete"/>
<jsp:setProperty name="invites" property="invitationToRestore" param="invitationToRestore"/>
<jsp:setProperty name="invites" property="start" param="start"/>

<head>
	<title>Manage Invites</title>
	<dht:stylesheets href="invites.css"/>
	<dht:scriptIncludes/>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/> 
		<div id="dhSharesArea">
		    <c:if test="${!empty invites.deletedInvitation}">
		        <h2 class="dh-title">Your Last Deleted Invite</h2>
                <div class="dh-share-shadow">
                    <div class="dh-share">	
                        <div class="dh-invitee" title="${invites.deletedInvitation.invite.humanReadableInvitee}">
                            <c:out value="${invites.deletedInvitation.invite.humanReadableInvitee}"/>
                        </div>
                        <div class="dh-invite-age">
                            <c:out value="${invites.deletedInvitation.inviterData.humanReadableAge}"/>
                        </div>
                        <br>
                        <div class="dh-invite-subject">
                            <c:out value="${invites.deletedInvitation.inviterData.invitationSubject}"/>
                        </div>            
                        <div class="dh-invite-message">
                            <c:out value="${invites.deletedInvitation.inviterData.invitationMessage}"/>
                        </div>                 
                        <br>
                        <div class="dh-invite-actions">
                            <a href="/invites?start=${invites.start}&invitationToRestore=${invites.deletedInvitation.invite.authKey}">Restore</a> 
                        </div>           
                        <br>
                    </div> <!-- dh-share -->
                </div> <!-- dh-share-shadow -->
		    </c:if>
		    <dht:inviteList outstandingInvitations="${invites.outstandingInvitations}" invitesPage="true" start="${invites.start}" maxInvitations="${invites.maxInvitationsShown}" totalInvitations="${invites.totalInvitations}"/>      
        </div> <!-- dhSharesArea -->
	</dht:mainArea>

	<dht:sidebarArea>
	
		<dht:sidebarAreaHeader>
		    <!-- might have these values come from the invites page -->
            <dht:headshot person="${invites.person}" size="192" />
            <dht:sidebarAreaHeaderName value="${invites.person.name}" canModify="false"/>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
		    <dht:sidebarPane title="Invite Someone Else">
			    <p class="dh-right-box-text">
                    <c:choose>           
			            <c:when test="${invites.invitations > 0}">
			                You can <a class="dh-invites-left" href="/invite">invite</a> ${invites.invitations} more people to join DumbHippo.
			            </c:when>
			            <c:otherwise>
			                You don't have invitations to send out available to you at the moment.
			            </c:otherwise>
			        </c:choose>    
			        <br>
			    </p>
		    </dht:sidebarPane>
		    <dht:sidebarPane title="Invite Tips" last="true">
                <p class="dh-right-box-text">
					Every now and then Dumb Hippo opens up to accept more people to try it out, when that happens feel free to invite friends to use it.  You'll see your number of invites available change on this site.
			    </p>
		    </dht:sidebarPane>
		</dht:sidebarPanes>
		
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>
