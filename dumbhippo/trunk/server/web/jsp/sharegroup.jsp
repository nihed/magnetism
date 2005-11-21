<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}"><jsp:forward page="/jsp/nogroup.jsp"/></c:if>

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<dht:stylesheets href="sharelink.css" />
	<title>Share <c:out value="${viewgroup.name}"/></title>
	<dht:scriptIncludes/>
	<script type="text/javascript">
		dojo.require("dh.sharegroup");
		dhShareGroupId = "${viewgroup.viewedGroupId}";
		dhShareGroupName = "${viewgroup.name}";
	</script>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
</head>
<body>
	<dht:header>
		Sharing ${viewgroup.name}
	</dht:header>

<div id="dhMain">
<!--  invisible at first to avoid flicker while we set up dojo widgets -->
	<div id="dhShareGroupForm" class="dhInvisible">
		<div class="dhVerticalPadding"></div>

		<h2>Invite friends to <c:out value="${viewgroup.name}"/></span></h2>
		
		<div class="dhVerticalPadding"></div>
		<div class="dhLabel">Share <u>W</u>ith:</div>
		
		<table>
		<tr>
		<td>
			<input dojoType="FriendComboBox" autocomplete="off" accesskey="w"
					type="text" id="dhRecipientComboBox"/>
		</td>
		<td>
			<input type="button" value="Add" accesskey="a" class="dhButton" 
					onclick="dh.share.doAddRecipientFromCombo();"/>
		</td>
		</tr>
		</table>
		
		<div id="dhRecipientsError" class="dhValidityError"></div>

		<div class="dhVerticalPadding"></div>
		
		<div id="dhRecipientListArea" class="dhBackgroundBox">		
			<div id="dhRecipientList"> </div>
		</div><!-- end of div containing create group link and recipient list -->

		<div class="dhVerticalPadding"></div>

		<div class="dhLabel"><u>C</u>omment:</div>
		<div class="dhTextArea" id="dhShareLinkDescription" /></div>

		<div class="dhVerticalPadding"></div>

		<input type="button" value="Share" accesskey="s"
				class="dhButton" onclick="dh.sharegroup.submitButtonClicked();"/>

		<div class="dhVerticalPadding"></div>

		<div id="dojoDebug"/> <!-- where to put dojo debug spew -->
	</div><!-- end dhShareGroupForm -->
<div><!-- end dhMain -->
</body>
</html>
