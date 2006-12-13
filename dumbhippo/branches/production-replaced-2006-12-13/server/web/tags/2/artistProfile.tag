<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="artist" required="true" type="com.dumbhippo.server.views.ExpandedArtistView"%>

<div class="dh-artist-profile">
    <div class="dh-artist-image">
        <img src="${artist.smallImageUrl}" width="${artist.smallImageWidth}" height="${artist.smallImageHeight}"/>
    </div>       
    <div class="dh-artist-info">      
        <div class="dh-artist-name">
            <c:out value="${artist.name}"/>
        </div>
        <div class="dh-artist-bio">
            <a href="${artist.wikipediaUrl}" target="_blank"><c:out value="${artist.name}"/> on Wikipedia</a>.
            <br/>
            <a href="${artist.yahooMusicPageUrl}" target="_blank"><c:out value="${artist.name}"/> on Yahoo!</a>.
	    </div>
	</div>
</div>
