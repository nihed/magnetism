<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.GroupsPage"/>

<c:set var="pageName" value="Groups" scope="page"/>

<c:set var="followed" value="${param['followed']}" scope="page"/>

<c:if test="${empty followed}">
	<c:set var="followed" value="false"/>
</c:if>
<c:if test="${followed && person.followedGroupsCount == 0 && person.activeGroupsCount > 0}">
	<c:set var="followed" value="false"/>
</c:if>
<c:if test="${!followed && person.followedGroupsCount > 0 && person.activeGroupsCount == 0}">
	<c:set var="followed" value="true"/>
</c:if>

<c:choose>
	<c:when test="${!followed}">
		<c:set var="displayedGroups" value="${person.activeGroups}" scope="page"/>
	</c:when>
	<c:otherwise>
		<c:set var="displayedGroups" value="${person.activeFollowedGroups}" scope="page"/>	
	</c:otherwise>
</c:choose>

<c:set var="possessive" value="${person.viewedPerson.name}'s" scope="page"/>
<c:set var="personIn" value="${person.viewedPerson.name} is in" scope="page"/>
<c:set var="personFollow" value="${person.viewedPerson.name} follows" scope="page"/>
<c:if test="${person.self}">
	<c:set var="possessive" value="My" scope="page"/>
	<c:set var="personIn" value="I'm in" scope="page"/>
	<c:set var="personFollow" value="I follow" scope="page"/>	
</c:if>

<head>
	<title><c:out value="${person.viewedPerson.name}"/>'s ${pageName} - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>
	<dht3:stylesheet name="person"/>
	<dht3:stylesheet name="groups"/>
	<dh:script module="dh.groups"/>
	<dht:faviconIncludes/>
</head>

<dht3:page currentPageLink="groups">	
	<c:if test="${person.self}">
		<dht3:accountStatus/>
	</c:if>
	<c:if test="${person.self && fn:length(person.invitedGroupMugshots) > 0}">
		<div class="dh-groups-invited-header dh-page-title-container">
			You've been invited to join <dht3:plural n="${fn:length(person.invitedGroupMugshots)}" s="group"/>  
			<div class="dh-page-options"><c:if test="${fn:length(person.invitedGroupMugshots) > 2}"><a class="dh-underlined-link" href="/group-invitations">MORE</a></c:if></div>
		</div>
		<c:forEach items="${person.invitedGroupMugshots}" var="group" varStatus="groupStatus" end="1">
			<dht3:groupInvitedStack who="${group.groupView}" blocks="${group.blocks}" showFrom="true" stackOrder="1_${groupStatus.count}"/>
		</c:forEach>		
	</c:if>

	<c:choose>
		<c:when test="${person.activeAndFollowedGroupsCount > 0}">
			<div class="dh-page-title-container">
				<span class="dh-page-title"><c:out value="${possessive}"/> ${pageName} (${person.activeAndFollowedGroupsCount})</span>
				<c:if test="${signin.active}">
					<a class="dh-groups-create-link dh-underlined-link" href="/create-group">Create a Group</a>
				</c:if>
				<div class="dh-page-options-container">
					<div class="dh-page-options">
						<dht3:randomTip isSelf="${person.self}"/><dht3:personRelatedPagesTabs selected="groups"/>
					</div>
				</div>
			</div>
			<div class="dh-page-options-sub-options-area dh-page-options">Show: 
			    <c:choose>
				     <c:when test="${!followed}">
					     Groups <c:out value="${personIn}"/> |
					     <c:choose>
					     	 <c:when test="${person.followedGroupsCount > 0}">
							     <a href="/groups?who=${person.viewedPerson.viewPersonPageId}&followed=true">Groups <c:out value="${personFollow}"/></a>
						     </c:when>
						     <c:otherwise>
							     <a class="dh-groups-disabled-link">Groups <c:out value="${personFollow}"/></a>
						     </c:otherwise>
					     </c:choose>
					 </c:when>
				     <c:otherwise>
				     	<c:choose>
					     	<c:when test="${person.activeGroupsCount > 0}">
								<a href="groups?who=${person.viewedPerson.viewPersonPageId}">Groups <c:out value="${personIn}"/></a>
							</c:when>
						     <c:otherwise>
								<a class="dh-groups-disabled-link">Groups <c:out value="${personIn}"/></a>
						     </c:otherwise>
						</c:choose>
						| Groups <c:out value="${personFollow}"/>
					</c:otherwise>
			    </c:choose>
			</div>
			
			<c:forEach items="${displayedGroups.results}" var="group" varStatus="stackStatus">
			    <dht3:groupStack who="${group.groupView}" stackOrder="2_${stackStatus.count}" blocks="${group.blocks}" showFrom="true"/>
			</c:forEach>
		    <dht:expandablePager pageable="${displayedGroups}"/>
	    </c:when>
	    <c:otherwise>
			<div class="dh-page-title-container">
				<span class="dh-page-title"><c:out value="${possessive}"/> ${pageName}</span>
				<div class="dh-page-options-container">
					<div class="dh-page-options">
						<dht3:randomTip isSelf="${person.self}"/><dht3:personRelatedPagesTabs selected="groups"/>
					</div>
				</div>
			</div>
			<dht3:shinyBox color="orange">
			    <div class="dh-person-stacker-header">			
				<table cellpadding="0" cellspacing="0">
					<tbody>
						<tr valign="top">
						<td>
						<div class="dh-image">
							<dh:png src="/images2/${buildStamp}/user_pix1/nophoto.png?size=60" style="width: 60; height: 60"/>
						</div>
						</td>
						<td>
						<div class="dh-person-header-next-to-image">
							<span class="dh-person-header-name">
							<c:choose>
								<c:when test="${!person.self}">
						    		<c:out value="${person.viewedPerson.name}"/> doesn't have any groups
						    	</c:when>
						    	<c:otherwise>My Groups</c:otherwise>
						    </c:choose>	
						</div>
						</td>
						<td align="right">
							<c:if test="${person.self}">
								<dht3:tip>						
								<div>
									<c:choose>
										<c:when test="${!signin.user.account.hasAcceptedTerms}">
											Here is where you can join and see updates from groups when you activate
											your Mugshot account above.
										</c:when>
										<c:when test="${signin.user.account.disabled || signin.user.account.adminDisabled}">
											You must reenable your account to join and see updates from groups.
										</c:when>
										<c:otherwise>
											Here is where you will see updates from groups that you join or create.
										</c:otherwise>
									</c:choose>
								</div>
								<div class="dh-tip-secondary">
									<a href="/active-groups">Browse active groups</a>
									<c:if test="${signin.user.account.hasAcceptedTerms && !(signin.user.account.disabled || signin.user.account.adminDisabled)}">
										| <a href="/create-group">Create a group</a>
									</c:if>
								</div>
								</dht3:tip>
							</c:if>
						</td>
						</tr>
					</tbody>
				</table>
				</div>
			</dht3:shinyBox>
	    </c:otherwise>
    </c:choose>
</dht3:page>
</html>
