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
			<div class="dh-page-shinybox-title-large">
				<span>People I've Invited to Join Mugshot <c:out value="(${person.pageableOutstandingInvitations.totalCount})"/></span>
				<a class="dh-underlined-link dh-page-shinybox-subtitle" href="/invitation">Invite friends! (${person.invitations} invitations left)</a> 
			</div>
	        <c:choose>
    	        <c:when test="${person.pageableOutstandingInvitations.totalCount > 0}">
					<c:forEach items="${person.pageableOutstandingInvitations.results}" var="invite">
						<div class="dh-person-item">
						    <div class="dh-image">
							    <dh:png src="/images2/${buildStamp}/user_pix1/invited.png?size=60" style="width: 60px; height: 60px"/>
						    </div>
						    <div class="dh-person-item-name">
						    	<c:out value="${invite.name}"/>
						    </div>
						</div>    
					</c:forEach>
			        <dht:expandablePager pageable="${person.pageableOutstandingInvitations}" anchor="dhInvites"/>
			    </c:when>
			    <c:otherwise>
		    	    No outstanding invitations.  <c:if test="${person.invitations > 0}"><a href="/invitation">Invite friends!</a></c:if>
			    </c:otherwise> 
			</c:choose>       			
		</dht3:shinyBox>      
	</c:if>		
</dht3:page>

</html>