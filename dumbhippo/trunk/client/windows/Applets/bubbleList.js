// bubbleList.js: A window with a list of notification bubbles
// Copyright Red Hat, Inc. 2006

// Notification implementation

dh.bubblelist = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId) {
    dh.util.debug("invoking dhInit")
    
    // Set some global parameters
    dh.selfId = selfId       // Current user ID
    dh.serverUrl = serverUrl // Base URL to server-side web pages
    dh.appletUrl = appletUrl // Base URL to local content files
    
    dh.display = new dh.bubblelist.Display(serverUrl, appletUrl, selfId); 
}

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
                document.body.insertBefore(old.div, body)
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
        var height = 0
        for (var i = 0; i < this.notifications.length; i++) {
            var bubble = this.notifications[i].bubble
            var bubbleWidth = bubble.getWidth()
            var bubbleHeight = bubble.getHeight()
            
            this.notifications[i].div.style.height = (bubbleHeight + 2) + "px"
            this.notifications[i].div.style.border = "solid 1px gray"
            
            if (bubbleWidth + 2 > width)
                width = bubbleWidth + 2
            height += bubbleHeight + 2
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

// Note if you change the parameters to these function, you must change HippoBubbleList.cpp

dhAddLinkShare = function (senderName, senderId, senderPhotoUrl, postId, linkTitle, 
                           linkURL, linkDescription, personRecipients, groupRecipients, 
                           viewers, postInfo, timeout) {
    dh.bubble.addPersonName(senderId, senderName)
    personRecipients = dhAdaptLinkRecipients(personRecipients)
    groupRecipients = dh.core.adaptExternalArray(groupRecipients)
    viewers = dhAdaptLinkRecipients(viewers)
    
    var data = new dh.bubble.PostData(senderName, senderId, senderPhotoUrl, postId, linkTitle, 
                                      linkURL, linkDescription, personRecipients, groupRecipients, 
                                      viewers, postInfo)
    return dh.display.addLinkShare(data)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhBubbleListCLear = function () {
    dh.display.clear()
}
