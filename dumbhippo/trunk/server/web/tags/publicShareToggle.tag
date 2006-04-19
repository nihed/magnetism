<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="defaultPublic" required="true" type="java.lang.Boolean"%>

<dh:png src="/images/${buildStamp}/worldShare.png" style="width: 48; height: 48; float: left;"/>
Send posts to <b>The World</b> by default:<br/>
<form>
<input name="defaultPublicShare" type="radio" id="defaultPublicOn" 
       <c:if test="${defaultPublic}">checked="true"</c:if>
       onclick="dh.actions.setDefaultSharePublic(true);"> <label for="defaultPublicOn">Share posts with the world</label><br/>
<input name="defaultPublicShare" type="radio" id="defaultPublicOff" 
       <c:if test="${!defaultPublic}">checked="true"</c:if>
       onclick="dh.actions.setDefaultSharePublic(false);"> <label for="defaultPublicOff">Keep posts private</label><br/>
</form>

