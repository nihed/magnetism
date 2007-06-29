<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="dht" %>
<%-- This tag defines the outer border of a share --%>
<div id="dhShareOuterDiv">
<table id="dhShareOuter" cellspacing="0" cellpadding="0" width="100%" height="100%">
	<tr style="height: 8px">
	<td class="dh-share-border" style="width: 8px;"></td>
	<td class="dh-share-border" style="width: 12px;"></td>
	<td class="dh-share-border"></td>
	<td class="dh-share-border" style="width: 12px;"></td>
	<td class="dh-share-border" style="width: 8px;"></td>
	</tr>
    <tr style="height: 12px">
	<td class="dh-share-border"></td>
    <td align="left" id="dhShareTL"><img src="/images2/${buildStamp}/lswhite_tl.png"/></td>
    <td></td>
    <td align="right" id="dhShareTR"><img src="/images2/${buildStamp}/lswhite_tr.png"/></td>
	<td class="dh-share-border"></td>
    </tr>
    <tr>
	<td class="dh-share-border"></td>
    <td></td>
    <td id="dhShareMain" align="center" valign="middle">
		<jsp:doBody/>
	</td>
    <td></td>
	<td class="dh-share-border"></td>
    </tr>
    <tr style="height: 12px">
	<td class="dh-share-border"></td>
    <td align="left" valign="bottom" id="dhShareBL"><img src="/images2/${buildStamp}/lswhite_bl.png"/></td>
    <td></td>
    <td align="right" valign="bottom" id="dhShareBR"><img src="/images2/${buildStamp}/lswhite_br.png"/></td>
	<td class="dh-share-border"></td>
    </tr>
	<tr style="height: 8px">
	<td class="dh-share-border"></td>
	<td class="dh-share-border"></td>
	<td class="dh-share-border"></td>
	<td class="dh-share-border"></td>
	<td class="dh-share-border"></td>
	</tr>
</table> <!-- ends dhShareOuter --> 
</div> <!-- ends dhShareOuterDiv -->