#!/bin/bash

# Turn Dojo blog feeds into frontpage material

# blog (all)
BLOG_URL='http://blog.dojotoolkit.org/?cat=-4&feed=rss2'
BLOG_FILE=blog.html

# blog (news)
STATUS_URL='http://blog.dojotoolkit.org/category/news/feed'
STATUS_FILE=status.html
OUTDIR=/srv/www/htdocs/index_data

XSL_FILE=$OUTDIR/rss.xsl
TMP_FILE=/tmp/updateFeed

updateFeed() {
	url=$1
	file=$2
	echo Updating $2 from $1
	wget -O $TMP_FILE $url 2> /dev/null
	# Poor man's encoding fix
	perl -i -pe 's/&#([0-9]+);/{{{enc$1}}}/g' $TMP_FILE
	xsltproc $XSL_FILE $TMP_FILE | perl -lpe 's/&amp;([#0-9a-zA-Z]+);/&$1;/g' > $file
	perl -i -pe 's/\{\{\{enc([0-9]+)\}\}\}/&#$1;/g' $file
	mv -f $file $OUTDIR/
	rm $TMP_FILE
}

updateFeed $BLOG_URL $BLOG_FILE
updateFeed $STATUS_URL $STATUS_FILE
