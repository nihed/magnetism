<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="outstandingInvitations" required="true" type="com.dumbhippo.web.ListBean"%>
<%@ attribute name="invitesPage" required="true" type="java.lang.Boolean"%>
<c:choose>
    <c:when test="${outstandingInvitations.size > 0}">
        <c:choose>
            <c:when test="${invitesPage}">
                <c:choose>
                    <c:when test="${outstandingInvitations.size > 1}">
                        <h2 class="dh-title">Your Outstanding Invites</h2>
                    </c:when>
                    <c:otherwise> 
                        <h2 class="dh-title">Your Outstanding Invite</h2>
                    </c:otherwise>
                </c:choose>        
                <!-- <div id="dhSharesArea">  -->
            </c:when>
            <c:otherwise>
                <p class="dh-right-box-text">
                You can manage your invites <a class="manage-invites" href="/invites">here</a>.
                </p>
            </c:otherwise>
        </c:choose>
        <c:forEach items="${outstandingInvitations.list}" var="outstandingInvitation">
            <c:if test="${invitesPage}"> 
                <div class="dh-share-shadow">
                <div class="dh-share">
            </c:if>		
            <div class="dh-invitee">
            <c:out value="${outstandingInvitation.invite.humanReadableInvitee}"/>
            </div>
            <div class="dh-invite-age">
            <c:out value="${outstandingInvitation.inviterData.humanReadableAge}"/>
            </div>
            <br>
            <c:if test="${invitesPage}"> 
                <div class="dh-invite-subject">
                <c:out value="${outstandingInvitation.inviterData.invitationSubject}"/>
                </div>            
                <div class="dh-invite-message">
                <c:out value="${outstandingInvitation.inviterData.invitationMessage}"/>
                </div>  
                <!-- let's display the links -->
                <br>
                <div class="dh-invite-actions">
                <a href="/invite?email=${outstandingInvitation.invite.invitee.encodedEmail}">Resend</a>&nbsp;
                <a href="/invites?invitationToDelete=${outstandingInvitation.invite.authKey}">Remove</a> 
                </div>           
                <br>
                </div> <!-- dh-share-shadow -->
                </div> <!-- dh-share -->
            </c:if>		  		
        </c:forEach>	
         <!--- <c:if test="${invitesPage}"> -->
         <!---   </div> <!-- dhSharesArea -->
         <!-- </c:if>  -->      
    </c:when>
    <c:otherwise>
        <c:choose> 
            <c:when test="${invitesPage}">
                <h2 class="dh-title">You Have No Outstanding Invites</h2>
            </c:when>
            <c:otherwise>
                <div class="dh-outstanding-invites-none">You Have No Outstanding Invites</div>
            </c:otherwise> 
        </c:choose>       
    </c:otherwise>
</c:choose>