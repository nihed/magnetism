dhFlickrInit = function () {
}

dhFlickrPhotoUploadStarted = function (filename) {
    var dlg = document.getElementById('dh-dialog')
    var dlg = document.getElementById('dh-dialog')
    dlg.appendChild(document.createTextNode("upload of "))
    dh.util.dom.appendSpanText(dlg, filename, "dh-filename")
    dlg.appendChild(document.createTextNode(" started"))
    dlg.appendChild(document.createElement("br"))    
}

dhFlickrPhotoUploadComplete = function (filename, photoId) {
    var dlg = document.getElementById('dh-dialog')
    dlg.appendChild(document.createTextNode("upload of "))
    dh.util.dom.appendSpanText(dlg, filename, "dh-filename")
    dlg.appendChild(document.createTextNode(" complete, photoid="))
    dh.util.dom.appendSpanText(dlg, photoId, "dh-filename")
    dlg.appendChild(document.createElement("br"))
}

