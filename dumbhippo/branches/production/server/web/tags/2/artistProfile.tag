<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="artist" required="true" type="com.dumbhippo.server.ExpandedArtistView"%>

<div class="dh-artist-profile">
    <div class="dh-artist-image">
        <img src="${artist.smallImageUrl}" width="${artist.smallImageWidth}" height="${artist.smallImageHeight}"/>
    </div>       
    <div class="dh-artist-info">      
        <div class="dh-artist-name">
            <c:out value="${artist.name}"/>
        </div>
        <div class="dh-artist-bio">
            Wikipedia entry about the artist is <a href="${artist.wikipediaUrl}" target="_blank">here</a>.
            <br/>
            Yahoo! Music entry about the artist is <a href="${artist.yahooMusicPageUrl}" target="_blank">here</a>.
	    </div>
	</div>    
</div>
