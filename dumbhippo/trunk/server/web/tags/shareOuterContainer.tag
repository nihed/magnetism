<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%-- This tag defines the outer border of a share --%>
<div id="dhShareOuterDiv">
<table id="dhShareOuter" cellspacing="0" cellpadding="0" width="100%" height="100%">
    <tr><td align="left" id="dhShareTL"><img src="/images/${buildStamp}/shareLinkTL.png"/></td>
    <td id="dhShareTop">&nbsp;</td>
    <td align="right" id="dhShareTR"><img src="/images/${buildStamp}/shareLinkTR.png"/></td>
    </tr>
    <tr>
    <td id="dhShareLeftBorder" align="left">&nbsp;</td>    
    <td id="dhShareMain" align="center" valign="middle">
	<jsp:doBody/>
	</td>
    <td id="dhShareRightBorder" align="right">&nbsp;</td>
    </tr>
    <tr>
    <td align="left" valign="bottom" id="dhShareBL"><img src="/images/${buildStamp}/shareLinkBL.png"/></td>
    <td width="100%" valign="bottom" id="dhShareBottom">&nbsp;</td>
    <td align="right" valign="bottom" id="dhShareBR"><img src="/images/${buildStamp}/shareLinkBR.png"/></td>
    </tr>
</table> <!-- ends dhShareOuter --> 
</div> <!-- ends dhShareOuterDiv -->