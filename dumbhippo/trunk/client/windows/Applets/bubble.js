// bubble.js: Shared handling of bubbles that display notifications to the
//   user of posts, MySpace comments ,and so forth
// Copyright Red Hat, Inc. 2006

dh.bubble = {} 

//////////////////////////////////////////////////////////////////////////////
// Keep track about information about entities such as Persons, Groups, etc.
//////////////////////////////////////////////////////////////////////////////

dh.bubble.Person = function(id, name, smallPhotoUrl) {
    this.id = id
    this.name = name
    this.smallPhotoUrl = smallPhotoUrl
}

dh.bubble.Resource = function(id, name) {
    this.id = id
    this.name = name
}

dh.bubble.Group = function(id, name, smallPhotoUrl) {
    this.id = id
    this.name = name
    this.smallPhotoUrl = smallPhotoUrl
}

// Global hash of id -> entity  (a Person, Resource, or Group)
dh.bubble._entities = {};

dh.bubble.findEntity = function(id) {
    return dh.bubble._entities[id]
}

dh.bubble.addEntity = function(entity) {
    dh.bubble._entities[entity.id] = entity;
}

//////////////////////////////////////////////////////////////////////////////
// Generic display code for a single notification bubble
//////////////////////////////////////////////////////////////////////////////
    
dh.bubble.BASE_WIDTH = 399
dh.bubble.NAVIGATION_WIDTH = 51
dh.bubble.BASE_HEIGHT = 123
dh.bubble.SWARM_HEIGHT = 180
    
// Create a new bubble object    
// @param includeNavigation whether the navigation arrows and close button 
//        should be included in the result
dh.bubble.Bubble = function(includeNavigation) {
    // Whether to include the quit button and previous/next arrows
    this._includeNavigation = includeNavigation
    
    // The notification currently being displayed
    this._data = null
    
    ///////////// Callbacks /////////////
    
    // After creating the Bubble object, the caller can assign to these fields
    // to override the default empty handling with real callbacks
    
    // Called when the close button is clicked
    this.onClose = function() {}
    
    // Called when the 'next' arrow is clicked
    this.onNext = function() {}
    
    // Called when the 'previous' arrow is clicked
    this.onPrevious = function() {}
    
    // Called when the user clicks on a DumbHippo post link
    // @param postId the ID of the post that was clicked on
    // @param url the URL of the link that was shared with the post (this is not
    //      the URL we take the user to; we redirect them through our
    //      site instead.)
    this.onLinkClick = function(postId, url) {}
    
    // Called when the size of the bubble changes
    this.onSizeChange = function() {}
    
    // Build the DOM tree for the bubble
    // @return the DOM node of the top node for the bubble
    this.create = function() {
        function append(parent, elementName, className) {
            var element = document.createElement(elementName)
            element.setAttribute("className", className)
            parent.appendChild(element)
            
            return element
        }
        function appendDiv(parent, className) {
            return append(parent, "div", className)
        }
    
        var bubble = this  // for callback closures

        this._topDiv = document.createElement("div")
        this._topDiv.setAttribute("className", "dh-notification-top")
        
        if (this._includeNavigation) {
            var navDiv = appendDiv(this._topDiv, "dh-notification-navigation")
            this._navText = appendDiv(navDiv, "dh-notification-navigation-text")
            this._navButtons = appendDiv(navDiv, "dh-notification-navigation-buttons")
        }

        this._leftImg = dh.util.createPngElement(dh.appletUrl + "bubbleLeft.png", 33, dh.bubble.BASE_HEIGHT)
        this._leftImg.className = "dh-left-img"
        this._leftImg.zIndex = 1         
        this._rightImg = dh.util.createPngElement(dh.appletUrl + "bubbleRight.png", 24, dh.bubble.BASE_HEIGHT)
        this._rightImg.className = "dh-right-img"
        this._rightImg.zIndex = 1         
        this._leftImgSwarm = dh.util.createPngElement(dh.appletUrl + "bubbleLeftSwarm.png", 33, dh.bubble.SWARM_HEIGHT)
        this._leftImgSwarm.className = "dh-left-img"
        this._leftImgSwarm.zIndex = 1            
        this._rightImgSwarm = dh.util.createPngElement(dh.appletUrl + "bubbleRightSwarm.png", 27, dh.bubble.SWARM_HEIGHT)
        this._rightImgSwarm.className = "dh-right-img"              
        this._rightImgSwarm.zIndex = 1
        
        var leftSide = appendDiv(this._topDiv, "dh-notification-leftside")
        this._leftImgSpan = append(this._topDiv, "span") 
        this._photoDiv = appendDiv(leftSide, "dh-notification-photo-div")
        this._photoLinkDiv = appendDiv(leftSide, "dh-notification-photolink")
        this._rightsideDiv = appendDiv(this._topDiv, "dh-notification-rightside")
        this._titleDiv = appendDiv(this._rightsideDiv, "dh-notification-title")
        this._bodyDiv = appendDiv(this._rightsideDiv, "dh-notification-body")
        this._rightImgSpan = append(this._topDiv, "span")       
        this._bottomrightDiv = appendDiv(this._rightsideDiv, "dh-notification-bottomright")
        var metaOuterDiv = appendDiv(this._bottomrightDiv, "dh-notification-viewers-outer")
        this._metaSpan = append(metaOuterDiv, "span", "dh-notification-meta")
        
        this._swarmNavDiv = appendDiv(this._topDiv, "dh-notification-swarm-nav")
        // Until we implement more of the mockup
        this._swarmNavDiv.appendChild(document.createTextNode("seen by:"))
        this._swarmDiv = appendDiv(this._topDiv, "dh-notification-swarm")
        
        if (this._includeNavigation) {
            this._closeButton = append(this._topDiv, "img", "dh-close-button")
            this._closeButton.setAttribute("src", dh.appletUrl + "close.png")
            this._closeButton.onclick = dh.util.dom.stdEventHandler(function (e) {
                bubble.onClose();
                e.stopPropagation();
                return false;
            })
        }
        
        return this._topDiv
    }
    
    // Set the notification that this bubble is currently displaying. 
    // @param data object, such as a dh.bubble.PostData or dh.bubble.MySpaceData
    //    While there isn't actually a base class, all data objects must export
    //    a set of methods forming a common interface.
    this.setData = function(data) {
        this._data = data
        this._render()
    }
    
    // Update the navigation arrows
    // @param position position of current notification in a list of notifications (first is 0)
    // @param numNotifications total number of notifications    
    this.updateNavigation = function(position, numNotifications) {
        if (!this._includeNavigation)
            return
    
        var bubble = this // for callback closures
        
        dh.util.dom.replaceContents(this._navText, document.createTextNode((position + 1) + " of " + numNotifications))
        
        dh.util.dom.clearNode(this._navButtons)
        var button = document.createElement("img")
        button.className = "dh-notification-navigation-button"
        button.setAttribute("src", dh.appletUrl + "activeLeft.png")
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onPrevious()
            return false;
        })
        
        button = document.createElement("img")
        button.className = "dh-notification-navigation-button"
        button.src = dh.appletUrl + "activeRight.png"
        this._navButtons.appendChild(button)
        button.onclick = dh.util.dom.stdEventHandler(function (e) {
            bubble.onNext()
            return false;
        })
    }
    
    // Helper function for data object display routines; create a link to a DumbHippo post
    this.createSharedLinkLink = function(linkTitle, postId, url) {
        var a = document.createElement("a")
        a.setAttribute("href", "javascript:true")
        
        var bubble = this
        a.onclick = dh.util.dom.stdEventHandler(function(e) {
            e.stopPropagation();
            bubble.onLinkClick(postId, url)
            return false;
        })
        dh.util.dom.appendSpanText(a, linkTitle, "dh-notification-link-title")
        return a
    }

    // Helper function for data object display routines; render a list of recipients
    // @param node parent node
    // @param arr array of recipients
    // @param normalCssClass CSS class to use for display of a different user
    // @param cssCssClass CSS class to use for display of the current user
    this.renderRecipients = function (node, arr, normalCssClass, selfCssClass) {
        for (var i = 0; i < arr.length; i++) {
            var recipientNode = this._renderRecipient(arr[i], normalCssClass, selfCssClass)
            node.appendChild(recipientNode)
            if (i < arr.length - 1) {
                node.appendChild(document.createTextNode(", "))
            }
        }  
    }
    
    // Get the width of the bubble's desired area
    // @return the desired width, in pixels
    this.getWidth = function() {
        var width = dh.bubble.BASE_WIDTH
        if (this._includeNavigation)
            width += dh.bubble.NAVIGATION_WIDTH
            
        return width
    }

    // Get the height of the bubble's desired area
    // @return the desired width, in pixels
    this.getHeight = function() {
        if (this._swarmDisplay)
            return dh.bubble.SWARM_HEIGHT
        else
            return dh.bubble.BASE_HEIGHT
    }

    // Render a single recipient
    this._renderRecipient = function (recipient, normalCssClass, selfCssClass) {  
        var name = recipient.name
        dh.util.debug("rendering recipient with id=" + recipient + ", name=" + name)
        var cssClass = normalCssClass;
        if (recipient.id == dh.selfId) {
            name = "you"
            cssClass = selfCssClass
        }
        var node = document.createElement("span")
        node.setAttribute("className", cssClass)
        node.appendChild(document.createTextNode(name))       
        return node
    }
     
    // Update the image for the photo on the left of the bubble
    this._setPhotoImage = function (src, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photo")
        var img = document.createElement("div")
        img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale')"
        img.setAttribute("className", "dh-notification-photo")
        a.appendChild(img)
        dh.util.dom.replaceContents(this._photoDiv, a)
    }
    
    // Update the title underneath the photo on the left
    this._setPhotoTitle = function (text, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photolink")
        a.appendChild(document.createTextNode(text))
        dh.util.dom.replaceContents(this._photoLinkDiv, a)
    }
    
    // Update the contents of the bubble to match this._data    
    this._render = function() {
        this._setPhotoImage(this._data.getPhotoSrc(), this._data.getPhotoLink())
        this._setPhotoTitle(this._data.getPhotoTitle(), this._data.getPhotoLink())
        
        dh.util.dom.clearNode(this._titleDiv)
        this._data.appendTitleContent(this, this._titleDiv)

        dh.util.dom.clearNode(this._bodyDiv)
        this._data.appendBodyContent(this, this._bodyDiv)
        
        dh.util.dom.clearNode(this._metaSpan)
        this._data.appendMetaContent(this, this._metaSpan)

        dh.util.dom.clearNode(this._swarmDiv)
        this._data.appendViewersContent(this, this._swarmDiv)
        this._setSwarmDisplay(this._swarmDiv.firstChild != null)
        
        dh.util.dom.clearNode(this._leftImgSpan)
        dh.util.dom.clearNode(this._rightImgSpan)
        var leftImg;
        var rightImg;
        if (this._swarmDisplay) {
            leftImg = this._leftImgSwarm
            rightImg = this._rightImgSwarm            
        } else {
            leftImg = this._leftImg
            rightImg = this._rightImg
        }
        this._leftImgSpan.appendChild(leftImg)
        this._rightImgSpan.appendChild(rightImg)
        
        this._fixupLayout()
    }
    
    // Adjust various sizes that we can't make the CSS handle
    this._fixupLayout = function() {
        if (this._includeNavigation)
            this._titleDiv.style.width = (this._rightsideDiv.clientWidth - this._closeButton.offsetWidth) + "px"
    }
    
    // Set whether the viewer bubble is currently showing
    this._setSwarmDisplay = function(swarmDisplay) {
        swarmDisplay = !!swarmDisplay
        if (this._swarmDisplay != swarmDisplay) {
            this._swarmDisplay = swarmDisplay
            
            if (swarmDisplay) {
                this._swarmDiv.style.display = "block"
                this._swarmNavDiv.style.display = "block"
            } else {
                this._swarmDiv.style.display = "none"
                this._swarmNavDiv.style.display = "none"
            }
            
            this.onSizeChange()
        }
    }   
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for DumbHippo posts
//////////////////////////////////////////////////////////////////////////////
    
// Extension point for specific post types
dh.bubble.postExtensions = {}
    
dh.bubble.PostData = function(senderId, postId, linkTitle, 
                              linkURL, linkDescription, recipients, 
                              viewers, postInfo, viewerHasViewed) {
    this.senderId = senderId
    this.postId = postId
    this.linkTitle = linkTitle
    this.linkURL = linkURL
    this.linkDescription = linkDescription
    this.recipients = recipients
    this.viewers = viewers
    if (postInfo != null && !postInfo.match(/^\s*$/)) {
        this.info = dh.parseXML(postInfo)
    } else {
        this.info = null
    }
    this.viewerHasViewed = viewerHasViewed

    this.getPhotoLink = function() {
        return dh.serverUrl + "person?who=" + this.senderId
    }
    
    this.getPhotoSrc = function() {
        dh.util.debug("looking up entity" + this.senderId)
        var ent = dh.bubble.findEntity(this.senderId)
        dh.util.debug("got entity " + ent)
        if (!ent)
            return ""
        var result = dh.serverUrl + ent.smallPhotoUrl       
        return result
    }
    
    this.getPhotoTitle = function() {
        var ent = dh.bubble.findEntity(this.senderId)    
        return ent.name
    }
    
    this.appendTitleContent = function(bubble, parent) {
        var a = bubble.createSharedLinkLink(this.linkTitle, this.postId, this.linkURL)    
        if (this.viewerHasViewed)
            dh.util.prependCssClass(a, "dh-notification-title-seen") 
        parent.appendChild(a)
    }
        
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.linkDescription))
        
        for (extension in dh.bubble.postExtensions) {
            var ext = dh.bubble.postExtensions[extension]
            if (ext.accept(this))
                ext.drawContent(this, bubble._bodyDiv)
        }
    }
    
    this.appendMetaContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode("Sent to "))
       
        var personRecipients = []
        var groupRecipients = []        
        var i;
        for (i = 0; i < this.recipients.length; i++) {
            var recipId = this.recipients[i]
            var ent = dh.bubble.findEntity(recipId)
            if ((ent instanceof dh.bubble.Person) || (ent instanceof dh.bubble.Resource)) {
                personRecipients.push(ent)
            } else if (ent instanceof dh.bubble.Group) {
                groupRecipients.push(ent)
            }
        }
        // FIXME this is all hostile to i18n
        bubble.renderRecipients(parent, groupRecipients, "dh-notification-group-recipient")
        if (personRecipients.length > 0 && groupRecipients.length > 0) {
            parent.appendChild(document.createTextNode(", "))
        }        
        bubble.renderRecipients(parent, personRecipients, "dh-notification-recipient", "dh-notification-self-recipient")
    }
            
    this.appendViewersContent = function(bubble, parent) {
        var viewers = []
        for (var i = 0; i < this.viewers.length; i++) {
            var ent = dh.bubble.findEntity(this.viewers[i])
            viewers.push(ent)          
        }
        if (viewers.length > 0) {
            bubble.renderRecipients(parent, viewers, "dh-notification-viewer", "dh-notification-self-viewer")
        }
    }
    
    this.getViewersPhotoSrc = function() {
        // Need to pass in the viewer ID as well as name to here to display
        return null
    }
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for MySpace blog comments
//////////////////////////////////////////////////////////////////////////////
    
// Create a new MySpace blog comment object
// @param myId ID of the current user (the user owning the blog)
// @param blogId ID of the blog *post* that was commented on
// @param commentId ID of the new comment on the post
// @param posterName Name of the comment poster
// @param posterImgUrl URL to a photo of the comment poster
// @param content the content of the post
dh.bubble.MySpaceData = function(myId, blogId, commentId, posterId, posterName, posterImgUrl, content) {
    this.myId = myId,
    this.blogId = blogId
    this.commentId = commentId
    this.posterId = posterId
    this.posterName = posterName
    this.posterImgUrl = posterImgUrl
    this.content = content
    
    this.getPhotoLink = function() {
        return "http://myspace.com/" + this.posterId
    }
    
    this.getPhotoSrc = function() {
        return this.posterImgUrl
    }
    
    this.getPhotoTitle = function() {
        return this.posterName
    }

    this.appendTitleContent = function(bubble, parent) {
        var a = document.createElement("a")
        a.appendChild(document.createTextNode("New MySpace comment from " + this.posterName))
        a.setAttribute("href", "http://blog.myspace.com/index.cfm?fuseaction=blog.view&friendID=" + this.myId + "&blogID=" + this.blogId)
        
        parent.appendChild(a)
    }
            
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.content))
    }
    
    this.appendMetaContent = function(bubble, parent) {
    }
    
    this.appendViewersContent = function(bubble, parent) {
    }
    
    this.getViewersPhotoSrc = function() {
        return null
    }
}
