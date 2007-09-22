# vim: ts=4
###
#
# Listen is the legal property of mehdi abaakouk <theli48@gmail.com>
# Copyright (c) 2006 Mehdi Abaakouk
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 2 as
# published by the Free Software Foundation
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
#
###

import logging,ConfigParser

import gobject
import gtkmozembed

_logger = logging.getLogger("mozembed_wrap")

try:
	cf = ConfigParser.ConfigParser()
	cf.read('/etc/gre.d/gre.conf')
	comp_path=cf.get(cf.sections()[0], 'GRE_PATH')
	gtkmozembed.set_comp_path(comp_path)
except:
	_logger.error("Cannot load GRE configuration", exc_info=True)
    
"""
This class wrap MozEmbed
Use workaround to resolve binding problem:
    - freeze when more than 150ko are render
    - crash when render data and the widget is not realize
"""
class MozClient(gtkmozembed.MozEmbed):
    def __init__(self):
        super(MozClient,self).__init__()
        
        
        self.connect("realize",self.on_realize)
        self.connect("unrealize",self.on_unrealize)
        
        self.__render_id = None
        self.is_realize = False
        
        self.current_data = None
        self.current_url = None
        
    def set_data(self,url,data):
        _logger.debug("set_data: %s %s",len(data), url) 
        self.current_data = data
        self.current_url = url
        if self.is_realize:
            self.on_realize()
        
    def on_unrealize(self,w=None):
        _logger.debug("on_unrealize")       
        self.is_realize = False
        
    def on_realize(self,w=None):
        _logger.debug("on_realize")    
        self.is_realize = True
        if self.current_data and self.current_url:
            if self.__render_id:
                try:gobject.remove_source(self.__render_id)
                except:pass
                self.__render_id = None
                self.close_stream()
            self.open_stream(self.current_url, "text/html")
            
            self.__render_id = gobject.idle_add(self.idle_set_data,self.current_data)
                
    def idle_set_data(self,data):
        _logger.debug("idle_set_data: %s", len(data))
        buffer = 10*1024
        tmp_data = data[:buffer]
        self.append_data(tmp_data,long(len(tmp_data)))
        tmp_data = data[buffer:]
        if len(tmp_data)>0:
            self.__render_id = gobject.idle_add(self.idle_set_data,tmp_data)
        else:
            _logger.debug("close_stream")
            self.close_stream()  
            self.__render_id = None
        
        
