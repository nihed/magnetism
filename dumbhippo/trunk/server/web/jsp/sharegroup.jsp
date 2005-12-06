<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.ViewGroupPage" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<jsp:forward page="/jsp/nogroup.jsp"/>
</c:if>

<head>
	<title>Share <c:out value="${viewgroup.name}"/></title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dht:scriptIncludes/>
	<object classid="clsid:5A96BF90-0D8A-4200-A23B-1C8DABC0CC04" id="dhEmbedObject"></object>
	<script type="text/javascript">
		dojo.require("dh.sharegroup");
		dhShareGroupId = "${viewgroup.viewedGroupId}";
		dhShareGroupName = "${viewgroup.name}";
	</script>
</head>

<body>
	<dht:header>
		Sharing ${viewgroup.name}
	</dht:header>


	<div id="dhMain">
		<!--  invisible at first to avoid flicker while we set up dojo widgets -->
		<div id="dhShareForm" class="dhInvisible">
			<div class="dhVerticalPadding"></div>

			<h2>Invite friends to <c:out value="${viewgroup.name}"/></h2>			
			
			<div class="dhVerticalPadding"></div>
			<div class="dhLabel">Share <u>W</u>ith:</div>

			<table>
			<tbody>
			<tr>
			<td>
				<input autocomplete="off" accesskey="w"
						type="text" id="dhRecipientComboBox" class="dhText"/>
				<div id="dhAutoSuggest" class="dhInvisible"><ul></ul></div>
			</td>
			<td>
				<input type="button" value="Add" accesskey="a" class="dhButton" 
						onclick="dh.share.doAddRecipientFromCombo();"/>
			</td>
			</tr>
			</tbody>
			</table>
			
			<div id="dhRecipientsError" class="dhValidityError"></div>
	
			<div class="dhVerticalPadding"></div>
			
			<div id="dhRecipientListArea" class="dhBackgroundBox">		
				<div id="dhRecipientList"> </div>
			</div>
	
			<div class="dhVerticalPadding"></div>
	
			<div class="dhLabel"><u>D</u>escription:</div>
			<div class="dhTextArea" id="dhShareDescription">
			</div>
	
			<div class="dhVerticalPadding"></div>
	
			<input type="button" value="Share" accesskey="s"
					class="dhButton share" onclick="dh.sharegroup.submitButtonClicked();"/>
	
			<div class="dhVerticalPadding"></div>
	
			<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
		</div><!-- end dhShareForm -->

	</div><!--  end dhMain -->
</body>
</html>
