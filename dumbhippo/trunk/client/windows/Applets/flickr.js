dh.flickr = {}

dh.flickr.Extension = function () {

    this.getPhotoset = function(post) {
        return dh.util.dom.selectNode(post.info, "/postInfo/flickr/photos")
    }

    this.accept = function(post) {
        if (!post.info)
            return false
        dh.util.debug("checking whether post is flickr")    
        var flickrElt = this.getPhotoset(post)
        var ret = (flickrElt != null)
        dh.util.debug("post is " + (ret ? "" : "not ") + " flickr")         
        return ret
    }

    this.drawContent = function(post, postContentNode) {
        var photoset = this.getPhotoset(post)
        dh.util.debug("drawing flickr content")        
        var photos = dh.util.dom.selectNodes(photoset, "photo/photoUrl")
        if (!photos) { dh.util.debug("invalid photo metadata, no photoUrls found"); return; }
        var photosetDiv = document.createElement("div")
        photosetDiv.setAttribute("class", "dh-notification-flickr-thumbnails")
        postContentNode.appendChild(photosetDiv)
        var photoWidth = 100
        var photoHeight = 100
        var maxPhotos = 3
        for (var i = 0; (i < photos.length) && (i < maxPhotos); i++) {
            var img = document.createElement("img")
            var textChild = photos[i].firstChild
            if (!textChild) { dh.util.debug("invalid photo metadata, no text value for photoUrl found"); return; }
            var url = textChild.nodeValue
            var baseUrl = dh.core.getServerBaseUrl()
            dh.util.debug("thumbnail url = " + baseUrl + url)
            img.setAttribute("class", "dh-notification-flickr-thumbnail")
            img.setAttribute("src", baseUrl + url)
            photosetDiv.appendChild(img)
        }
    }
}

dh.bubble.postExtensions.flickr = new dh.flickr.Extension()