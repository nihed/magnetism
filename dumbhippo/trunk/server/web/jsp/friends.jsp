<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="contacts" class="com.dumbhippo.web.ContactsPage" scope="request"/>

<%-- If there's no who= param, we want to redirect to the signed-in user's who=, 
     but if who=invalid, we want an error message --%>
<c:if test="${empty param.who && contacts.signin.valid}">
	<c:redirect url="/friends?who=${contacts.signin.userId}"></c:redirect>
</c:if>

<jsp:setProperty name="contacts" property="start" param="start"/>
<jsp:setProperty name="contacts" property="stop" param="stop"/>
<jsp:setProperty name="contacts" property="viewedUserId" param="who"/>

<head>
	<title>View Friends</title>
	<dht:stylesheets href="contacts.css" iehref="contacts-iefixes.css"/>
	<dht:scriptIncludes/>
</head>
<dht:bodyWithAds>
	<dht:mainArea>
		<dht:toolbar/> 

		<c:choose>
		    <c:when test="${contacts.self}"> 
                <dht:largeTitle>Your Friends</dht:largeTitle>
			</c:when>
			<c:otherwise>
			    <dht:largeTitle><c:out value="${contacts.viewedPerson.name}"/>'s Friends</dht:largeTitle>        
			</c:otherwise>   
        </c:choose>     
		
	    <c:choose>		
			<c:when test="${contacts.totalContacts > 0}">		              	    
                <div class="dh-people-grid">
				    <dh:entityList value="${contacts.contacts.list}" showInviteLinks="${contacts.self and (contacts.invitations > 0)}" photos="true" bodyLengthLimit="8" longBodyLengthLimit="30" twoLineBody="true"/>
		        </div>
		        <c:if test="${!empty contacts.viewedUserId}">
		            <c:set var="whoParam" value="&who=${contacts.viewedUserId}" scope="page"/>
		        </c:if>    
				<c:if test="${contacts.start > 1}">
				    <c:choose>
				        <c:when test="${contacts.start > contacts.totalContacts}">
				            <c:set var="prevNumber" value="${contacts.totalContacts}" scope="page"/>
				        </c:when>
				        <c:otherwise>
				            <c:set var="prevNumber" value="${contacts.start-1}" scope="page"/>
				        </c:otherwise>
				    </c:choose>    				            
                    <div class="dh-prev-contacts">
                        <a href="/friends?stop=${contacts.start-1}${whoParam}">Prev <c:out value="${prevNumber}"/></a>
		   		    </div>
                </c:if>
                <div class="dh-viewing-contacts">
                    <c:out value="${contacts.contacts.size}"/> out of <c:out value="${contacts.totalContacts}"/> 
		   		</div>                
				<c:if test="${contacts.hasMoreContacts}">
                    <div class="dh-next-contacts">
                        <a href="/friends?start=${contacts.nextStart}${whoParam}">Next <c:out value="${contacts.totalContacts-contacts.nextStart+1}"/></a>
				    </div>
                </c:if>
		    </c:when>
		    <c:otherwise>
		        <c:choose>
		            <c:when test="${contacts.self}"> 
				        <!-- FIXME: need class definition for this -->				
			            <div class="dh-people-none">You Need Peeps!!</div>
			        </c:when>
			        <c:otherwise>
			            <div class="dh-people-none">They Need Peeps!!</div>
			        </c:otherwise>   
			    </c:choose>     			        
			</c:otherwise>
		</c:choose>
		
	</dht:mainArea>

	<dht:sidebarArea>
	
		<dht:sidebarAreaHeader>
		    <!-- might have these values come from the invites page -->
            <dht:headshot person="${contacts.viewedPerson}" size="192" />
            <dht:sidebarAreaHeaderName value="${contacts.viewedPerson.name}" canModify="false"/>
		</dht:sidebarAreaHeader>

		<dht:sidebarPanes>
		    <c:choose>
		        <c:when test="${contacts.self}"> 
		            <dht:sidebarPane title="Invite Someone Else">
			            <p class="dh-right-box-text">
                            <c:choose>           
			                    <c:when test="${contacts.invitations > 0}">
			                        You can <a class="dh-invites-left" href="/invite">invite</a> ${contacts.invitations} more people to join DumbHippo.
			                    </c:when>
			                    <c:otherwise>
			                        You don't have invitations to send out available to you at the moment.
			                    </c:otherwise>
			                </c:choose>    
			                <br>
			            </p>   
		            </dht:sidebarPane>
		            <dht:sidebarPane title="Friend Tips" last="true">
                        <p class="dh-right-box-text">
		                    Your friends are the people you know and communicate with on
		                    DumbHippo. Sending a message to someone adds them to your
		                    friends list. Your friends can see more information about
		                    you; for example, they can see your other friends.
			            </p>
		            </dht:sidebarPane>
		        </c:when>
		        <c:otherwise>
		            <dht:sidebarPane title="Check Them Out">
                        <p class="dh-right-box-text">
		                    All about <a href="/person?who=${contacts.viewedUserId}"><c:out value="${contacts.viewedPerson.name}"/></a>. 
			            </p>
		            </dht:sidebarPane>
		            <dht:sidebarPane title="Friend Tips" last="true">
                        <p class="dh-right-box-text">
		                    Your friends are the people you know and communicate with on
		                    DumbHippo. Once you are on someone's friends list, you can 
		                    see their other friends.
			            </p>
		            </dht:sidebarPane>
		        </c:otherwise>
		    </c:choose>    
		</dht:sidebarPanes>		
	</dht:sidebarArea>

</dht:bodyWithAds>
</html>
