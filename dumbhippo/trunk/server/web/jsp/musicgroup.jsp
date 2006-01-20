<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.GroupMusicPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="fromInvite" param="fromInvite"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="who"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<dht:errorPage>Group not found</dht:errorPage>
</c:if>

<head>
	<title><c:out value="${viewgroup.name}'s Music"/></title>
	<dht:stylesheets href="musicgroup.css" iehref="group-iefixes.css" />
	<dht:scriptIncludes/>
    <script type="text/javascript">
        dojo.require("dh.util");
    </script>
</head>
<body>

<div id="dhContainer">

	<div id="dhMainArea">
		<dht:logo/>

		<c:choose>
			<c:when test="${viewgroup.justAdded}">
				<div id="dhInformationBar"><dh:entity value="${viewgroup.inviter}" photo="true"/><p> invited you to this group, you can <a href='javascript:dh.actions.leaveGroup("${viewgroup.viewedGroupId}")'>leave</a> it any time.</p></div>
			</c:when>
			<c:otherwise>

			</c:otherwise>
		</c:choose>

		<h2 class="dh-title"><c:out value="${viewgroup.name}"/>'s Recent Songs</h2>

		<div>
			<c:forEach items="${viewgroup.latestTracks.list}" var="track">
				<dht:track track="${track}"/>
			</c:forEach>
		</div>
	
		<h2 class="dh-title"><c:out value="${viewgroup.name}"/>'s Most Played Songs</h2>

		<div>
			<c:forEach items="${viewgroup.frequentTracks.list}" var="track">
				<dht:track track="${track}"/>
			</c:forEach>
		</div>
	</div>

	<div id="dhPersonalArea">
		<div id="dhPhotoNameArea">
		<dht:groupshot group="${viewgroup.viewedGroup}" size="192"/>
		<div id="dhName"><c:out value="${viewgroup.name}"/></div>
		</div>

		<div class="dh-right-box-area">
			<div class="dh-right-box dh-right-box-last">
				<h5 class="dh-title">Group Members</h5>
				<div class="dh-people">
					<dh:entityList value="${viewgroup.activeMembers}" photos="true" music="true"/>
				</div>
			</div>
		</div>
	</div>
</div>

<div id="dhOTP">
<dht:rightColumn/>
</div>

</body>
</html>
