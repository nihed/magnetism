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

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>

<dh:bean id="browser" class="com.dumbhippo.web.BrowserBean" scope="request"/>

<c:if test="${empty group.viewedGroup}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<c:if test="${!group.viewedGroup.status.canModify}">
	<dht:errorPage>Only members can edit a group</dht:errorPage>
</c:if>

<c:set var="pageName" value="Edit ${group.viewedGroup.name}" scope="page"/>

<c:choose>
    <c:when test="${browser.ie}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_ie.gif"/>
        <c:set var="browseInputSize" value="0"/>
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <%-- This is ok here, but don't use browser.firefox on the anonymous pages --%>
    <%-- for which we use a single cache for all gecko browsers. --%>
    <c:when test="${browser.firefox && browser.windows}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_ff.gif"/>   
        <c:set var="browseInputSize" value="1"/>  
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:when test="${browser.firefox && browser.linux}">
        <c:set var="browseButton" value="/images3/${buildStamp}/browse_lff.gif"/>   
        <c:set var="browseInputSize" value="1"/>  
        <c:set var="browseInputClass" value="dh-hidden-file-upload"/>
    </c:when>
    <c:otherwise>
        <c:set var="browseButton" value=""/>     
        <c:set var="browseInputSize" value="24"/>
        <c:set var="browseInputClass" value="dh-file-upload"/>
    </c:otherwise>
</c:choose>

<head>
	<title><c:out value="${pageName}"/> - Mugshot</title>
	<dht3:stylesheet name="site" iefixes="true"/>		
	<dht3:stylesheet name="account" iefixes="true"/>		
	<dht3:stylesheet name="group-account"/>		
	<dht:faviconIncludes/>
	<dh:script modules="dh.groupaccount,dh.event"/>
	<script type="text/javascript">
		dh.formtable.currentValues = {
			'dhGroupNameEntry' : <dh:jsString value="${group.name}"/>,
			'dhAboutGroupEntry' : <dh:jsString value="${!empty group.viewedGroup.group.description ? group.viewedGroup.group.description : ''}"/>
		};		
		dh.groupaccount.groupId = <dh:jsString value="${group.viewedGroupId}"/>
		dh.groupaccount.initialMembershipOpen = ${group.publicOpen};
		dh.groupaccount.followersNumber = ${group.followers.size};
		dh.groupaccount.invitedFollowersNumber = ${group.invitedFollowers.size};
		dh.groupaccount.reloadPhoto = function() {
			dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer')], 60);
		}
		dh.event.addPageLoadListener(dhGroupAccountInit);
	</script>
</head>	

<dht3:page currentPageLink="group-account">

    <dht3:pageSubHeader title="${group.viewedGroup.name} - ${group.private ? 'Private Group' : 'Public Group'}" privatePage="${group.private}">
		<dht3:groupRelatedPagesTabs group="${group.viewedGroup}"/>
	</dht3:pageSubHeader>
    
    <dht3:shinyBox color="orange">
        <c:choose>
		    <c:when test="${!group.private}">
		        <div class="dh-section-header">Public Info</div>
		        <div class="dh-section-explanation">This information will be visible on the <a href="/group?who=${group.viewedGroupId}">group page</a>.</div>
			</c:when>
			<c:otherwise>
                <div class="dh-section-header">Private Info</div>
		        <div class="dh-section-explanation">Because this group is private, this information will only be visible to group members.</div>			
			</c:otherwise>
		</c:choose>
	
		<dht:formTable tableId="dhAccountInfoForm" tableClass="dh-form-table-orange">
		    <c:if test="${!group.private}">
		        <c:choose> 
		            <c:when test="${group.publicOpen}">
		                <c:set var="openChecked" value="checked"/>
		            </c:when>
		            <c:otherwise>
		                <c:set var="byInvitationChecked" value="checked"/>
		            </c:otherwise>
		        </c:choose>    
		        <dht:formTableRowStatus controlId='dhMembershipSelection' statusLinkCount='2'></dht:formTableRowStatus>
                <dht:formTableRow label="Membership">
		            <dht:formTable tableClass="dh-form-table-orange dh-no-extra-space-table" hasLabelCells="false" hasInfoCells="true">
			            <dht:formTableRow info="Anyone can join the group">  
				            <input ${openChecked} class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupMembership" id="dhGroupMembershipOpen" value="open" onclick="dh.groupaccount.processMembershipSelection();"><label class="dh-label" for="dhGroupVisibilityOpen">Open</label>
				        </dht:formTableRow>
				        <dht:formTableRow info="Anyone can follow the group, they need an invitation to join">
				            <input ${byInvitationChecked} class="dh-radio-input" ${disabledAttr} type="radio" name="dhGroupMembership" id="dhGroupMembershipByInvitation" value="byInvitation" onclick="dh.groupaccount.processMembershipSelection();"><label class="dh-label" for="dhGroupVisibilityByInvitation">By Invitation</label>
				        </dht:formTableRow>
				    </dht:formTable>    
			    </dht:formTableRow>
			</c:if>							
		    <dht:formTableRowStatus controlId='dhGroupNameEntry'></dht:formTableRowStatus>
			<dht:formTableRow label="Group name" altRow="${!group.private}" controlId='dhGroupNameEntry'>
				<dht:textInput id="dhGroupNameEntry" extraClass="dh-name-input"/>
				<div id="dhGroupNameEntryDescription" style="display: none"></div>
			</dht:formTableRow>
			<dht:formTableRow label="About group" altRow="${group.private}" controlId='dhAboutGroupEntry'>
				<div>
			        <dht:textInput id="dhAboutGroupEntry" multiline="true"/>
					<div id="dhAboutGroupEntryDescription" style="display: none"></div>
				</div>    
			</dht:formTableRow>
			<dht:formTableRow label="Picture" altRow="${!group.private}">
				<div id="dhHeadshotImageContainer" class="dh-image">
					<dht:groupshot group="${group.viewedGroup}" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.groupaccount.reloadPhoto);" />
				</div>
				<div class="dh-next-to-image">
					<div class="dh-picture-instructions">Upload new picture:</div>
					<c:set var="location" value="/groupshots" scope="page"/>
					<c:url value="/upload${location}" var="posturl"/>
					<form id='dhPictureForm' enctype="multipart/form-data" action="${posturl}" method="post">
						<input id='dhPictureEntry' class="${browseInputClass}" type="file" name="photo" size="${browseInputSize}"/>
						<c:if test="${browseInputClass == 'dh-hidden-file-upload'}">
						    <div id='dhStyledPictureEntry' class="dh-styled-file-upload">
						        <img src="${browseButton}">
						    </div>
						</c:if>
						<input type="hidden" name="groupId" value="${group.viewedGroupId}"/>
						<input type="hidden" name="reloadTo" value="/group-account?group=${group.viewedGroupId}"/>
					</form>
					<div class="dh-picture-more-instructions">
					    or <a href="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.groupaccount.reloadPhoto);" title="Choose from a library of pictures">choose a stock picture</a>						
					</div>
					<div id="dhChooseStockLinkContainer">
					</div>		
				</div>
				<div class="dh-grow-div-around-floats"><div></div></div>				
			</dht:formTableRow>						
			<dht:formTableRowStatus controlId='dhFeedEntry'></dht:formTableRowStatus>
			<dht:formTableRow label="Feeds" altRow="${group.private}">
				<table cellpadding="0" cellspacing="0" class="dh-address-table">
					<tbody>	
						<c:forEach items="${group.feeds.list}" var="feed" varStatus="status">
							<tr>
								<td><a href="${feed.feed.link.url}" target="_blank"><c:out value="${feed.feed.title}"/></a></td>
								<td align="right" title="${feed.feed.lastFetchSucceeded ? 'This feed is working great!' : 'Something went wrong last time Mugshot checked this feed'}">
									<c:choose>
										<c:when test="${feed.feed.lastFetchSucceeded}">
											<dh:png src="/images2/${buildStamp}/check10x10.png" style="width: 10; height: 10;" klass="dh-feed-status-icon"/>
										</c:when>
										<c:otherwise>
											<dh:png src="/images2/${buildStamp}/alert10x10.png" style="width: 10; height: 10;" klass="dh-feed-status-icon"/>
										</c:otherwise>
									</c:choose>
								</td>
								<td>
									<c:set var="feedJs" scope="page">
										<jsp:attribute name="value">
											<dh:jsString value="${feed.feed.source.url}"/>
										</jsp:attribute>
									</c:set>
									<a href="javascript:dh.groupaccount.removeFeed(${feedJs});">remove</a>
								</td>
							</tr>
							<tr class="dh-feed-detail">
								<td colspan="2"><c:out value="${feed.feed.link.url}"/></td>
								<td></td>
							</tr>
							<tr class="dh-feed-spacer">
								<td></td>
								<td></td>
								<td></td>
							</tr>
						</c:forEach>
						<tr>
							<td colspan="3">
								<dht:textInput id='dhFeedEntry' maxlength="255"/>
								<img id='dhFeedAddButton' src="/images3/${buildStamp}/subscribe_button.gif" onclick="dh.groupaccount.tryAddFeed();"/>
							</td>
						</tr>
					</tbody>
				</table>
			</dht:formTableRow>
		</dht:formTable>
    </dht3:shinyBox>

</dht3:page>		
<dht:photoChooser/>
<dht:feedPopups/>
</html>							
				
				
				
				


				