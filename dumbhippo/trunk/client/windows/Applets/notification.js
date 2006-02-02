// Notification implementation

dh.notification = {}
dh.notification.extension = {}

dh.display = null;
dh.notification.extensions = {}

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl, selfId) {
    dh.util.debug("invoking dhInit")
    dh.display = new dh.notification.Display(serverUrl, appletUrl, selfId); 
}

dh.notification.Display = function (serverUrl, appletUrl, selfId) {
    // Whether the user is currently using the computer
    this._idle = false
    
    // Current user guid
    this.selfId = selfId
    
    // Whether the bubble is showing
    this._visible = false
    
    // Whether we're reserving space for viewer information 
    // (should be true or false, we reserve null for "uninitialized")
    this._showViewers = null
    
    // personId -> name
    this._nameCache = {}
    
    this.serverUrl = serverUrl
    this.appletUrl = appletUrl
    
    this._pageTimeoutId = null
    
    // postid -> notification
    this.savedNotifications = {}

    this.addPersonName = function (personId, name) {
        this._nameCache[personId] = name
    }
    
    this.getPersonName = function (personId) {
        var ret = this._nameCache[personId]
        if (!ret)
            ret = "(unknown)"
        return ret;
    }
    
    this._initNotifications = function() {
        this.notifications = []
        this.position = -1
    }
    
    this._initNotifications() // And do it initially
    
    // Initialize close button
    var display = this;
    this.closeButton = document.getElementById("dh-close-button")
    this.closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
            e.stopPropagation();
            dh.util.debug("doing close, position=" + display.position)            
            display._markCurrentAsSeen();
            display.close();
            return false;
    })    
    
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
    this.addLinkShare = function (share, timeout) {
        var prevShareData = this._findLinkShare(share.postId)
        var shouldDisplayShare = this._shouldDisplayShare(share)
        if (prevShareData) {
            // Update the viewer data
            prevShareData.notification.data.viewers = share.viewers        
        }
        if (!prevShareData || (prevShareData.position < 0 && shouldDisplayShare)) {   
            // We don't have it at all, or it was saved and needs to be redisplayed
            this._pushNotification('linkShare', share, timeout)
            return true
        } else if (prevShareData && prevShareData.position == this.position) {
            // We're currently displaying this share, rerender
            this._display_linkShare(share)
            return false
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

    this._setShowViewers = function(showViewers) {
        showViewers = !!showViewers
        if (this._showViewers != showViewers) {
            this._showViewers = showViewers
            
            var viewersDiv = document.getElementById("dh-notification-viewers-outer")
            var space
            
            if (showViewers) {
                viewersDiv.style.display = "block"
                space = viewersDiv.offsetHeight
            } else {
                viewersDiv.style.display = "none"
                space = 0
            }
            
            dh.util.debug("showing viewers area: " + showViewers + ", reserving " + space + " additional pixels of height")
            
            window.external.application.SetViewerSpace(space)
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
        dh.util.debug("notification type " + notification.notificationType)
        switch (notification.notificationType) {
            case 'linkShare':
                this._display_linkShare(notification.data)
                break
            case 'mySpaceComment':
                this._display_mySpaceComment(notification.data)
                break
            default:
                dh.util.debug("unknown notfication type " + notification.notificationType)
        }
        //var handler = this["_display_" + notification.notificationType]
        //dh.util.debug("notification handler: " + handler)     
        //handler(this, notification.data)
        this._updateNavigation()
        this._resetPageTimeout()
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
            if (notification.state == "pending") // shouldn't happen
                notification.state = "missed"
            // be sure we've saved it, this is a noop for already saved
            this.savedNotifications[notification.data.postId] = notification
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
        if (notification.state == "pending")
            notification.state = "missed"               
    }
    
    this._markCurrentAsSeen = function () {
        var notification = this.notifications[this.position]
        if (notification.state == "pending")
            notification.state = "seen"            
    }

    this._updateNavigation = function () {
        var navText = dh.util.dom.getClearedElementById("dh-notification-navigation-text")
        navText.appendChild(document.createTextNode((this.position+1) + " of " + this.notifications.length))
        
        var navButtons = dh.util.dom.getClearedElementById("dh-notification-navigation-buttons")
        var img = dh.util.dom.createHrefImg(this.appletUrl + "activeLeft.png", "")
        img.firstChild.setAttribute("className", "dh-notification-navigation")
        navButtons.appendChild(img)
        var display = this
        img.onclick = dh.util.dom.stdEventHandler(function (e) {      
            display.goPrevious();
            return false;
        })
        img = dh.util.dom.createHrefImg(this.appletUrl + "activeRight.png", "")
        img.firstChild.setAttribute("className", "dh-notification-navigation")      
        navButtons.appendChild(img)
        img.onclick = dh.util.dom.stdEventHandler(function (e) {
            display._markCurrentAsSeen()        
            display.goNext();
            return false;
        })
    }
    
    this._getExternalAnchor = function (href) {
        var a = document.createElement("a")
        a.setAttribute("href", "")
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            window.external.application.OpenExternalURL(href)
            return false;
        })
        return a    
    }
    
    this._setPhotoUrl = function (src, url) {
        var imgDiv = dh.util.dom.getClearedElementById("dh-notification-photo")
        var a = this._getExternalAnchor(url) 
        a.setAttribute("className", "dh-notification-photo")
        var img = document.createElement("div")
        img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale')"
        img.setAttribute("className", "dh-notification-photo")
        a.appendChild(img)  
        imgDiv.appendChild(a)   
        dh.util.debug("Src = " + src)
    }
    
    this._setPhotoLink = function (text, url) {
        var photoLinkDiv = dh.util.dom.getClearedElementById("dh-notification-photolink")
        var a = this._getExternalAnchor(url)
        a.setAttribute("className", "dh-notification-photolink")
        a.appendChild(document.createTextNode(text))
        photoLinkDiv.appendChild(a)
    }
    
    this._createSharedLinkLink = function(linkTitle, postId, url, hookfn) {
        var a = document.createElement("a")
        a.setAttribute("href", "javascript:true")
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            window.external.application.DisplaySharedLink(postId, url)
            if (hookfn)
                hookfn()
            return false;
        })
        dh.util.dom.appendSpanText(a, linkTitle, "dh-notification-link-title")
        return a
    }

    // This function adjusts various sizes that we can't make the CSS handle
    this._fixupLayout = function() {
    
        // First make the title ellipsize before it runs over the close button
        var rightsideDiv = document.getElementById("dh-notification-rightside")
        var titleDiv = document.getElementById("dh-notification-title")
        var closeButton = document.getElementById("dh-close-button")
        
        titleDiv.style.width = (rightsideDiv.clientWidth - closeButton.offsetWidth) + "px"
        
        // Now set the height of the body element to be fixed to the remaining space
        var bodyElement = document.body
        var bottomrightDiv = document.getElementById("dh-notification-bottomright")
        var desiredHeight = bodyElement.clientHeight - titleDiv.offsetHeight - bottomrightDiv.offsetHeight
        
        // Hack - we don't want partial lines to be shown, so compute how many 
        // full lines fit. We do this by knowing that titleDiv is one line high
        // this will break if the title is changed to a different font, etc.
        var lineHeight = titleDiv.clientHeight
        if (lineHeight > 0)
            desiredHeight = Math.floor(desiredHeight / lineHeight) * lineHeight

        var bodyDiv = document.getElementById("dh-notification-body")
        bodyDiv.style.height = desiredHeight + "px"
    }
     
    this.renderRecipient = function (recipient, normalCssClass, selfCssClass) {
        var id = recipient.id
        var name = recipient.name
        dh.util.debug("rendering recipient with name=" + name)
        var cssClass = normalCssClass;
        if (id == this.selfId) {
            name = "you"
            cssClass = selfCssClass
        }
        var node = document.createElement("span")
        node.setAttribute("className", cssClass)
        node.appendChild(document.createTextNode(name))       
        return node
    }
     
    this.renderRecipients = function (node, arr, normalCssClass, selfCssClass) {
        for (var i = 0; i < arr.length; i++) {
            var recipientNode = this.renderRecipient(arr[i], normalCssClass, selfCssClass)
            node.appendChild(recipientNode)
            if (i < arr.length - 1) {
                node.appendChild(document.createTextNode(", "))
            }
        }  
    }
    
    this.markAsSeenGoNext = function () {
        this._markCurrentAsSeen()
        this.goNextOrClose();    
    }
  
    // Returns true iff we should display this share
    this._display_linkShare = function (share) {
        dh.util.debug("displaying " + share.postId + " " + share.linkTitle)

        var personUrl = this.serverUrl + "person?who=" + share["senderId"]
        this._setPhotoUrl(this.serverUrl + share.senderPhotoUrl, personUrl)
                      
        this._setPhotoLink(this.getPersonName(share.senderId), personUrl)
        
        var display = this;
        var titleDiv = dh.util.dom.getClearedElementById("dh-notification-title")
        titleDiv.appendChild(this._createSharedLinkLink(share.linkTitle, share.postId, share.linkURL, function () { display.markAsSeenGoNext() }))

        var bodyDiv = dh.util.dom.getClearedElementById("dh-notification-body")
        bodyDiv.appendChild(document.createTextNode(share.linkDescription))
        
        for (extension in dh.notification.extensions) {
            var ext = dh.notification.extensions[extension]
            dh.util.debug("using notification extension: " + extension + " (" + ext + ")")
            if (ext.accept(share)) {
                dh.util.debug("drawing content for " + extension)
                ext.drawContent(share, bodyDiv)
            }
        }

        var metaDiv = dh.util.dom.getClearedElementById("dh-notification-meta")
        metaDiv.appendChild(document.createTextNode("This was sent to "))
        var personRecipients = share.personRecipients
        var groupRecipients = share.groupRecipients
        // FIXME this is all hostile to i18n
        this.renderRecipients(metaDiv, personRecipients, "dh-notification-recipient", "dh-notification-self-recipient")
        if (personRecipients.length > 0 && groupRecipients.length > 0) {
            metaDiv.appendChild(document.createTextNode(" and "))
        }
        if (groupRecipients.length > 1) {
            metaDiv.appendChild(document.createTextNode("the groups "))            
            dh.util.dom.joinSpannedText(metaDiv, groupRecipients, "dh-notification-group-recipient", ", ")
        } else if (groupRecipients.length == 1) {
            metaDiv.appendChild(document.createTextNode("the "))
            dh.util.dom.appendSpanText(metaDiv, groupRecipients[0], "dh-notification-group-recipient")
            metaDiv.appendChild(document.createTextNode(" group"))
        }
        
        var viewers = share.viewers
        if (viewers.length > 0) {
            var viewersDiv = dh.util.dom.getClearedElementById("dh-notification-viewers")
            dh.util.dom.appendSpanText(viewersDiv, "Viewed by: ", "dh-notification-viewers-label")
            this.renderRecipients(viewersDiv, viewers, "dh-notification-viewer", "dh-notification-self-viewer")            

            // Need to pass in the viewer ID as well as name to here to display
            var viewersPhotoDiv = document.getElementById("dh-notification-viewers-photo")
            viewersPhotoDiv.style.display = "None"

            this._setShowViewers(true)
        } else {
            this._setShowViewers(false)
        }
        
        this._fixupLayout()
    }
    
    this._display_mySpaceComment = function (comment) {
        dh.util.debug("displaying blog " + comment.blogId + " comment " + comment.commentId)

        var display = this;
        var titleDiv = dh.util.dom.getClearedElementById("dh-notification-title")
        var a = this._getExternalAnchor("http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=" + comment.myId + "&blogID=" + comment.blogId)
        a.appendChild(document.createTextNode("New MySpace comment from " + comment.posterName))
        titleDiv.appendChild(a)
        
        var personUrl = "http://myspace.com/" + comment.posterId
        dh.util.debug("using myspace photo url: " + comment.posterImgUrl)
        this._setPhotoUrl(comment.posterImgUrl, personUrl)
        this._setPhotoLink(comment.posterName, personUrl)        
  
        var bodyDiv = dh.util.dom.getClearedElementById("dh-notification-body")
        // bodyDiv.innerHTML = comment.content
        bodyDiv.appendChild(document.createTextNode(comment.content))
  
        var metaDiv = dh.util.dom.getClearedElementById("dh-notification-meta")
        this._setShowViewers(false)
        
        this._fixupLayout()    
    }
}

dhAdaptLinkRecipients = function (recipients) {
    dh.util.debug("adapting array")
    var ret = dh.core.adaptExternalArray(recipients)
    for (var i = 0; i < ret.length; i++) {
        dh.util.debug("adapting " + ret[i])
        var recipient = dh.core.adaptExternalArray(ret[i])
        ret[i] = {id: recipient[0], name: recipient[1] }
    }
    return ret;
}

// Global namespace since it's painful to do anything else from C++
// Note if you change the parameters to this function, you must change
// HippoBubble.cpp
dhAddLinkShare = function (senderName, senderId, senderPhotoUrl, postId, linkTitle, 
                           linkURL, linkDescription, personRecipients, groupRecipients, viewers, postInfo, timeout) {
    dh.util.debug("in dhAddLinkShare, senderName: " + senderName)  
    dh.display.setVisible(true)
    dh.display.addPersonName(senderId, senderName)
    personRecipients = dhAdaptLinkRecipients(personRecipients)
    groupRecipients = dh.core.adaptExternalArray(groupRecipients)
    viewers = dhAdaptLinkRecipients(viewers)
    return dh.display.addLinkShare({senderId: senderId,
                             senderPhotoUrl: senderPhotoUrl,
                             postId: postId,
                            linkTitle: linkTitle,
                            linkURL: linkURL,
                            linkDescription: linkDescription,
                            personRecipients: personRecipients,
                            groupRecipients: groupRecipients,
                            viewers: viewers,
                            info: dh.parseXML(postInfo)}, timeout)
}

dhAddMySpaceComment = function (myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    dh.util.debug("in dhAddMySpaceComment, blogId: " + blogId + " commentId: " + commentId + " posterId: " + posterId + " content: " + content)
    try {
    dh.display.addMySpaceComment({myId: myId,
                                  blogId: blogId,
                                  commentId: commentId, 
                                  posterId: posterId, 
                                  posterName: posterName,
                                  posterImgUrl: posterImgUrl,
                                  content:  content})
    } catch (e) {
        dh.util.debug("addMySpaceComment failed: " + e.message)
    }
    dh.util.debug("current position: " + dh.display.position)
}

dhDisplayMissed = function () {
    dh.util.debug("in dhDisplayMissed")
    dh.display.displayMissed()
}

dhSetIdle = function(idle) {
    dh.display.setIdle(idle)
}