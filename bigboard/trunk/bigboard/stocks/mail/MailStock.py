import logging, re, htmlentitydefs

import gobject, gtk
import hippo

from bigboard.stock import Stock
from bigboard.slideout import ThemedSlideout
import bigboard.google as google
import bigboard.google_stock as google_stock  
from bigboard.big_widgets import CanvasHBox, CanvasVBox, Button, Header, ThemedText, PrelightingCanvasBox
#TODO: add a scrollable view for emails
#import bigboard.scroll_ribbon as scroll_ribbon

import libgmail_patched as libgmail

_logger = logging.getLogger('bigboard.stocks.MailStock')

def remove_strange_tags(s, markup=False):
    if "\\u003cb\\>" in s:
        if markup == True:
            b = "<b>"
            e = "</b>"
        else:
            b = ""
            e = ""
        s = s.replace("\\u003cb\\>", b)
        s = s.replace("\\u003c/b\\>", e)
    return s

def convert_entities(s):
    exp = re.compile("&[#a-zA-Z0-9]*;")
    for match in exp.finditer(s):
        if match is not None:
            html_entity = match.group()
            try:
                if html_entity[1] == '#':
                    entity_num = int(html_entity[2:-1])
                    replacement_entity = unichr(entity_num)
                else:
                    entity_str = html_entity[1:-1]
                    replacement_entity = unichr(htmlentitydefs.name2codepoint[entity_str])
                s = s.replace(html_entity, replacement_entity)
            except KeyError:
                pass
    return s

class LabelSlideout(ThemedSlideout):
    __gsignals__ = {
                    'changed' : (gobject.SIGNAL_RUN_LAST, gobject.TYPE_NONE, (gobject.TYPE_STRING, )),
                   }
    def __init__(self, ga):
        super(LabelSlideout, self).__init__()
        vbox = CanvasVBox(border_color=0x0000000ff, spacing=4)
        self.get_root().append(vbox)
        header = Header(topborder=False)
        account_text = ThemedText(theme_hints=['header'], text=ga.name, font="14px Bold")
        header.append(account_text, hippo.PACK_EXPAND)        
        vbox.append(header)
        folderCounts = ga.getFolderCounts()
        folderCounts["unread"] = ga.getUnreadMsgCount()
        for label, number in folderCounts.iteritems():
            box = PrelightingCanvasBox()
            box.connect('button-release-event', self.on_button_release_event, label)
            vbox.append(box)
            hbox = CanvasHBox(spacing=4, padding=4)
            text= hippo.CanvasText(text=label, xalign=hippo.ALIGNMENT_START)
            hbox.append(text)
            text= hippo.CanvasText(text=number, xalign=hippo.ALIGNMENT_START)
            hbox.append(text, flags=hippo.PACK_END)
            box.append(hbox)
    
    def on_button_release_event (self, hippo_item, event, label_text):
        self.emit('changed', label_text)

class EmailSlideout(ThemedSlideout):
    def __init__(self, thread):
        super(EmailSlideout, self).__init__()
        vbox = CanvasVBox(border_color=0x0000000ff, spacing=4)
        self.get_root().append(vbox)
        self.__header = Header(topborder=False)
        
        subject = remove_strange_tags(thread.subject)
        
        subject_box = ThemedText(theme_hints=['header'], text=subject, font="14px Bold")
        self.__header.append(subject_box, hippo.PACK_EXPAND)
        vbox.append(self.__header)
        
        for key in ("date", "categories", "snippet"):
            value = getattr(thread, key, None)
            if value:
                if type(value) is list:
                    s = ", ".join(value)
                if type(value) is str:
                    s = remove_strange_tags(value)
                
                s = convert_entities(s)
                box = hippo.CanvasText(text=s, xalign=hippo.ALIGNMENT_START)
                vbox.append(box)
        
        #todo: nicify email, strip out junk, and show actual email
        #email_source = thread[len(thread)-1].source
        #we could use a regular expression, but its not so simple.
        #exp = "^\\nReceived:.?\\nMessage-ID:.?\\nDate:.?\\nFrom:.?\\nTo:.?\\nSubject:.?\\n"
        # the following doesn't always work
        #psr =  email.parser.Parser()
        #print psr.parsestr(email_source).get_payload()

class MailStock(Stock, google_stock.GoogleStock):
    """Shows recent emails"""
    def __init__(self, *args, **kwargs):
        print "starting mail stock"
        Stock.__init__(self, *args, **kwargs)
        google_stock.GoogleStock.__init__(self, 'gmail', **kwargs)

        self._box = hippo.CanvasBox(orientation=hippo.ORIENTATION_VERTICAL, spacing=4, padding_top=2)
        
        self.__slideout = None
        
        self.__google_account = None
        self.__folder = 'inbox'
        
        self.__display_limit = 4
        
        button = self._create_login_button()
        self._box.append(button)
        
        self._add_more_button(self.__on_more_button)
                                
    def get_content(self, size):
        return self._box
    
    def update_google_data(self, gobj):
        username = gobj.get_account().get_username_as_google_email()
        password = gobj.get_account().get_password()
        self.__update_email_box (username, password)
   
    def __update_email_box (self, username, password):        
        self._box.remove_all()
        
        try:
            if self.__google_account is None or username != self.__google_account.name:
                self.__google_account = libgmail.GmailAccount(username, password)
                self.__google_account.login()
                
            account = ThemedText(theme_hints=['header'], text=self.__google_account.name, font="14px Bold Italic")
            self._box.append(account)
            
            box = PrelightingCanvasBox()
            box.connect("button-release-event", self.create_label_slideout, self.__google_account)
            self._box.append(box)
            label = hippo.CanvasText(text=self.__folder, font="14px Bold Italic")
            box.append(label)
            
            if self.__folder == 'inbox':
                threads = self.__google_account.getMessagesByFolder(self.__folder)
            
            elif self.__folder == 'unread':
                threads = self.__google_account.getUnreadMessages()
            else:
                threads = self.__google_account.getMessagesByLabel(self.__folder)
            
            i = 0
            for thread in threads:
                if i >= self.__display_limit: break
                
                subject = remove_strange_tags(thread.subject, True)
                
                box = PrelightingCanvasBox()
                box.connect("button-release-event", self.create_email_slideout, thread)
                self._box.append(box)
                email = hippo.CanvasText(markup=subject, xalign=hippo.ALIGNMENT_START)
                box.append(email)
                i += 1
            labelsDict = self.__google_account.getFolderCounts()
            footer = ThemedText(theme_hints=['footer'], text="%s unread" % labelsDict['inbox'], font="14px Bold Italic")
            self._box.append(footer)
            print "updated mailbox"
            
        except libgmail.GmailLoginFailure:
            error = hippo.CanvasText(text="Error: Could not connect to gmail.", size_mode=hippo.CANVAS_SIZE_WRAP_WORD)
            self._box.append(error)
            
    def show_slideout(self, widget):
        def on_slideout_close(s, action_taken):
            if action_taken:
                self._panel.action_taken()
            s.destroy()
            self.__slideout = None
        self.__slideout.connect('close', on_slideout_close)
        y = widget.get_context().translate_to_screen(widget)[1]
        if not self.__slideout.slideout_from(204, y):
            self.__slideout.destroy()
            self.__slideout = None
            return
    
    def create_label_slideout(self, widget, hippo_event, data):
        self.__slideout = LabelSlideout(data)
        self.__slideout.connect('changed', self.on_label_changed)
        self.show_slideout(widget)
    
    def create_email_slideout(self, widget, hippo_event, data):
         self.__slideout = EmailSlideout(data)
         self.show_slideout(widget)
    
    def on_label_changed (self, slideout, label):
        self.__folder = label
        self.__update_email_box(self.__google_account.name, None)
    
    def __on_more_button(self):
        libbig.show_url("http://mail.google.com/mail")
