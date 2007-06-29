<%-- the contentType breaks the Eclipse jsp editor,
     you need to do Open With, Text Editor instead --%>
<%@ page pageEncoding="UTF-8" contentType="text/javascript"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>
<dht:requirePersonBean asOthersWouldSee="true" needExternalAccounts="true"/>
<jsp:setProperty name="person" property="viewedUserId" param="who"/>
document.write(<dh:jsString value="${person.whereImAtHtml}"/>);
