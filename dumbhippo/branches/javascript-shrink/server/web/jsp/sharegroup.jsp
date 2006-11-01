<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="viewgroup" class="com.dumbhippo.web.pages.*Page" scope="request"/>
<jsp:setProperty name="viewgroup" property="viewedGroupId" param="groupId"/>

<c:if test="${empty viewgroup.viewedGroupId}">
	<jsp:forward page="/jsp/nogroup.jsp"/>
</c:if>

<head>
	<title>Share <c:out value="${viewgroup.name}"/></title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
		<dh:script module="dh.sharegroup"/>
	<script type="text/javascript">
		dhShareGroupId = "${viewgroup.viewedGroupId}";
		dhShareGroupName = "${viewgroup.name}";
		dhShareGroupInvitationCount = "${viewgroup.invitations}";
		dhShareGroupIsForum = ${viewgroup.forum};
	</script>
</head>

<body scroll="no">
<dht:shareOuterContainer>
    <table id="dhShareContainer" class="dhInvisible" cellspacing="0" cellpadding="0" width="100%">
        <tr>
        <td id="dhShareLeft" width="46%">
        <table id="dhShareLeftContainer" cellspacing="0" cellpadding="0">
			<tr><td valign="top" width="100%">
        	<img src="/images/${buildStamp}/dumbhippo_logo.gif"/><br/>
			<dht:largeTitle>Invite friends to <c:out value="${viewgroup.name}"/></dht:largeTitle>
			</td>
    	    <tr valign="bottom" height="80%">
        	<td>
	        <div id="dhShareSendTo">Share <u>W</u>ith:</div>
    	    <div><span id="dhInvitationsRemainingMessage" style="display: none;"></span></div> 	        
 			<dht:shareRecipientEntry/>
	        </td></tr>        
        </table> <!-- end dhShareLeftContainer -->
        </td> <!-- ends dhShareLeft -->

        <td style="width: 2px">&nbsp;</td>

        <td id="dhShareRight" width="53%">
		<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
   		<dht:shareDescriptionShare onsubmit="dh.sharegroup.submitButtonClicked();"/>		
        </td> <!-- ends dhShareRight -->
        </tr>
    </table> <!-- ends dhShareContainer -->
</dht:shareOuterContainer>    
</body>
</html>			

