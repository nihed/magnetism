dhFlickrPhotoUploadComplete = function (filename) {
    var dlg = document.getElementById('dh-dialog')
    dlg.appendChild(document.createTextNode("photo upload complete: " + filename))
}