<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="outstandingInvitations" required="true" type="com.dumbhippo.web.ListBean"%>
<%@ attribute name="invitesPage" required="true" type="java.lang.Boolean"%>
<%@ attribute name="start" required="true" type="java.lang.Integer"%>
<%@ attribute name="maxInvitations" required="true" type="java.lang.Integer"%>
<%@ attribute name="totalInvitations" required="true" type="java.lang.Integer"%>


<c:choose>
    <c:when test="${totalInvitations > 0}">
        <c:choose>
            <c:when test="${invitesPage}">
                <!-- it is possible for the total invitations number to be greater than 0, while -->
                <!-- the number of outstanding invitations to be displayed on a given page is 0 -->
                <!-- in which case we just want to offer a link to the Newer invitations -->
                <!-- particularly, this case arises when there was a single invitation on a page -->
                <!-- that got deleted -->
                <c:if test="${outstandingInvitations.size > 1}">
                    <h2 class="dh-title">Your Outstanding Invites</h2>
                </c:if>
                <c:if test="${outstandingInvitations.size == 1}"> 
                    <h2 class="dh-title">Your Outstanding Invite</h2>
                </c:if>       
                <c:if test="${start > 0}">
                    <div class="dh-newer-invitations">
                        <a href="/invites?start=${start-maxInvitations}">Newer invitations</a>
                    </div>
				</c:if>	 
                <!-- <div id="dhSharesArea">  -->
            </c:when>
            <c:otherwise>
                <p class="dh-right-box-text">
                    You can manage your invites <a class="manage-invites" href="/invites">here</a>.
                </p>
            </c:otherwise>
        </c:choose>
        <c:forEach items="${outstandingInvitations.list}" var="outstandingInvitation" varStatus="status">
            <c:choose>
                <c:when test="${status.count == (maxInvitations+1)}">
			        <!-- extra result is a marker that we have more coming, but don't display it -->
                    <c:choose>
                        <c:when test="${invitesPage}">
                            <div class="dh-later-invitations">
                                <a href="/invites?start=${start+maxInvitations}">Later invitations</a></h2>
						    </div>
                        </c:when>
                        <c:otherwise>
                            <p class="dh-right-box-text">
                                You have <c:out value="${totalInvitations-maxInvitations}"/> <a class="manage-invites" href="/invites">more</a>
                                <c:choose> 
                                    <c:when test="${totalInvitations-maxInvitations > 1}">
                                        invites.
                                    </c:when>
                                    <c:otherwise>
                                        invite.
                                    </c:otherwise>    
                                </c:choose> 
                            </p>
                        </c:otherwise>
                    </c:choose>    
                </c:when>
                <c:otherwise>        		     
                    <c:if test="${invitesPage}"> 
                        <div class="dh-share-shadow">
                        <div class="dh-share">
                    </c:if>		
                    <div class="dh-invitee" title="${outstandingInvitation.invite.humanReadableInvitee}">
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
                            <a href="/invites?start=${start}&invitationToDelete=${outstandingInvitation.invite.authKey}">Remove</a> 
                        </div>           
                        <br>
                        </div> <!-- dh-share -->
                        </div> <!-- dh-share-shadow -->
                    </c:if>		
                </c:otherwise>
            </c:choose>                  		
        </c:forEach>	     
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