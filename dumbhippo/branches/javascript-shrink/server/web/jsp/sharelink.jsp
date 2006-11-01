<html>
<%@ page pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<dh:bean id="sharelink" class="com.dumbhippo.web.pages.ShareLinkPage" scope="request"/>

<head>
	<title>Sharing a Link</title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dh:script module="dh.share"/>
	<dh:script module="dh.sharelink"/>
</head>
<body scroll="no">
<dht:shareOuterContainer>
    <table id="dhShareContainer" class="dhInvisible" cellspacing="0" cellpadding="0" width="100%">
        <tr>
        <td id="dhShareLeft" width="46%">
        <table id="dhShareLeftContainer" cellspacing="0" cellpadding="0">
    	    <tr><td><img src="/images2/${buildStamp}/lslinkswarm.gif"/></td></tr>
        	<tr><td width="100%" style="overflow: hidden"><div dojoType="InlineEditBox" id="dhUrlTitleToShare">(untitled)</div>
        			<div id="dhTitleError" class="dhValidityError"></div></td></tr>
		    <tr><td><div id="dhUrlToShareDiv" class="dhLabel dhInvisible">
				Link: <input id="dhUrlToShare" type="text" class="dhText"/></div></td></tr>
        	</td>
	        <tr valign="bottom" height="80%">
    	    <td>
	        <div id="dhShareSendTo">Send to:</div>
 			<dht:shareRecipientEntry/>
        	</td></tr>        
        </table> <!-- end dhShareLeftContainer -->
        </td> <!-- ends dhShareLeft -->

        <td width="2px">&nbsp;</td>

        <td id="dhShareRight" width="53%">
		<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
   		<dht:shareDescriptionShare onsubmit="dh.sharelink.submitButtonClicked();"/>
        </td> <!-- ends dhShareRight -->
        </tr>
    </table> <!-- ends dhShareContainer -->
</dht:shareOuterContainer>    
</body>
</html>
