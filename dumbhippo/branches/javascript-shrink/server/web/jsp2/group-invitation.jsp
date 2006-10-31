<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<c:if test="${!signin.valid}">
	<%-- should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>
<jsp:setProperty name="group" property="allMembers" value="true"/>

<c:if test="${empty group.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title>Invitations to <c:out value="${group.name}"/></title>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/invitation.css"/>
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/group.css"/>
	<dht:faviconIncludes/>
	<dht:scriptIncludes>
		<dht:script src="dh.groupinvitation"/>
	</dht:scriptIncludes>
	<dht:embedObject/>
	<script type="text/javascript">
		dh.groupinvitation.groupId = <dh:jsString value="${group.viewedGroupId}"/>
		dh.groupinvitation.initialValues = {
			'dhAddressEntry' : '',
			'dhSubjectEntry' : <dh:jsString value="${group.shareSubject}"/>,
			'dhMessageEntry' : <dh:jsString value="${!empty param['body'] ? param['body'] : group.viewedGroup.group.description}"/>
		}
		dojo.event.connect(dojo, "loaded", dj_global, "dhGroupInvitationInit");
	</script>
</head>
<dht:twoColumnPage>
	<dht:sidebarGroup/>
	<dht:contentColumn>
		<dht:zoneBoxInvitation back='true'>
			<div></div> <%-- IE bug workaround, display:none as first child causes problems --%>
			<div class="dh-message" id="dhMessageDiv" style='display: ${empty param["message"] ? "none" : "block"};'>
				<c:out value='${param["message"]}'/>
			</div>
			<c:choose>
				<c:when test="${group.canAddMembers}">				
					<dht:zoneBoxTitle>INVITE A FRIEND TO THIS GROUP</dht:zoneBoxTitle>
				</c:when>
				<c:otherwise>
					<dht:zoneBoxTitle>INVITE A FRIEND TO FOLLOW THIS GROUP</dht:zoneBoxTitle>
				</c:otherwise>
			</c:choose>
			<c:if test="${signin.user.account.invitations == 0}">
				<div class="dh-warning-note">
					Since you currently don't have any invitations to Mugshot to give out,
					you can only invite someone to a group if they already have a Mugshot
					account.
				</div>
			</c:if>
			<dht:formTable>
				<dht:formTableRow label="Member or email address">
					<dht:textInput id="dhAddressEntry"/>
		            <img id="dhAddressButton" src="/images2/${buildStamp}/dropdownarrow.gif"/>
				</dht:formTableRow>
				<dht:formTableRow label="Subject">
					<dht:textInput id="dhSubjectEntry"/>
				</dht:formTableRow>
				<dht:formTableRow label="Message">
					<dht:textInput id="dhMessageEntry" multiline="true"/>
				</dht:formTableRow>
				<tr>
					<td></td>
					<td class="dh-control-cell"><input type="button" value="Send" onclick="dh.groupinvitation.send()"/></td>
				</tr>
			</dht:formTable>
			<c:if test="${group.invitedMembers.size > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>PENDING INVITATIONS</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.invitedMembers.list}" var="person">
						<dht:personItem who="${person}" invited="true"/>
					</c:forEach>
				</dht:twoColumnList>
			</c:if>
			<c:if test="${group.invitedFollowers.size > 0}">
				<dht:zoneBoxSeparator/>
				<dht:zoneBoxTitle>PENDING INVITATIONS TO FOLLOW</dht:zoneBoxTitle>
				<dht:twoColumnList>
					<c:forEach items="${group.invitedFollowers.list}" var="person">
						<dht:personItem who="${person}" invited="true"/>
					</c:forEach>
				</dht:twoColumnList>
			</c:if>			
		</dht:zoneBoxInvitation>
	</dht:contentColumn>
</dht:twoColumnPage>
<form id="dhReloadForm" action="/group-invitation?group=${group.viewedGroupId}" method="post">
	<input id="dhReloadMessage" name="message" type="hidden"/>
	<input id="dhReloadBody" name="body" type="hidden"/>	
</form>
</html>
