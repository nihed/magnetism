<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/jsp/dumbhippo.tld" prefix="dh" %>

<%@ attribute name="musicOn" required="true" type="java.lang.Boolean"%>

<c:if test="${musicOn}">
        <c:set var="dhMusicClass" value="dh-music-sharing-toggle-on"/>
        <c:set var="dhMusicText" value="Turn Music Sharing Off"/>
        <c:set var="dhMusicIcon" value="audio-volume-muted.png"/>
</c:if>
<c:if test="${!musicOn}">
        <c:set var="dhMusicClass" value="dh-music-sharing-toggle-off"/>
        <c:set var="dhMusicText" value="Turn Music Sharing On"/>
        <c:set var="dhMusicIcon" value="audio-volume-high.png"/>
</c:if>

<a class="dh-music-sharing-toggle ${dhMusicClass}" href="javascript:dh.actions.setMusicSharingEnabled(${!musicOn});"><dh:png klass="dh-music-icon" src="/images/${buildStamp}/${dhMusicIcon}" style="width: 24; height: 24;"/>${dhMusicText}</a>
