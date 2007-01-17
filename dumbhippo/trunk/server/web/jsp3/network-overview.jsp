<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.MyFriendsPage"/>

<c:set var="pageName" value="Network" scope="page"/>
<c:set var="possessive" value="${person.viewedPerson.name}'s" scope="page"/>
<c:if test="${person.self}">
	<c:set var="possessive" value="My" scope="page"/>
</c:if>

<head>
	<title><c:out value="${possessive}"/> ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true" lffixes="true"/>
	<dht3:stylesheet name="person"/>	
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="network-alphabetical">
	<c:if test="${person.self}">
		<dht3:accountStatus/>
	</c:if>
	<dht3:pageSubHeader title="${possessive} ${pageName} (${person.userContactCount})">
		<dht3:randomTip isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs selected="network"/>
	</dht3:pageSubHeader>
	<dht3:networkTabs selected="network-alphabetical"/>
		
    <dht3:shinyBox color="grey">
        <div class="dh-page-shinybox-title-large"><span>People in <c:out value="${possessive} ${pageName} (${person.userContactCount})"/></span></div>
        <c:choose>  
            <c:when test="${person.userContactCount > 0}">
          	    <c:forEach items="${person.pageableUserContactsBasics.results}" var="person">
			        <dht3:personItem who="${person}"/>
		        </c:forEach>
		        <div class="dh-grow-div-around-floats"><div></div></div>
		        <dht:expandablePager pageable="${person.pageableUserContactsBasics}" anchor="dhFriends"/>
            </c:when>
            <c:when test="${person.self}">
                Why not check out <a href="/active-people">Active People</a> or search for friends using their e-mail.
			</c:when>
			<c:otherwise>
			    <%-- Contacts can be 0 because there are no contacts or because the viewer can not see --%>
		        <%-- this person's friends. In either case, if this is not a self view, the statement below is valid. --%>
		        You cannot view this person's friends.
			</c:otherwise>
	    </c:choose>            
    </dht3:shinyBox>
    
    <c:if test="${person.self}">
        <dht3:shinyBox color="grey">
        <div class="dh-page-shinybox-title-large">People Following Me <c:out value="(${person.followers.size})"/></div>
        <c:choose>
            <c:when test="${person.followers.size > 0}">
				<c:forEach items="${person.pageableFollowers.results}" var="person">
					<dht3:personItem who="${person}"/>
				</c:forEach>
				<div class="dh-grow-div-around-floats"><div></div></div> 
		        <dht:expandablePager pageable="${person.pageableFollowers}" anchor="dhFollowers"/>
		    </c:when>
		    <c:otherwise>
		        Everyone who gets updates about you is already in your network. 
		    </c:otherwise> 
		</c:choose>       
        </dht3:shinyBox>
        
        <dht3:shinyBox color="grey">
            <dht:messageArea idSuffix="InvList"/>
			<div class="dh-page-shinybox-title-large">
				<span>People I've Invited to Join Mugshot <c:out value="(${person.pageableInvitedContacts.totalCount})"/></span>
				<c:if test="${person.invitations != 1}">
			        <c:set var="plural" value="s"/>
			    </c:if>
				<a class="dh-underlined-link dh-page-shinybox-subtitle" href="/invitation">Invite friends! (${person.invitations} invitation${plural} left)</a> 
			</div>
	        <c:choose>
    	        <c:when test="${person.pageableInvitedContacts.totalCount > 0}">
					<c:forEach items="${person.pageableInvitedContacts.results}" var="invitedContact">
						 <dht3:personItem who="${invitedContact}"/>
					</c:forEach>
					<div class="dh-grow-div-around-floats"><div></div></div>
			        <dht:expandablePager pageable="${person.pageableInvitedContacts}" anchor="dhInvitedContacts"/>
			    </c:when>
			    <c:otherwise>
		    	    You have no outstanding invitations.  <c:if test="${person.invitations > 0}"><a href="/invitation">Invite friends!</a></c:if>
			    </c:otherwise> 
			</c:choose>       			
		</dht3:shinyBox>    
		
		<dht3:shinyBox color="grey">
		    <dht:messageArea idSuffix="ContactsList"/>
			<div class="dh-page-shinybox-title-large">
				<span>People I've Shared Links With <c:out value="(${person.pageableContactsWithoutInvites.totalCount})"/></span>
			</div>
	        <c:choose>
    	        <c:when test="${person.pageableContactsWithoutInvites.totalCount > 0}">
					<c:forEach items="${person.pageableContactsWithoutInvites.results}" var="contact">
						 <dht3:personItem who="${contact}"/>
					</c:forEach>
					<div class="dh-grow-div-around-floats"><div></div></div>
			        <dht:expandablePager pageable="${person.pageableContactsWithoutInvites}" anchor="dhContacts"/>
			    </c:when>
			    <c:otherwise>
		    	    All contacts you've shared links with received invitations! 
			    </c:otherwise> 
			</c:choose>       			
		</dht3:shinyBox>      
	</c:if>		
</dht3:page>

</html>