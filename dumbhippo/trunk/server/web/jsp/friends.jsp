<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="contacts" class="com.dumbhippo.web.ContactsPage" scope="request"/>
<jsp:setProperty name="contacts" property="start" param="start"/>
<jsp:setProperty name="contacts" property="stop" param="stop"/>
<jsp:setProperty name="contacts" property="viewedPersonId" param="who"/>

<head>
	<title>View Friends</title>
	<dht:stylesheets href="contacts.css" iehref="contacts-iefixes.css"/>
	<dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>
		<dht:toolbar/> 

		<c:choose>
		    <c:when test="${contacts.self}"> 
                <h2 class="dh-title">Your Friends</h2>
			</c:when>
			<c:otherwise>
			    <h2 class="dh-title"> <c:out value="${contacts.person.name}"/>'s Friends</h2>        
			</c:otherwise>   
        </c:choose>     
		
	    <c:choose>		
			<c:when test="${contacts.totalContacts > 0}">		              	    
                <div class="dh-people-grid">
				    <dh:entityList value="${contacts.contacts.list}" showInviteLinks="${contacts.self and (contacts.invitations > 0)}" photos="true" bodyLengthLimit="8" longBodyLengthLimit="30" twoLineBody="true"/>
		        </div>
		        <c:if test="${!empty contacts.viewedPersonId}">
		            <c:set var="whoParam" value="&who=${contacts.viewedPersonId}" scope="page"/>
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
		
	</div>

	<div id="dhPersonalArea">
	
		<div id="dhPhotoNameArea">
		    <!-- might have these values come from the invites page -->
            <dht:headshot person="${contacts.person}" size="192" />
            <div id="dhName"><c:out value="${contacts.person.name}"/></div>
		</div>

		<div class="dh-right-box-area">
		    <c:choose>
		        <c:when test="${contacts.self}"> 
		            <div class="dh-right-box">
			            <h5 class="dh-title">Invite Someone Else</h5>
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
		            </div>
		            <div class="dh-right-box dh-right-box-last">
			            <h5 class="dh-title">Friend Tips</h5>
                        <p class="dh-right-box-text">
		                    Your friends are the people you know and communicate with on
		                    DumbHippo. Sending a message to someone adds them to your
		                    friends list. Your friends can see more information about
		                    you; for example, they can see your other friends.
			            </p>
		            </div>
		        </c:when>
		        <c:otherwise>
		            <div class="dh-right-box">
			            <h5 class="dh-title">Check Them Out</h5>
                        <p class="dh-right-box-text">
		                    All about <a href="/person?who=${contacts.viewedPersonId}"><c:out value="${contacts.person.name}"/></a>. 
			            </p>
		            </div>
		            <div class="dh-right-box dh-right-box-last">
			            <h5 class="dh-title">Friend Tips</h5>
                        <p class="dh-right-box-text">
		                    Your friends are the people you know and communicate with on
		                    DumbHippo. Once you are on someone's friends list, you can 
		                    see their other friends.
			            </p>
		            </div>
		        </c:otherwise>
		    </c:choose>    
		</div>		
	</div>
	<dht:bottom/>
</div>

<div id="dhOTP">
    <dht:rightColumn/>
</div>

</body>
</html>
