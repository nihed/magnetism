<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="contacts" class="com.dumbhippo.web.ContactsPage" scope="request"/>
<jsp:setProperty name="contacts" property="start" param="start"/>
<jsp:setProperty name="contacts" property="stop" param="stop"/>
<jsp:setProperty name="contacts" property="viewedPersonId" param="who"/>

<head>
	<title>View Contacts</title>
	<dht:stylesheets href="contacts.css" iehref="contacts-iefixes.css"/>
	<dht:scriptIncludes/>
</head>
<body>
<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>
		<dht:toolbar/> 

	    <c:choose>		
			<c:when test="${contacts.contacts.size > 0}">		              	    
                <div class="dh-people-grid">
				    <dh:entityList value="${contacts.contacts.list}" showInviteLinks="${contacts.self and (contacts.invitations > 0)}" photos="true" bodyLengthLimit="8" longBodyLengthLimit="30" twoLineBody="true"/>
		        </div>
		        <c:if test="${!empty contacts.viewedPersonId}">
		            <c:set var="whoParam" value="&who=${contacts.viewedPersonId}" scope="page"/>
		        </c:if>    
				<c:if test="${contacts.start > 1}">
                    <div class="dh-prev-contacts">
                        <a href="/contacts?stop=${contacts.start-1}${whoParam}">Prev</a>
		   		    </div>
                </c:if>
				<c:if test="${contacts.hasMoreContacts}">
                    <div class="dh-next-contacts">
                        <a href="/contacts?start=${contacts.nextStart}${whoParam}">Next</a>
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
			            <h5 class="dh-title">Contacts Tips</h5>
                        <p class="dh-right-box-text">
		                    Your contacts are the people you know. 
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
			            <h5 class="dh-title">Contacts Tips</h5>
                        <p class="dh-right-box-text">
		                    Their contacts are the people they know.
		                    You would not see their contacts, if they
		                    did not claim to know you. 
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