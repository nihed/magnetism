// bubble.js: Shared handling of bubbles that display notifications to the
//   user of posts, MySpace comments ,and so forth
// Copyright Red Hat, Inc. 2006

dh.bubble = {} 

//////////////////////////////////////////////////////////////////////////////
// Generic display code for a single notification bubble
//////////////////////////////////////////////////////////////////////////////
    
dh.bubble.BASE_WIDTH = 399
dh.bubble.NAVIGATION_WIDTH = 51
dh.bubble.BASE_HEIGHT = 123
dh.bubble.SWARM_HEIGHT = 180
// Extra amount of height to add when we are using the list images rather than 
// the standalone images. The difference here is the difference between a 3
// pixel and 5 pixel white border for the top and bottom of the bubble
dh.bubble.LIST_EXTRA_HEIGHT = 4
    
// Create a new bubble object    
// @param isStandaloneBubble if true, then this is the standalone notification bubble,
//        and should get the navigation arrows and close button, and the appropriate
//        images for the sides.
dh.bubble.Bubble = function(isStandaloneBubble, heightAdjust) {
    // Whether to include the quit button and previous/next arrows
    this._isStandaloneBubble = isStandaloneBubble

    // The notification currently being displayed
    this._data = null
    
    // The "page" of the swarm area
    this._page = null
    
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
    
    // Called when the display of the bubble should be updated (without a size change)
    this.onUpdateDisplay = function() {}
    
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
        
        if (this._isStandaloneBubble) {
            var navDiv = appendDiv(this._topDiv, "dh-notification-navigation")
            this._navText = appendDiv(navDiv, "dh-notification-navigation-text")
            this._navButtons = appendDiv(navDiv, "dh-notification-navigation-buttons")
            
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
        } else {
            function createImage(src, className, height) {
                var img = document.createElement("img")
                img.src = dh.appletUrl + src
                img.width = 10
                img.height = height
                img.className = className
                img.zIndex = 1
                return img
            }
            
            this._leftImg = createImage("listBubbleLeft.png", "dh-left-img", dh.bubble.BASE_HEIGHT + dh.bubble.LIST_EXTRA_HEIGHT)
            this._rightImg = createImage("listBubbleRight.png", "dh-right-img", dh.bubble.BASE_HEIGHT + dh.bubble.LIST_EXTRA_HEIGHT)
            this._leftImgSwarm = createImage("listBubbleLeftSwarm.png", "dh-left-img", dh.bubble.SWARM_HEIGHT + dh.bubble.LIST_EXTRA_HEIGHT)
            this._rightImgSwarm = createImage("listBubbleRightSwarm.png", "dh-right-img", dh.bubble.SWARM_HEIGHT + dh.bubble.LIST_EXTRA_HEIGHT)
        }
        
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
        this._swarmDiv = appendDiv(this._topDiv, "dh-notification-swarm")
        
        if (this._isStandaloneBubble) {
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
        if (!this._isStandaloneBubble)
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
    
    // Set which "page" we are currently showing in the bottom tab; the
    // @param page the name of the page ("whosThere", "someoneSaid", etc.)
    this.setPage = function(page) {
        this._page = page
        if (this._swarmPages && this._swarmPages.length > 0)
            this._updateSwarmPage()
        this.onUpdateDisplay()
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
            var recipientNode = this.renderRecipient(arr[i], normalCssClass, selfCssClass)
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
        if (this._isStandaloneBubble)
            width += dh.bubble.NAVIGATION_WIDTH
            
        return width
    }

    // Get the height of the bubble's desired area
    // @return the desired width, in pixels
    this.getHeight = function() {
        var extraHeight
        if (this._isStandaloneBubble)
            extraHeight = 0
        else
            extraHeight = dh.bubble.LIST_EXTRA_HEIGHT
        if (this._swarmDisplay)
            return extraHeight + dh.bubble.SWARM_HEIGHT
        else
            return extraHeight + dh.bubble.BASE_HEIGHT
    }

    // Render a single recipient
    this.renderRecipient = function (recipient, normalCssClass, selfCssClass) {  
        dh.util.debug("rendering recipient with id=" + recipient.Id + ", name=" + recipient.Name)
        var cssClass = normalCssClass
        var name = recipient.Name
        if (recipient.Id == dh.selfId) {
            name = "you"
            cssClass = selfCssClass
        }
        var node = document.createElement("span")
        node.setAttribute("className", cssClass)
        node.appendChild(document.createTextNode(name))
        return node
    }
     
    this.createPngElement = function(src) {
        var img = document.createElement("div")
        img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='" + src + "', sizingMethod='scale')"
        
        return img
    }

    // Update the image for the photo on the left of the bubble
    this._setPhotoImage = function (src, url) {
        var a = document.createElement("a")
        a.setAttribute("href", url)
        a.setAttribute("className", "dh-notification-photo")
        img = this.createPngElement(src)
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
        this._swarmPages = this._data.appendSwarmContent(this, this._swarmDiv)
        
        if (this._swarmPages.length > 0) {
            this._updateSwarmPage()
            this._setSwarmDisplay(true)
        } else {
            this._setSwarmDisplay(false)
        }

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
        this.onUpdateDisplay()
    }
    
    // Adjust various sizes that we can't make the CSS handle
    this._fixupLayout = function() {
        if (this._isStandaloneBubble)
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
    
    // Update the currently showing page and the navigation links for the swarm area
    this._updateSwarmPage = function() {
        dh.util.dom.clearNode(this._swarmNavDiv)
        var activePage = null
        for (i = 0; i < this._swarmPages.length; i++) {
            var page = this._swarmPages[i]
            if (page.name == this._page) {
                activePage = page
            }
        }
        
        if (activePage == null) {
            activePage = this._swarmPages[0]
        }
        
        for (i = 0; i < this._swarmPages.length; i++) {
            var page = this._swarmPages[i]
            page.div.style.display = (page == activePage) ? "block" : "none"
            var link = this._createSwarmNavLink(page, page == activePage)
            link.style.width = (100 / this._swarmPages.length) + "%"
            this._swarmNavDiv.appendChild(link)
        }            
    }
    
    // Create a link that goes in the navigation area
    this._createSwarmNavLink = function(page, isActive) {
        var link
        
        if (isActive)
            link = document.createElement("span")
        else {
            link = document.createElement("a")
                    
            link.href = "javascript:void(0)"
            var bubble = this
            var pageName = page.name
            link.onclick = function() { bubble.setPage(pageName) }
        }
                    
        link.className = "dh-notification-swarm-nav-link"
        link.appendChild(document.createTextNode(page.title))
        
        return link
    }
}

//////////////////////////////////////////////////////////////////////////////
// Notification data object for DumbHippo posts
//////////////////////////////////////////////////////////////////////////////
    
// Extension point for specific post types
dh.bubble.postExtensions = {}
    
dh.bubble.PostData = function(post) {
    this.post = post
    var postInfo = post.Info
    if (postInfo != null && !postInfo.match(/^\s*$/)) {
        this.info = dh.parseXML(postInfo)
    } else {
        this.info = null
    }

    this.getPhotoLink = function() {
        return dh.serverUrl + "person?who=" + this.post.Sender.Id
    }
    
    this.getPhotoSrc = function() {
        return dh.serverUrl + this.post.Sender.SmallPhotoUrl
    }
    
    this.getPhotoTitle = function() {
        return this.post.Sender.Name
    }
    
    this.appendTitleContent = function(bubble, parent) {
        var a = bubble.createSharedLinkLink(this.post.Title, this.post.Id, this.post.Url)
        if (this.post.HaveViewed)
            dh.util.prependCssClass(a, "dh-notification-title-seen") 
        parent.appendChild(a)
    }
        
    this.appendBodyContent = function(bubble, parent) {
        parent.appendChild(document.createTextNode(this.post.Description))
        
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
        var recipients = this.post.Recipients
        for (i = 0; i < recipients.length; i++) {
            var recipient = recipients.item(i)
            var type = recipient.type
            if (type == 2) // GROUP
                groupRecipients.push(recipient)
            else
                personRecipients.push(recipient)
        }
        // FIXME this is all hostile to i18n
        bubble.renderRecipients(parent, groupRecipients, "dh-notification-group-recipient")
        if (personRecipients.length > 0 && groupRecipients.length > 0) {
            parent.appendChild(document.createTextNode(", "))
        }        
        bubble.renderRecipients(parent, personRecipients, "dh-notification-recipient", "dh-notification-self-recipient")
    }
            
    this.appendSwarmContent = function(bubble, parent) {
        var pages = []
    
        if (this.post.CurrentViewers.length > 0) {
            var whosThereDiv = document.createElement("div")
            whosThereDiv.className  = "dh-notification-whos-there"
            parent.appendChild(whosThereDiv)
        
            var viewersArray = []
            var viewers = this.post.CurrentViewers
            for (var i = 0; i < viewers.length; i++) {
                viewersArray.push(viewers.item(i))
            }
            bubble.renderRecipients(whosThereDiv, viewersArray, "dh-notification-viewer", "dh-notification-self-viewer")
            
            pages.push({ name: "whosThere", title: "who's there", div: whosThereDiv })
        }
        
        if (this.post.LastChatSender != null) {
            var someoneSaidDiv = document.createElement("div")
            someoneSaidDiv.className  = "dh-notification-someone-said"
            parent.appendChild(someoneSaidDiv)
            
            var senderPhoto = bubble.createPngElement(dh.serverUrl + this.post.LastChatSender.SmallPhotoUrl)
            senderPhoto.className = "dh-notification-chat-sender-photo"
            someoneSaidDiv.appendChild(senderPhoto)
            
            var messageSpan = document.createElement("span")
            messageSpan.className = "dh-notification-chat-message"
            messageSpan.appendChild(document.createTextNode(this.post.LastChatMessage))
            someoneSaidDiv.appendChild(messageSpan)            
            
            var sender = this.post.LastChatSender
            var senderDiv = document.createElement("div")
            senderDiv.className = "dh-notification-chat-sender"
            senderDiv.appendChild(bubble.renderRecipient(sender, "dh-notification-sender"))
            senderDiv.appendChild(document.createTextNode(" just said"))
            someoneSaidDiv.appendChild(senderDiv)
            
            var chattingUserCount = this.post.ChattingUserCount
            if (chattingUserCount > 0) {
                var chatCountDiv = document.createElement("div")
                chatCountDiv.className = "dh-notification-chat-count"
                chatCountDiv.appendChild(document.createTextNode("chat: " + chattingUserCount + " people"))
                someoneSaidDiv.appendChild(chatCountDiv)
            }
            
            pages.push({ name: "someoneSaid", title: "someone said", div: someoneSaidDiv })
        }
        
        return pages
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
    
    this.appendSwarmContent = function(bubble, parent) {
        return []
    }
    
    this.getViewersPhotoSrc = function() {
        return null
    }
}
