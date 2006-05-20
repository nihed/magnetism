dojo.provide('dh.breaksfirefox10')

/* Firefox 1.0 won't _parse_ this regex, i.e. it will get upset if it reads this file, even without evaluating it */

// we mainly identify a url by it containing a dot and two or three letters after it, which
// can then be followed by a slash and more letters and acceptable characters 
// this should superset almost all possibly ways to type in a url
// we also use http://, https://, www, web, ftp, and ftp:// to identify urls like www.amazon, which
// are also accepted by the browers

dhUrlRegex = /([^\s"'<>[\]][\w._%-:/]*\.[a-z]{2,3}(\/[\w._%-:/&=?]*)?(["'<>[\]\s]|$))|(https?:\/\/)|((www|web)\.)|(ftp\.)|(ftp:\/\/)/i
