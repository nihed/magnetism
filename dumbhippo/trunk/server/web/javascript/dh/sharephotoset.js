dojo.require("dh.sharelink");
dojo.provide("dh.sharephotoset");
dojo.provide("dh.flickrupload");

dh.flickrupload.Photo = function(filename, thumbnailPath) {
	this.filename = filename
	this.thumbnailPath = thumbnailPath
	this.state = 'queued'
	this.flickrId = null
	
	this.render = function() {
		this.div = document.createElement("div")	
		this.img = document.createElement("img")
		this.img.setAttribute("src", thumbnailPath)
		this.div.appendChild(this.img)
		this.textSpan = document.createElement("span")
		this.div.appendChild(this.textSpan)
		var text;
		if (this.state == 'queued')
			text = "Waiting to upload"
		else if (this.state == 'uploading')
			text = "Uploading, " + this.progress + "% complete"
		else if (this.state == 'complete')
			text = "Upload complete"
		else
			text = "(error)"
		this.textSpan.appendChild(document.createTextNode(text))
		return this.div
	}
	
	this.getId = function() {
		return this.filename
	}
	
	this.getFlickrId = function () {
		return this.flickrId
	}
	
	this.getState = function () {
		return this.state
	}
	
	this.uploadProgress = function (progress) {
		this.state = 'uploading'
		this.progress = progress
	}
	
	this.uploadComplete = function (flickrId) {
		this.state = 'complete'
		this.flickrId = flickrId
	}
}
dojo.inherits(dh.flickrupload.Photo, Object);

dh.flickrupload.UploadSet = function() {
	this.photos = []
	this.currentPhoto = null
	this.div = document.createElement("div")
	this.photoDiv = document.createElement("div")
	this.div.appendChild(this.photoDiv)
	this.statusText = document.createElement("span")
	this.div.appendChild(this.statusText)
	this.flickrUrl = null
	
	this.createNavigationLink = function (text, cb) {
		var a = document.createElement("a")
		a.appendChild(document.createTextNode(text))
		a.style.margin = "1em"
		a.setAttribute("href", "javascript:")
		a.onclick = cb
		return a
	}
	var uploadset = this
	this.next = this.createNavigationLink("Next", function() { uploadset.doNext(); })
	this.prev = this.createNavigationLink("Previous", function() { uploadset.doPrev(); })
	this.div.appendChild(document.createElement("br"))
	this.div.appendChild(this.prev)	
	this.div.appendChild(this.next)
	
	// Invoked from sharelink.js
	this.render = function () {
		var parent = document.getElementById("dhFlickrPhotoUpload")
		parent.appendChild(this.div)
	}
	
	this.getUrl = function () {
		return this.flickrUrl
	}

	this.setFlickrUrl = function (url) {
		this.flickrUrl = url
	}
	
	this.addPhoto = function (filename, thumbnailPath) {
		var photo = new dh.flickrupload.Photo(filename, thumbnailPath)
		dojo.debug("constructed photo " + photo + " thumbnail: " + thumbnailPath)
		this.photos.push(photo)
		if (this.currentPhoto == null) {
			this.setPhoto(0)
		} else {
			this.redrawStatusText()		
		}
	}
	
	this.uploadProgress = function (photoId, progress) {
		this.invokeAndRedraw(photoId, function (photo) { photo.uploadProgress(progress) })
	}
	
	this.uploadComplete = function (photoId, flickrPhotoId) {
		this.invokeAndRedraw(photoId, function (photo) { photo.uploadComplete(flickrPhotoId) })
	}
	
	this.doNext = function() {
		this.setPhoto(this.currentPhoto + 1)
	}
	
	this.doPrev = function() {
		this.setPhoto(this.currentPhoto - 1)
	}
	
	// private:
	this.invokeAndRedraw = function (photoId, cb) {
		var i = this.findPhotoIndexForId(photoId)
		var photo = this.photos[i]
		cb(photo)
		if (this.currentPhoto == i)
			this.setPhoto(this.currentPhoto) // redraws	
	}
	
	this.setPhoto = function(i) {
		if (i < 0 || i >= this.photos.length) {
			dojo.debug("invalid photo index " + i)			
			return
		}
		dojo.debug("displaying photo index " + i)	
		var photo = this.photos[i]
		this.currentPhoto = i
		dh.util.clearNode(this.photoDiv)
		this.photoDiv.appendChild(photo.render())
		this.redrawStatusText()		
	}

	this.findPhotoIndexForId = function (id) {
		var i;
		for (i = 0; i < this.photos.length; i++) {
			if (this.photos[i].getId() == id) {
				return i;
			}
		}
		return null
	}
	
	this.redrawStatusText = function () {
		dh.util.clearNode(this.statusText)
		var i = this.currentPhoto
		if (i == null) {
			i = 0;
		}
		this.statusText.appendChild(document.createTextNode("" + (i+1) + " of " + this.photos.length))
	}
}
dojo.inherits(dh.flickrupload.UploadSet, Object);

dojo.debug("creating photoset instance"); 
dh.sharephotoset.instance = new dh.flickrupload.UploadSet()

dh.sharephotoset.submitButtonClicked = function() {
	dojo.debug("clicked share photo button");
	var title = dh.sharelink.urlTitleToShareEditBox.textValue
	var descriptionHtml = dh.util.getTextFromRichText(dh.share.descriptionRichText)
	dojo.debug("title = " + title)
	dojo.debug("description = " + descriptionHtml)		
	window.external.CreatePhotoset(title, descriptionHtml)
}

// These functions are invoked from the client directly; they're
// globally namespaced in a hackish way because doing real javascript
// namespacing was too annoying to code in C++
dhFlickrAddPhoto = function (filename, thumbnailFilename) {
	try {
	dojo.debug("adding photo " + filename)
	dh.sharephotoset.instance.addPhoto(filename, thumbnailFilename)
	} catch (e) {
		dojo.debug("dhFlickrAddPhoto failed:" + e.message)
	}
}

dhFlickrPhotoUploadStarted = function (filename) {
	try {
	dojo.debug("got upload started for " + filename)
	dh.sharephotoset.instance.uploadProgress(filename, 69)
	} catch (e) {
		dojo.debug("dhFlickrPhotoUploadStarted failed:" + e.message)
	}	
}

dhFlickrPhotoUploadComplete = function (filename, photoId) {
	try {
	dojo.debug("upload complete for photo " + filename)
	dh.sharephotoset.instance.uploadComplete(filename, photoId)
	} catch (e) {
		dojo.debug("dhFlickrPhotoUploadComplete failed:" + e.message)
	}		
}

dhFlickrPhotosetCreated = function (photosetId, photosetUrl) {
	dojo.debug("photoset creation complete, id=" + photosetId);
}

dhFlickrError = function (text) {
	var display = document.getElementById("dhFlickrError")
	dh.util.clearNode(display)
	display.appendChild(document.createTextNode(text))
}

dh.sharelink.extHooks.push(function () { dojo.debug("rendering photoset"); 
                                         dh.sharephotoset.instance.render(); });

