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
dh.bubblelist.BUBBLE_MARGIN = 8 // Margin around bubbles (collapses between)

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
                this.notifications[i].data.post.Id == share.post.Id) {
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
    
    this.updatePost = function (id) {
        for (var i = 0; i < this.notifications.length; i++) {
            if (this.notifications[i].data instanceof dh.bubble.PostData &&
                this.notifications[i].data.post.Id == id) {
                var old = this.notifications[i]
                old.bubble.setData(old.data)
                return
            }
        }
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
        var someChanged = false
        for (var i = 0; i < this.notifications.length; i++) {
            var bubble = this.notifications[i].bubble
            var bubbleWidth = bubble.getWidth() + 2 * (dh.bubblelist.BUBBLE_MARGIN + dh.bubblelist.BORDER)
            var bubbleHeight = bubble.getHeight()
            
            // In some cases, IE doesn't reflow the list correctly on height changes
            // artificially forcing a size change on all the bubbles after the one
            // that actually changed by setting their height to 0 then back to
            // the correct height works around this
            var lastHeight = this.notifications[i].lastHeight
            var heightChanged = lastHeight == null || lastHeight != bubbleHeight
            if (someChanged)
                this.notifications[i].div.style.height = "0px"
            this.notifications[i].lastHeight = bubbleHeight
            
            this.notifications[i].div.style.height = bubbleHeight + "px"
            
            if (bubbleWidth > width)
                width = bubbleWidth
            height += bubbleHeight + dh.bubblelist.BUBBLE_MARGIN
            
            if (heightChanged)
                someChanged = true
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
            width += 16; // Allow for the width of the scrollbar
            document.body.scroll = "yes"
        } else {
            document.body.scroll = "no"
        }
        
        window.external.application.Resize(width, height)
    }
}

// Global namespace since it's painful to do anything else from C++

// Note if you change the parameters to these function, you must change HippoBubbleList.cpp

dhAddLinkShare = function (post) { 
    var data = new dh.bubble.PostData(post)
    dh.display.addLinkShare(data)
}

dhUpdatePost = function (post) {
    dh.display.updatePost(post.Id)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhBubbleListClear = function () {
    dh.display.clear()
}

