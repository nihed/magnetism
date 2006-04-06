<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%@ attribute name="onsubmit" required="true" type="java.lang.String"%>
<%-- The description entry area and share button --%>
<table id="dhShareRightOuterContainer" cellspacing="0" cellpadding="0" width="100%">
	<tr height="5px">
    <td align="left" valign="top"><img src="/images/${buildStamp}/blue_tl.gif" class="dhShareTopCornerImg"/></td>
    <td>&nbsp;</td>
	<td align="right" valign="top"><img src="/images/${buildStamp}/blue_tr.gif" class="dhShareTopCornerImg"/></td>
    </tr>
    <tr>
    <td></td><td><div id="dhShareDescriptionLabel">Description:</div>
    </td><td></td>
    </tr>
    <tr>
    <td></td>
    <td id="dhShareDescriptionArea">
    <table id="dhShareDescriptionContainer" cellspacing="0" cellpadding="0" width="100%">
        <tr height="5px">
    	<td align="left" valign="top"><img src="/images/${buildStamp}/whiteinner_tl.gif" class="dhShareTopCornerImg"/></td>
        <td>&nbsp;</td>
        <td align="right" valign="top"><img src="/images/${buildStamp}/whiteinner_tr.gif" class="dhShareTopCornerImg"/></td>
        </tr>
        <tr height="100%">
        <td style="width: 5px;">&nbsp;</td>
        <td align="center" valign="top" width="100%"><textarea id="dhShareDescriptionTextArea" name="dhShareDescriptionTextArea" tabindex="2"></textarea></td>
        <td></td>
        <tr height="5px">
        <td align="left" valign="bottom"><img src="/images/${buildStamp}/whiteinner_bl.gif" class="dhShareBottomCornerImg"/></td>
        <td><div>&nbsp;</div></td>
        <td align="right" valign="bottom"><img src="/images/${buildStamp}/whiteinner_br.gif" class="dhShareBottomCornerImg"/></td>
        </tr>
    </table>
    </td> <!-- ends dhShareDescriptionArea -->
    <td></td>
    </tr>
    <tr>
    <td align="left" valign="bottom"><img src="/images/${buildStamp}/blue_bl.gif" class="dhShareBottomCornerImg"/></td>
    <td valign="middle" align="left"><div id="dhShareDescriptionBottomFill"><dht:createGroupDialog/></div></td>
    <td align="right" valign="bottom"><div style="position: relative;"><img id="dhShareShareButton" src="/images/${buildStamp}/but_share.gif" onclick="${onsubmit}" tabindex="3"/></div></td>
    </tr>
</table> 
