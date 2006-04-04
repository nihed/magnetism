<html>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>

<head>
	<title>Sharing a Link</title>
	<dht:stylesheets href="sharelink.css" iehref="sharelink-iefixes.css" />
	<dht:scriptIncludes/>
	<script type="text/javascript" src="javascript/dh/share.js"></script>
	<script type="text/javascript" src="javascript/dh/sharelink.js"></script>
</head>
<body scroll="no">
<dht:shareOuterContainer>
    <table id="dhShareContainer" class="dhInvisible" cellspacing="0" cellpadding="0" width="100%">
        <tr>
        <td id="dhShareLeft" width="46%">
        <table id="dhShareLeftContainer" cellspacing="0" cellpadding="0">
        <tr><td valign="top" width="100%">
        <img src="/images/${buildStamp}/dumbhippo_logo.gif"/><br/>
		<div dojoType="InlineEditBox" id="dhUrlTitleToShare">(untitled)</div>
     	<div id="dhUrlToShareDiv" class="dhLabel dhInvisible">
			Link: <input id="dhUrlToShare" type="text" class="dhText"/>
		</div>
		<div id="dhTitleError" class="dhValidityError"></div>		
        </td>
        <tr valign="bottom" height="80%">
        <td>
        <div id="dhShareSendTo">Send to:</div>
 		<dht:shareRecipientEntry/>
        </td></tr>        
        </table> <!-- end dhShareLeftContainer -->
        </td> <!-- ends dhShareLeft -->

        <td style="width: 2px">&nbsp;</td>

        <td id="dhShareRight" width="53%">
		<div id="dojoDebug"></div> <!-- where to put dojo debug spew -->
   		<dht:shareDescriptionShare/>
        </td> <!-- ends dhShareRight -->
        </tr>
    </table> <!-- ends dhShareContainer -->
</dht:shareOuterContainer>    
</body>
</html>
