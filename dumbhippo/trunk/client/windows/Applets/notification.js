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

dh.notification.Person = function (id, name, smallPhotoUrl) {
    this.id = id
    this.name = name
    this.smallPhotoUrl = smallPhotoUrl
}

dh.notification.Resource = function (id, name) {
    this.id = id
    this.name = name
}

dh.notification.Group = function (id, name, smallPhotoUrl) {
    this.id = id
    this.name = name
    this.smallPhotoUrl = smallPhotoUrl
}

// Global hash of id -> entity  (a Person, Resource, or Group)
dh.notification._entities = {};

dh.notification.findEntity = function (id) {
    return dh.notification._entities[id]
}

dh.notification.Display = function (serverUrl, appletUrl, selfId) {
    // Whether the user is currently using the computer
    this._idle = false
    
    // Whether the bubble is showing
    this._visible = false
    
    this._pageTimeoutId = null
    
    // postid -> notification
    this.savedNotifications = {}

    this._initNotifications = function() {
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
        display.goNext();
    }
    this._bubble.onLinkClick = function(postId, url) {
        window.external.application.DisplaySharedLink(postId, url)
        display._markAsSeenGoNext()
    }
    
    document.body.appendChild(this._bubble.create())
    
    this._bubble.onSizeChange = function() {
        window.external.application.Resize(display._bubble.getWidth(), display._bubble.getHeight())
    }
    this._bubble.onSizeChange()
    
    this._pushNotification = function (nType, data, timeout) {
        this.notifications.push({notificationType: nType,
                                 state: "pending",
                                 data: data,
                                 timeout: timeout})
        dh.util.debug("position " + this.position + " notifications: " + this.notifications)                                 
        if (this.position < 0) {
            this.setPosition(0)
        }
        this._updateNavigation()
    }
    
    this._shouldDisplayShare = function (share) {
        if (share.viewers.length > 0 && share.viewers[0] == dh.selfId) {
            dh.util.debug("Not displaying notification of self-view")
            return false;
        }
        // We only display bubbles if the viewers are at a certain
        // threshold: http://devel.dumbhippo.com/wiki/Bubble_Behavior#States
        if (share.viewers.length > 127) {
            dh.util.debug("> 127 viewers, not displaying")
            return false
        }
        if (share.viewers.length >= 3 &&
            share.viewers.length <= 5) {
            dh.util.debug("between 3 and 5 viewers inclusive, displaying share")                     
            return true
        }
        // Compute log_2 of viewers
        var logViewerLen = Math.LOG2E * Math.log(share.viewers.length)
        if (share.viewers.length > 5 && Math.ceil(logViewerLen) == Math.floor(logViewerLen)) {
            dh.util.debug("viewers is a power of 2, displaying share")
            return true
        }
        dh.util.debug("not displaying share")                
        return false    
    }
    
    this._findLinkShare = function (postId) {
        for (var i = 0; i < this.notifications.length; i++) {
            var notification = this.notifications[i] 
            if (notification.notificationType == 'linkShare' &&
                notification.data.postId == postId) {
                return {notification: notification, position: i}
            }
        }
        if (this.savedNotifications[postId]) {
            return {notification: this.savedNotifications[postId], position: -1}
        }
        return null
    }

    // Returns true iff we should show the window if it's hidden
    this.addLinkShare = function (share, timeout, isRedisplay) {
        var prevShareData = this._findLinkShare(share.postId)
        var shouldDisplayShare = isRedisplay || this._shouldDisplayShare(share)
        if (prevShareData) {
            // Update the viewer data
            prevShareData.notification.data.viewers = share.viewers
        }
        if (!prevShareData || (prevShareData.position < 0 && shouldDisplayShare)) {   
            // We don't have it at all, or it was saved and needs to be redisplayed
            this._pushNotification('linkShare', share, timeout)
            return true
        } else if (prevShareData && prevShareData.position == this.position) {
            // We're currently displaying this share, set it again in the bubble to force rerendering
            dh.util.debug("rerendering bubble")
            this._bubble.setData(share)
            return true
        } else {
            dh.util.debug("not rerendering bubble");
        }
        return false
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
    
    this.displayMissed = function () {    
        for (postid in this.savedNotifications) {
            var notification = this.savedNotifications[postid]
            if (notification.state == "missed") {
                this._pushNotification(notification.notificationType, notification.data)
            }
        }    
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
                    timeout = 7 // default timeout
                }
                var display = this;                
                this._pageTimeoutId = window.setTimeout(function() {
                    display.displayTimeout();
                    }, timeout * 1000); // 7 seconds
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
        this._updateNavigation()
        this._resetPageTimeout()
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
    
    this.notifyMissedChanged = function () {
        var haveMissed = false;
        for (postid in this.savedNotifications) {
            var notification = this.savedNotifications[postid]
            if (notification.state == "missed") {
                haveMissed = true;
            }
        }
        window.external.application.SetHaveMissedBubbles(haveMissed)
    }
    
    this.close = function () {
        dh.util.debug("bubble close invoked")
        this.setVisible(false)
        this._clearPageTimeout()
        window.external.application.Close()     
        var curDate = new Date()
        for (var i = 0; i < this.notifications.length; i++) {
            var notification = this.notifications[i]
            notification.saveDate = curDate
            dh.util.debug("saving notification " + notification)
            if (notification.state == "pending") // shouldn't happen
                notification.state = "missed"
            // be sure we've saved it, this is a noop for already saved
            this.savedNotifications[notification.data.postId] = notification
            dh.util.debug("done saving notification " + notification)            
        }
        this.notifyMissedChanged()      
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

dhAddPerson = function (id, name, smallPhotoUrl) 
{
    dh.util.debug("adding person " + id + " " + name + " " + smallPhotoUrl)
    var person = new dh.notification.Person(id, name, smallPhotoUrl)
    dh.notification._entities[id] = person
}

dhAddResource = function (id, name) 
{
    dh.util.debug("adding resource " + id + " " + name)
    var res = new dh.notification.Resource(id, name)
    dh.notification._entities[id] = res
}

dhAddGroup = function (id, name, smallPhotoUrl) 
{
    dh.util.debug("adding group " + id + " " + name + " " + smallPhotoUrl)
    var grp = new dh.notification.Group(id, name, smallPhotoUrl)
    dh.notification._entities[id] = grp
}

// Note if you change the parameters to this function, you must change
// HippoBubble.cpp
dhAddLinkShare = function (isRedisplay, senderId, postId, linkTitle, 
                           linkURL, linkDescription, recipients, 
                           viewers, postInfo, timeout, viewerHasViewed) {
    dh.display.setVisible(true)
    viewers = dh.core.adaptExternalArray(viewers)
    recipients = dh.core.adaptExternalArray(recipients)
    
    dh.util.debug("adding link share id=" + postId + " " + viewers.length + " viewers")
    var data = new dh.bubble.PostData(senderId, postId, linkTitle, 
                                      linkURL, linkDescription, recipients, 
                                      viewers, postInfo, viewerHasViewed)
    return dh.display.addLinkShare(data, timeout, isRedisplay)                   
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    var data = new dh.bubble.MySpaceData(myId, blogId, commentId, posterId, posterName, posterImgUrl, content)
    dh.display.addMySpaceComment(data)
}

dhDisplayMissed = function () {
    dh.display.displayMissed()
}

dhSetIdle = function(idle) {
    dh.display.setIdle(idle)
}