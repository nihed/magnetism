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

dh.bubblelist.BUBBLE_MARGIN = 3 // Margin around bubbles (collapses between)

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
        document.body.insertBefore(div, document.body.firstChild)
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
                document.body.removeChild(old.div)
                document.body.insertBefore(old.div, document.body.firstChild)
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
        dh.util.dom.clearNode(document.body)
    }
    
    this._updateSize = function() {
        var width = 0
        var height = dh.bubblelist.BUBBLE_MARGIN
        for (var i = 0; i < this.notifications.length; i++) {
            var bubble = this.notifications[i].bubble
            var bubbleWidth = bubble.getWidth() + 2 * dh.bubblelist.BUBBLE_MARGIN
            var bubbleHeight = bubble.getHeight()
            
            this.notifications[i].div.style.height = bubbleHeight + "px"
            
            if (bubbleWidth > width)
                width = bubbleWidth
            height += bubbleHeight + dh.bubblelist.BUBBLE_MARGIN
        }
        if (height > dh.maxVerticalSize) {
            height = dh.maxVerticalSize
            width += 17; // Scrollbar width
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
