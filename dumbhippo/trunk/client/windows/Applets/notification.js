// Notification implementation

dh.notification = {}

dh.display = null;

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId) {
    dh.util.debug("invoking dhInit")
    
    // Set some global parameters
    dh.selfId = selfId       // Current user ID
    dh.serverUrl = serverUrl // Base URL to server-side web pages
    dh.appletUrl = appletUrl // Base URL to local content files
    
    dh.display = new dh.notification.Display(serverUrl, appletUrl, selfId); 
}

// Constants for the "why" of showing shares
dh.notification.NEW = 1
dh.notification.VIEWER = 2
dh.notification.MESSAGE = 4
dh.notification.ACTIVITY = 8

dh.notification.Display = function (serverUrl, appletUrl, selfId) {
    // Whether the user is currently using the computer
    this._idle = false
    
    // Whether the bubble is showing
    this._visible = false
    
    this._pageTimeoutId = null

    this._defaultTimeout = 7  // in seconds
    
    this._initNotifications = function() {
        // postid or groupid -> notification
        this.notifications = []
        this.position = -1
    }
    
    this._initNotifications() // And do it initially
    
    this._bubble = new dh.bubble.Bubble(true)
    
    var display = this;
    this._bubble.onClose = function() {
        dh.util.debug("got close event");
        display._markCurrentAsSeen();
        dh.util.debug("done marking current as seen, closing")
        display.close();
    }
    this._bubble.onPrevious = function() {
        display.goPrevious();
    }
    this._bubble.onNext = function() {
        display._markCurrentAsSeen()
        display.goNextOrClose();
    }
    this._bubble.onLinkClick = function(postId, url) {
        window.external.application.DisplaySharedLink(postId, url)
        display._markAsSeenGoNext()
    }
    
    document.body.appendChild(this._bubble.create())

    this._updateIdle = null;
    this._idleUpdateDisplay = function() {
        if (this._updateIdle)
            return;
    
        window.setTimeout(function () {
            this._updateIdle = null;
            window.external.application.UpdateDisplay();
        }, 0)
    }
    
    this._bubble.onSizeChange = function() {
        window.external.application.Resize(display._bubble.getWidth(), display._bubble.getHeight())
        display._idleUpdateDisplay()
    }
    this._bubble.onSizeChange()
    
    this._bubble.onUpdateDisplay = function() {
        display._idleUpdateDisplay()
    }
    
    // returns true if the bubble result is that the bubble is displayed
    this._pushNotification = function (nType, data, timeout, why) {
        this.notifications.push({notificationType: nType,
                                 state: "pending",
                                 data: data,
                                 timeout: timeout,
                                 why: why})
        dh.util.debug("position " + this.position + " notifications: " + this.notifications)                                 
        var result = false
        if (this.position < 0) {
            this.setPosition(0)
            result = true
        }
        this._updateNavigation()
    }
    
    this._removeNotification = function(position) {
        this.notifications.splice(position, 1)
        if (this.notifications.length == 0) {
            this.close()
            return
        }
        if (this.position == position) {
            if (this.position < this.notifications.length)
                this.setPosition(this.position)
            else
                this.setPosition(this.position - 1)
        }
        this._updateNavigation()
        this._idleUpdateDisplay()
    }
    
    this.shouldDisplayShare = function (share) {
        if ((share.post.CurrentViewers.length > 0 && share.post.CurrentViewers.item(0).Id == dh.selfId)
            || (share.post.Sender.id == dh.selfId && share.post.CurrentViewers.length == 0)) {
            dh.util.debug("Not displaying notification of self-view or initial post")
            return false;
        }
        return true
    }
    
    this._findNotification = function (id) {
        for (var i = 0; i < this.notifications.length; i++) {
            var notification = this.notifications[i]
            // there is no id defined for mySpace notifications
            // and we never reuse them, so this code will never find
            // mySpace notifications, change if needed             
            if (notification.data.getId() == id) {
                return {notification: notification, position: i}
            }
        }
        
        return null
    }
    
    // Show the page of the swarm bubble relevant to a particular reason
    this._showRelevantPage = function(why) {
        if ((why & dh.notification.MESSAGE) != 0)
            this._bubble.setPage("someoneSaid")
        else if ((why & dh.notification.VIEWER) != 0)
            this._bubble.setPage("whosThere")
    }

    // Returns true iff we should show the window if it's hidden. 
    this.addBubbleUpdate = function (bubbleData, updateOnly, why) {
        
        var prevBubbleData = this._findNotification(bubbleData.getId())
        if (prevBubbleData) {
            // you already have a notification for this particular activity in your
            // stack of bubbles, update the data
            prevBubbleData.notification.data = bubbleData
            prevBubbleData.notification.why |= why
            
            if (prevBubbleData.position == this.position) {
                // We're currently displaying this data, set it again in the bubble to force rerendering
                dh.util.debug("resetting current bubble data")
                this._bubble.setData(bubbleData)
                this._showRelevantPage(why)
            }
            
            // do more tests if ever have data with negative positions (signifying saved missed notifications)
            return !bubbleData.getIgnored()
        }
        
        if (bubbleData.getIgnored() || updateOnly)
            return false

        // We don't have it at all, or it was saved and needs to be redisplayed
        // bubble type is either 'linkShare' or 'groupUpdate'
        var displayed = this._pushNotification(bubbleData.getBubbleType(), bubbleData, bubbleData.getTimeout(), why)
            
        if (displayed)
            this._showRelevantPage(why)
        this._idleUpdateDisplay() // Handle changes to the navigation arrows
        return true
    }
    
    // Refresh the display of the share, if showing, otherwise do nothing
    this.updatePost = function(id) {
        var prevShareData = this._findNotification(id)
        if (!prevShareData) {
            dh.util.debug("did not find notification for post id " + id);
            return            
        } 
        dh.util.debug("found a notification for post id " + id);
        // Check to see if the last viewer has left the post; if so and
        // we were only showing this page because there were viewers
        // remove the page and possibly close the bubble
        if (prevShareData.notification.data.post.currentViewers.length == 0) {
            prevShareData.notification.why &= ~dh.notification.VIEWER
            if (prevShareData.notification.why == 0) {
                this._removeNotification(prevShareData.position)
                return
            }
        }
        
        if (prevShareData.position == this.position) {
            // set data again in the bubble to force rerendering
            this._bubble.setData(prevShareData.notification.data)
        }
    }
    
    this.addMySpaceComment = function (comment) {
        // If we already have a notification for the same comment, ignore this
        for (var i = 0; i < this.notifications.length; i++) {
            if (this.notifications[i].notificationType == 'mySpaceComment' &&
                this.notifications[i].data.commentId == comment.commentId) {
                return;
            }
        }
        
        // Otherwise, add a new notification page
        this._pushNotification('mySpaceComment', comment, 0)    
    }
    
    this._clearPageTimeout = function() {
        if (this._pageTimeoutId != null) {
            window.clearTimeout(this._pageTimeoutId)
            this._pageTimeoutId = null
        }
    }
    
    this._resetPageTimeout = function() {
        if (!this._idle) {
            this._clearPageTimeout();
            // Handle infinite timeout   
            if (this.position >= 0) {
                var timeout = this.notifications[this.position].timeout
                dh.util.debug("current page timeout is " + timeout)        
                if (timeout < 0) {
                    return;
                } else if (timeout == 0) {
                    timeout = this._defaultTimeout
                }
                var display = this;                
                this._pageTimeoutId = window.setTimeout(function() {
                    display.displayTimeout();
                    }, timeout * 1000);
            }
        }
    }

    this.setPosition = function(pos) {
        if (pos < 0) {
            dh.util.debug("negative position specified")
            return
        } else if (pos >= this.notifications.length) {
            dh.util.debug("position " + pos + " is too big")
            return
        }
            
        dh.util.debug("switching to position " + pos + " from " + this.position)
        var notification = this.notifications[pos]  
        this.position = pos
        this._bubble.setData(notification.data)
        this._showRelevantPage(notification.why)
        this._updateNavigation()
        this._resetPageTimeout()
        this._idleUpdateDisplay()
    }

    this._updateNavigation = function() {
        this._bubble.updateNavigation(this.position, this.notifications.length)
    }

    this.goPrevious = function () {
        this.setPosition(this.position - 1)
    }
    
    this.goNext = function () {
        this.setPosition(this.position + 1)
    }
    
    this.displayTimeout = function () {
        dh.util.debug("got display timeout")
        this._markCurrentAsMissed()
        this.goNextOrClose()
    }
    
    this.goNextOrClose = function () {
        dh.util.debug("doing goNextOrClose")
        if (this.position >= (this.notifications.length-1)) {
            this.close();
        } else {
            this.goNext();
        }
    }
    
    this.close = function () {
        dh.util.debug("bubble close invoked")
        this.setVisible(false)
        this._clearPageTimeout()
        window.external.application.Close()     
        this._initNotifications()
    }
    
    this.setIdle = function(idle) {
        dh.util.debug("Idle status is now " + idle + " at position " + this.position)
        this._idle = idle
        if (this._idle)
            this._clearPageTimeout()
        else if (this._visible)
            this._resetPageTimeout()
    }
    
    this.setVisible = function(visible) {
        this._visible = visible
    }
    
    this._markCurrentAsMissed = function () {
        var notification = this.notifications[this.position]
        if (notification && notification.state == "pending")
            notification.state = "missed"               
    }
    
    this._markCurrentAsSeen = function () {
        var notification = this.notifications[this.position]
        if (notification && notification.state == "pending")
            notification.state = "seen"            
    }

    this._markAsSeenGoNext = function () {
        this._markCurrentAsSeen()
        this.goNextOrClose();    
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

// Note if you change the parameters to these functions, you must change HippoBubble.cpp

dhAddLinkShare = function (post) {
    dh.display.setVisible(true)
    
    dh.util.debug("adding link share id=" + post.Id)
    var data = new dh.bubble.PostData(post)
    
    if (!dh.display.shouldDisplayShare(data))
        return false
        
    return dh.display.addBubbleUpdate(data, false, dh.notification.NEW)
}

dhPostActivity = function(post) {
    dh.display.setVisible(true)
    
    dh.util.debug("post activity id=" + post.Id)
    var data = new dh.bubble.PostData(post)
    return dh.display.addBubbleUpdate(data, false, dh.notification.ACTIVITY)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhGroupViewerJoined = function(entity, updateOnly) {
    dh.display.setVisible(true)
    
    var data = new dh.bubble.GroupChatData(entity)
    return dh.display.addBubbleUpdate(data, updateOnly, dh.notification.VIEWER)
}

dhGroupChatRoomMessage = function(entity, updateOnly) {
    dh.display.setVisible(true)
        
    var data = new dh.bubble.GroupChatData(entity)
    return dh.display.addBubbleUpdate(data, updateOnly, dh.notification.MESSAGE)
}

dhGroupMembershipChanged = function(group, user, status) {
    dh.display.setVisible(true)
              
    var data = new dh.bubble.GroupMembershipChangeData(group, user, status)
    return dh.display.addBubbleUpdate(data, false, dh.notification.NEW)
}

dhViewerJoined = function(post, updateOnly) {
    dh.display.setVisible(true)
    
    var data = new dh.bubble.PostData(post)
    return dh.display.addBubbleUpdate(data, updateOnly, dh.notification.VIEWER)
}

dhChatRoomMessage = function(post, updateOnly) {
    dh.display.setVisible(true)
        
    var data = new dh.bubble.PostData(post)
    return dh.display.addBubbleUpdate(data, updateOnly, dh.notification.MESSAGE)
}

dhUpdatePost = function(post) {
    dh.display.updatePost(post.Id)
}

dhSetIdle = function(idle) {
    dh.display.setIdle(idle)
}
