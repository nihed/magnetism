// bubbleList.js: A window with a list of notification bubbles
// Copyright Red Hat, Inc. 2006

// Notification implementation

dh.bubblelist = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId, maxVerticalSize) {
    dh.util.debug("invoking dhInit")
    
    // Set some global parameters
    dh.selfId = selfId       // Current user ID
    dh.serverUrl = serverUrl // Base URL to server-side web pages
    dh.appletUrl = appletUrl // Base URL to local content files
    dh.maxVerticalSize = maxVerticalSize // Maximum size of content area vertically
    
    dh.display = new dh.bubblelist.Display(serverUrl, appletUrl, selfId); 
}

dh.bubblelist.BORDER = 2 // Border around entire page
dh.bubblelist.BUBBLE_MARGIN = 7 // Margin around bubbles (collapses between)

dh.bubblelist.Display = function (serverUrl, appletUrl, selfId) {
    // Whether the user is currently using the computer
    this._idle = false
    
    this.notifications = []
    
    this._addNotification = function (data) {
        var bubble = new dh.bubble.Bubble(false)
        
        var display = this;
        bubble.onLinkClick = function(postId, url) {
            window.external.application.DisplaySharedLink(postId, url)
        }
    
        var div = bubble.create()
        
        // Add a div that we'll use to hide a CSS positioning bug in IE
        var borderFixupDiv = document.createElement("div")
        borderFixupDiv.className = "dh-bubble-border-fixup"
        div.appendChild(borderFixupDiv)
        
        var bubbleListTop = document.getElementById("dhBubbleListTop")
        bubbleListTop.insertBefore(div, bubbleListTop.firstChild)
        bubble.setData(data)
    
        bubble.onSizeChange = function() {
            display._updateSize()
        }
    
        this.notifications.unshift({bubble: bubble,
                                    data: data,
                                    div: div})
                                 
        this._updateSize()
    }
        
    this.addLinkShare = function (share) {
        // If we already are displaying this post, update it and move it to the top
        for (var i = 0; i < this.notifications.length; i++) {
            if (this.notifications[i].data instanceof dh.bubble.PostData &&
                this.notifications[i].data.postId == share.postId) {
                var old = this.notifications[i]
                this.notifications.splice(i, 1)
                this.notifications.unshift(old)
                
                old.bubble.setData(share)
                var bubbleListTop = document.getElementById("dhBubbleListTop")
                bubbleListTop.removeChild(old.div)
                bubbleListTop.insertBefore(old.div, bubbleListTop.firstChild)
                return
            }
        }

        // Otherwise, insert at the top
        this._addNotification(share)
    }
    
    this.addMySpaceComment = function (comment) {
        // If we already have a notification for the same comment, ignore this
        for (var i = 0; i < this.notifications.length; i++) {
            if (this.notifications[i].data instanceof dh.bubble.MySpaceData &&
                this.notifications[i].data.commentId == comment.commentId) {
                return;
            }
        }
        
        // Otherwise, insert at the top
        this._addNotification(comment)
    }
    
    this.clear = function() {
        this.notifications = []
        var bubbleListTop = document.getElementById("dhBubbleListTop")
        dh.util.dom.clearNode(bubbleListTop)
    }
    
    this._updateSize = function() {
        var width = 0
        var height = dh.bubblelist.BUBBLE_MARGIN + 2 * dh.bubblelist.BORDER
        for (var i = 0; i < this.notifications.length; i++) {
            var bubble = this.notifications[i].bubble
            // The + 1 here is to make the default size even, so the borders match;
            // see the comment for '.dh-bubble-border-fixup' in bubbleList.css for 
            // why we'll end up with 1 pixel more on the right otherwise
            var bubbleWidth = bubble.getWidth() + 2 * (dh.bubblelist.BUBBLE_MARGIN + dh.bubblelist.BORDER) + 1
            var bubbleHeight = bubble.getHeight()
            
            this.notifications[i].div.style.height = bubbleHeight + "px"
            
            if (bubbleWidth > width)
                width = bubbleWidth
            height += bubbleHeight + dh.bubblelist.BUBBLE_MARGIN
        }
        if (height > dh.maxVerticalSize) {
            // Make the horizontal scrollbar not turn on and allow the user to
            // scroll into regions unknown; this presumably has to
            // do with some absolutely positioned element which is in the wrong
            // place or is the wrong size, but it's easier to just fix the
            // width here than track that down. There's also a height mismatch
            // which is more identifiable; it's because of the dh-bubble-border-fixup 
            // div which is not precisely sized to the size of each bubble
            bubbleListTop = document.getElementById("dhBubbleListTop")
            bubbleListTop.style.width = width
            bubbleListTop.style.height = height

            height = dh.maxVerticalSize
            width += 21; // Allow for the width of the scrollbar
            document.body.scroll = "yes"
        } else {
            document.body.scroll = "no"
        }
        
        window.external.application.Resize(width, height)
    }
}

dhAdaptLinkRecipients = function (recipients) {
    var result = []
    var tmp = dh.core.adaptExternalArray(recipients)
    for (var i = 0; i < tmp.length; i += 2) {
        result.push({ id: tmp[i], name: tmp[i + 1] })
    }
    return result;
}

// Global namespace since it's painful to do anything else from C++

dhAddPerson = function (id, name, smallPhotoUrl) 
{
    dh.util.debug("adding person " + id + " " + name + " " + smallPhotoUrl)
    var person = new dh.bubble.Person(id, name, smallPhotoUrl)
    dh.bubble.addEntity(person)
}

dhAddResource = function (id, name) 
{
    dh.util.debug("adding resource " + id + " " + name)
    var res = new dh.bubble.Resource(id, name)
    dh.bubble.addEntity(res)
}

dhAddGroup = function (id, name, smallPhotoUrl) 
{
    dh.util.debug("adding group " + id + " " + name + " " + smallPhotoUrl)
    var grp = new dh.bubble.Group(id, name, smallPhotoUrl)
    dh.bubble.addEntity(grp)
}

// Note if you change the parameters to these function, you must change HippoBubbleList.cpp

dhAddLinkShare = function (senderId, postId, linkTitle,
                           linkURL, linkDescription, recipients,
                           viewers, postInfo, timeout, viewerHasViewed) {
    viewers = dh.core.adaptExternalArray(viewers)
    recipients = dh.core.adaptExternalArray(recipients)
    
    var data = new dh.bubble.PostData(senderId, postId, linkTitle, 
                                      linkURL, linkDescription, recipients, 
                                      viewers, postInfo, viewerHasViewed)
    dh.display.addLinkShare(data)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhBubbleListClear = function () {
    dh.display.clear()
}
