<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="onsubmit" required="true" type="java.lang.String"%>
<%-- The description entry area and share button --%>
<table id="dhShareRightOuterContainer" cellspacing="0" cellpadding="0" width="100%">
	<tr height="8">
    <td width="8" align="left" valign="top"><img src="/images2/${buildStamp}/lsorange_tl.png" class="dhShareTopCornerImg"/></td>
    <td></td>
	<td width="8" align="right" valign="top"><img src="/images2/${buildStamp}/lsorange_tr.png" class="dhShareTopCornerImg"/></td>
    </tr>
    <tr>
    <td></td>
    <td><div id="dhShareDescriptionLabel">Description:</div></td>
    <td></td>
    </tr>
    <tr>
    <td></td>
    <td id="dhShareDescriptionArea">
    <table id="dhShareDescriptionContainer" cellspacing="0" cellpadding="0" width="100%">
        <td align="center" valign="top" width="100%"><textarea id="dhShareDescriptionTextArea" name="dhShareDescriptionTextArea" tabindex="2"></textarea></td>
    </table>
    </td> <!-- ends dhShareDescriptionArea -->
    <td></td>
    </tr>
    <tr>
    <td align="left" valign="bottom"><img src="/images2/${buildStamp}/lsorange_bl.png" class="dhShareBottomCornerImg"/></td>
    <td valign="middle" align="left"><div id="dhShareDescriptionBottomFill"><dht:createGroupDialog/></div></td>
    <td align="right" valign="bottom">
    	<div style="position: relative;">
    		<img id="dhShareShareButton" class="dh-share-share-button" src="/images2/${buildStamp}/sendbutton.gif" onclick="${onsubmit}" tabindex="3"/>
			<img id="dhShareShareButtonClicked" class="dh-share-share-button" src="/images2/${buildStamp}/sendbutton2.gif" style="display: none;"/>
		</div>
	</td>
    </tr>
</table> 
