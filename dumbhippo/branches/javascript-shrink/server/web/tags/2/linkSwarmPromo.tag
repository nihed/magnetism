<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags/2" prefix="dht" %>

<%@ attribute name="separator" required="false" type="java.lang.Boolean" %>
<%@ attribute name="linksLink" required="false" type="java.lang.Boolean" %>
<%@ attribute name="browserInstructions" required="false" type="java.lang.Boolean" %>

<table><tr>
<td rowSpan="2"><img src="/images2/${buildStamp}/buzzer50x44.gif" /></td>
<td class="dh-promo-description"><strong>Web Swarm</strong> lets you share and chat about links with friends.</td>
</tr><tr>
<td>
	<span class="dh-option-list"><%-- Just want to hack the style of this one element --%>
	<a style="margin-left:0;" class="dh-option-list-option" href="/links-learnmore">Learn more</a>
	<c:if test="${!empty browserInstructions && browserInstructions}">
	|
	<a class="dh-option-list-option" href="/bookmark">Browser instructions</a>
    </c:if>
    <c:if test="${!empty linksLink && linksLink}">
	|
	<a class="dh-option-list-option" href="/links">See what people are sharing</a>
    </c:if>	
	</span>
</td>
</tr></table>
<c:if test="${!empty separator && separator}">
        <dht:zoneBoxSeparator/>
</c:if>

