<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<%@ taglib tagdir="/WEB-INF/tags/3" prefix="dht3" %>

<c:if test="${!signin.valid}">
	<%-- should never happen since we require signin to get here --%>
	<dht:errorPage>Not signed in</dht:errorPage>
</c:if>

<dht3:requirePersonBean beanClass="com.dumbhippo.web.pages.PersonPage"/>

<c:set var="pageName" value="Create Group" scope="page"/>

<c:set var="disabled" value="false"/>
<c:set var="disabledAttr" value=""/>
<c:if test="${!signin.active}">	
    <c:set var="disabled" value="true"/>
    <c:set var="disabledAttr" value="disabled"/>
</c:if>
    
<head>
    <title><c:out value="${pageName}"/> - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>
	<dht3:stylesheet name="styled-form" iefixes="true"/>		
	<dht:faviconIncludes/>
	<dh:script modules="dh.groupaccount,dh.event"/>	
	<script type="text/javascript">
		dh.event.addPageLoadListener(dhCreateGroupInit);
	</script>
</head>

<dht3:page currentPageLink="create-group">
	<dht3:accountStatus/>
	<dht3:pageSubHeader title="${pageName}">
		<dht3:randomTip isSelf="${person.self}"/>
		<dht3:personRelatedPagesTabs/> 
	</dht3:pageSubHeader>

    <dht3:shinyBox color="orange">
        <div class="dh-section-header">Create a Group for Specific Interests and Friends</div>
        <div class="dh-section-explanation">
            Sharing links with Web Swarm using a group name is easier than selecting friends' names one by one. Groups can be public to be seen by everyone 
            or private for just group members. Any group member can invite new people and change account settings.
        </div>
        <dht:messageArea/>
		<dht:formTable tableClass="dh-form-table-orange">
			<dht:formTableRow label="Privacy">
		        <dht:formTable tableClass="dh-form-table-orange dh-no-extra-space-table" hasLabelCells="false" hasInfoCells="true">
			        <dht:formTableRow info="Visible to anyone browsing the Mugshot website">  
				        <input class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupVisibility" id="dhGroupVisibilityPublic" value="public" checked="true" onclick="dh.groupaccount.groupPrivacyChanged();"><label class="dh-label" for="dhGroupVisibilityPublic">Public</label>
				    </dht:formTableRow>
				    <dht:formTableRow info="Visible only to group members">
				        <input class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupVisibility" id="dhGroupVisibilityPrivate" value="private" onclick="dh.groupaccount.groupPrivacyChanged();"><label class="dh-label" for="dhGroupVisibilityPrivate">Private</label>
				    </dht:formTableRow>
				</dht:formTable>    
			</dht:formTableRow>
            <dht:formTableRow label="Membership" altRow="true">
		        <dht:formTable tableClass="dh-form-table-orange dh-no-extra-space-table" hasLabelCells="false" hasInfoCells="true">
			        <dht:formTableRow info="Anyone can join the group" altRow="true">  
				        <input class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupMembership" id="dhGroupMembershipOpen" value="open" checked="true"><label class="dh-label" for="dhGroupVisibilityOpen">Open</label>
				    </dht:formTableRow>
				    <dht:formTableRow info="People need an invitation to join, public groups can have followers" altRow="true">
				        <input class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupMembership" id="dhGroupMembershipByInvitation" value="byInvitation"><label class="dh-label" for="dhGroupVisibilityByInvitation">By invitation</label>
				    </dht:formTableRow>
				</dht:formTable>    
			</dht:formTableRow>			
			<dht:formTableRow label="Group name">
				<dht:textInput id="dhGroupNameEntry" disabled="${disabled}"/>
			</dht:formTableRow>
			<dht:formTableRow label="About group" altRow="true">
				<dht:textInput id="dhAboutGroupEntry" multiline="true" disabled="${disabled}"/>
			</dht:formTableRow>
		</dht:formTable>
		<div class="dh-section-explanation">
		    Save and start inviting people.
		</div>    
		<c:choose>
            <c:when test="${disabled}">
                <img id="dhCreateGroupSave" class="dh-shinybox-bottom-content" src="/images3/${buildStamp}/save_25px_disabled.gif"/>
            </c:when>
            <c:otherwise>
                <img id="dhCreateGroupSave" class="dh-shinybox-bottom-content" src="/images3/${buildStamp}/save_25px.gif" onclick="javascript:dh.groupaccount.createGroup();"/>        
            </c:otherwise>
        </c:choose>        
    </dht3:shinyBox>
</dht3:page>
</html>    
        
        