<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<dh:bean id="group" class="com.dumbhippo.web.pages.GroupPage" scope="request"/>
<jsp:setProperty name="group" property="viewedGroupId" param="group"/>

<c:if test="${empty group.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<c:if test="${!group.viewedGroup.status.canModify}">
	<dht:errorPage>Only members can edit a group</dht:errorPage>
</c:if>

<head>
	<title>Edit <c:out value="${group.name}"/></title> <%-- see also groupaccount.js --%>
	<dht:siteStyle/>	
	<link rel="stylesheet" type="text/css" href="/css2/${buildStamp}/group-account.css"/>
	<dht:faviconIncludes/>
	<dh:script modules="dh.groupaccount,dh.event"/>
	<script type="text/javascript">
		dh.formtable.currentValues = {
			'dhGroupNameEntry' : <dh:jsString value="${group.name}"/>,
			'dhAboutGroupEntry' : <dh:jsString value="${!empty group.viewedGroup.group.description ? group.viewedGroup.group.description : ''}"/>
		};		
		dh.groupaccount.groupId = <dh:jsString value="${group.viewedGroupId}"/>
		dh.groupaccount.reloadPhoto = function() {
			dh.photochooser.reloadPhoto([document.getElementById('dhHeadshotImageContainer'),
			                             document.getElementById('dhSidebarBoxProfileGroupImage')], 60);
		}
		dh.event.addPageLoadListener(dhGroupAccountInit);
	</script>
</head>
<dht:twoColumnPage>
	<dht:sidebarGroup onAccountPage="true"/>
	<dht:contentColumn>
		<dht:zoneBoxGroups back='true'>
			<c:choose>
				<c:when test="${group.public}">
					<dht:zoneBoxTitle>PUBLIC INFO</dht:zoneBoxTitle>
					<dht:zoneBoxSubtitle>This information will be visible on the <a href="/group?who=${group.viewedGroupId}">group page</a>.</dht:zoneBoxSubtitle>
				</c:when>
				<c:otherwise>
					<dht:zoneBoxTitle>PRIVATE INFO</dht:zoneBoxTitle>
					<dht:zoneBoxSubtitle>This information will only be visible to group members.</dht:zoneBoxSubtitle>
				</c:otherwise>
			</c:choose>
			
			<dht:formTable>
				<dht:formTableRowStatus controlId='dhGroupNameEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Group Name">
					<dht:textInput id="dhGroupNameEntry"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhAboutGroupEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="About Group">
					<dht:textInput id="dhAboutGroupEntry" multiline="true"/>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhPictureEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Picture">
					<div id="dhHeadshotImageContainer" class="dh-image">
						<dht:groupshot group="${group.viewedGroup}" customLink="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.groupaccount.reloadPhoto);" />
					</div>
					<div class="dh-next-to-image">
						<div>Upload new picture:</div>
						<c:set var="location" value="/groupshots" scope="page"/>
						<c:url value="/upload${location}" var="posturl"/>
						<div>
							<form enctype="multipart/form-data" action="${posturl}" method="post">
								<input id='dhPictureEntry' type="file" name="photo"/>
								<input type="hidden" name="groupId" value="${group.viewedGroupId}"/>
								<input type="hidden" name="reloadTo" value="/group-account?group=${group.viewedGroupId}"/>
							</form>
						</div>
						<div id="dhChooseStockLinkContainer">
						</div>
						<div>
							or <a href="javascript:dh.photochooser.show(document.getElementById('dhChooseStockLinkContainer'), dh.groupaccount.reloadPhoto);" title="Choose from a library of pictures">choose stock picture</a>
						</div>
					</div>
					<div class="dh-grow-div-around-floats"><div></div></div>
				</dht:formTableRow>
				<dht:formTableRowStatus controlId='dhFeedEntry'></dht:formTableRowStatus>
				<dht:formTableRow label="Feeds">
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
									<input id='dhFeedAddButton' type="button" value="Subscribe" onclick="dh.groupaccount.tryAddFeed();"/>
								</td>
							</tr>
						</tbody>
					</table>
				</dht:formTableRow>
			</dht:formTable>			
		</dht:zoneBoxGroups>
	</dht:contentColumn>
	<dht:photoChooser/>
	<dht:feedPopups/>
</dht:twoColumnPage>
</html>
