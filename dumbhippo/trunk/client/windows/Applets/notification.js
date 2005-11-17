// Notification implemefntation

dh.notification = {}
dh.notification.extension = {}

dh.display = null;
dh.notification.extensions = {}

// Global function called immediately after document.write
var dhInit = function(serverUrl, appletUrl) {
    var closeButton = document.getElementById("dh-close-button")
    closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
            e.stopPropagation();
            window.external.Close();
            return false;
    })
    dh.display = new dh.notification.Display(serverUrl, appletUrl); 
}

dh.notification.Display = function (serverUrl, appletUrl) {
    // Whether the user is currently using the computer
    this._idle = false
    
    // Whether the bubble is showing
    this._visible = false
    
    // personId -> name
    this._nameCache = {}
    
    this.serverUrl = serverUrl
    this.appletUrl = appletUrl
    
    this._pageTimeoutId = null

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
       
        // postId -> int
        this.swarmPositions = {}
        // postId -> personId
        // Used to determine which photo to display
        this.lastSwarmer = {}
        // postId -> {person1: true, person2: true, ...}    
        this.swarmers = {}
    }
    
    this._initNotifications() // And do it initially
    
    this._pushNotification = function (nType, data) {
        this.notifications.push({notificationType: nType,
                                 data: data})
        dh.util.debug("position " + this.position + " notifications: " + this.notifications)                                 
        if (this.position < 0) {
            this.setPosition(0)
        }
        this._updateNavigation()        
    }
    
    this.addLinkShare = function (share) {
        this._pushNotification('linkShare', share)
    }
    
    this.addSwarmNotice = function (swarm) {
        this.lastSwarmer[swarm.postId]  = swarm.swarmerId
        if (this.swarmPositions[swarm.postId] == undefined) {
            dh.util.debug('no existing swarm for " + swarm.postId + ", creating new')       
            this.swarmers[swarm.postId] = {}
            this.swarmers[swarm.postId][swarm.swarmerId] = true         
            this._pushNotification('swarm', swarm)
            this.swarmPositions[swarm.postId] = (this.notifications.length - 1)
        } else {
            dh.util.debug("found existing swarm for " + swarm.postId + ", redrawing")
            this.swarmers[swarm.postId][swarm.swarmerId] = true
            this.setPosition(this.position) // redraws (should we set position to the swarm position?)
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
            this._pageTimeoutId = window.setTimeout(
                function() {
                    dh.display.goNextOrClose()
                }, 7 * 1000); // 7 seconds
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
        if (notification.notificationType == 'linkShare')
            this._display_linkShare(notification.data)
        else if (notification.notificationType == 'swarm')
            this._display_swarm(notification.data)
        else
            dh.util.debug("unknown notfication type " + notification.notificationType)
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
    
    this.goNextOrClose = function () {
        if (this.position >= (this.notifications.length-1)) {
            this.close();
        } else {
            this.goNext();
        }
    }
    
    this.close = function () {
        this.setVisible(false)
        this._clearPageTimeout()
        window.external.Close()
        this._initNotifications()
    }
    
    this.setIdle = function(idle) {
        dh.util.debug("Idle status is now " + idle)
        this._idle = idle
        if (this._idle)
            this._clearPageTimeout()
        else if (this._visible)
            this._resetPageTimeout()
    }
    
    this.setVisible = function(visible) {
        this._visible = visible
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
            display.goNext();
            return false;
        })
    }
    
    this._getExternalAnchor = function (href) {
        var a = document.createElement("a")
        a.setAttribute("href", "")
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            window.external.OpenExternalURL(href)
            return false;
        })
        return a    
    }
    
    this._setPhotoUrl = function (src, url) {
        var imgDiv = dh.util.dom.getClearedElementById("dh-notification-photo")
        var a = this._getExternalAnchor(url) 
        a.setAttribute("className", "dh-notification-photo")        
        var img = document.createElement("img")
        img.setAttribute("src", src)
        img.setAttribute("className", "dh-notification-photo")
        a.appendChild(img)  
        imgDiv.appendChild(a)   
    }
    
    this._setPhotoLink = function (text, url) {
        var photoLinkDiv = dh.util.dom.getClearedElementById("dh-notification-photolink")
        var a = this._getExternalAnchor(url)
        a.setAttribute("className", "dh-notification-photolink")
        a.appendChild(document.createTextNode(text))
        photoLinkDiv.appendChild(a)
    }
    
    this._createSharedLinkLink = function(linkTitle, postId, hookfn) {
        var a = document.createElement("a")
        a.setAttribute("href", "javascript:true")
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            window.external.DisplaySharedLink(postId)
            if (hookfn)
                hookfn()
            return false;
        })
        dh.util.dom.appendSpanText(a, linkTitle, "dh-notification-link-title")
        return a
    }
    
    this._display_linkShare = function (share) {
        dh.util.debug("displaying " + share.postId + " " + share.linkTitle)

        var personUrl = this.serverUrl + "viewperson?personId=" + share["senderId"]
        this._setPhotoUrl(this.serverUrl + "files/headshots/" + share["senderId"],
                          personUrl)
                      
        this._setPhotoLink(this.getPersonName(share.senderId), personUrl)
        
        var display = this;
        var hook = function () {
            display.goNextOrClose();
        }

        var titleDiv = dh.util.dom.getClearedElementById("dh-notification-title")
        titleDiv.appendChild(this._createSharedLinkLink(share.linkTitle, share.postId, hook))

        var bodyDiv = dh.util.dom.getClearedElementById("dh-notification-body")
        bodyDiv.appendChild(document.createTextNode(share.linkDescription))
        
        for (extension in dh.notification.extensions) {
            var ext = dh.notification.extensions[extension]
            dh.util.debug("got extension: " + extension + " (" + ext + ")")
            if (ext.accept(share)) {
                dh.util.debug("drawing content for " + extension)
                ext.drawContent(share, bodyDiv)
            }
        }

        var metaDiv = dh.util.dom.getClearedElementById("dh-notification-meta")
        metaDiv.appendChild(document.createTextNode("This was sent to "))
        var personRecipients = dh.core.adaptExternalArray(share["personRecipients"])
        var groupRecipients = dh.core.adaptExternalArray(share["groupRecipients"])  
        // FIXME this is all hostile to i18n
        dh.util.dom.joinSpannedText(metaDiv, personRecipients, "dh-notification-recipient", ", ")
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
    }
    
    this._display_swarm = function (swarm) {
        dh.util.debug("displaying swarm for " + swarm.postId)
        
        var lastSwarmer = this.lastSwarmer[swarm.postId]
        dh.util.debug("last swarmer is " + lastSwarmer)

        var personUrl = this.serverUrl + "viewperson?personId=" + lastSwarmer
        this._setPhotoUrl(this.serverUrl + "files/headshots/" + lastSwarmer,
                          personUrl)
                      
        var name = this.getPersonName(lastSwarmer)
        this._setPhotoLink(name, lastSwarmer)

        var titleDiv = dh.util.dom.getClearedElementById("dh-notification-title")
        titleDiv.appendChild(document.createTextNode("Swarm for "));
        titleDiv.appendChild(this._createSharedLinkLink(swarm.postTitle, swarm.postId, null))

        var bodyDiv = dh.util.dom.getClearedElementById("dh-notification-body")
        for (personId in this.swarmers[swarm.postId]) {
            dh.util.dom.appendSpanText(bodyDiv, this.getPersonName(personId), "dh-notification-swarmer")
            bodyDiv.appendChild(document.createTextNode(", "))
        }

        var metaDiv = dh.util.dom.getClearedElementById("dh-notification-meta")
        metaDiv.appendChild(document.createTextNode("And 2 trillion other people")) 
    }   
}

// Global namespace since it's painful to do anything else from C++
dhAddLinkShare = function (senderName, senderId, postId, linkTitle, 
                           linkURL, linkDescription, personRecipients, groupRecipients) {
    dh.util.debug("in dhAddLinkShare, senderName: " + senderName)
    dh.display.setVisible(true)
    dh.display.addPersonName(senderId, senderName)                            
    dh.display.addLinkShare({senderId: senderId,
                             postId: postId,
                            linkTitle: linkTitle,
                            linkURL: linkURL,
                            linkDescription: linkDescription,
                            personRecipients: personRecipients,
                            groupRecipients: groupRecipients})
}

dhAddSwarmNotice = function (postId, swarmerId, postTitle, swarmerName) {
    dh.display.setVisible(true)
    dh.util.debug("in dhAddSwarmNotice, postId: " + postId)
    dh.display.addPersonName(swarmerId, swarmerName)
    dh.display.addSwarmNotice({postId: postId,
                               swarmerId: swarmerId,
                               postTitle: postTitle})
}

dhSetIdle = function(idle) {
    dh.display.setIdle(idle)
}