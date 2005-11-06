/*------------------------------------------------------------------------------
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is levelonelabs.com code.
 * The Initial Developer of the Original Code is Level One Labs. Portions
 * created by the Initial Developer are Copyright (C) 2001 the Initial
 * Developer. All Rights Reserved.
 *
 *         Contributor(s):
 *             Scott Oster      (ostersc@alum.rpi.edu)
 *             Steve Zingelwicz (sez@po.cwru.edu)
 *             William Gorman   (willgorman@hotmail.com)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable
 * instead of those above. If you wish to allow use of your version of this
 * file only under the terms of either the GPL or the LGPL, and not to allow
 * others to use your version of this file under the terms of the NPL, indicate
 * your decision by deleting the provisions above and replace them with the
 * notice and other provisions required by the GPL or the LGPL. If you do not
 * delete the provisions above, a recipient may use your version of this file
 * under the terms of any one of the NPL, the GPL or the LGPL.
 *----------------------------------------------------------------------------*/


package com.levelonelabs.aim;


import java.util.List;


/**
 * Interface for AIM connection provider
 *
 * @author Scott Oster
 *
 * @created January 1, 2002
 */
public interface AIMSender {
    
    public static final String DEFAULT_GROUP = "TOC";
    
    public void sendMessage(AIMBuddy buddy, String text);

    public void sendWarning(AIMBuddy buddy);

    public List<ScreenName> getBuddyNames();
    public AIMBuddy getBuddy(ScreenName name);
    public void addBuddy(AIMBuddy buddy);
    public void removeBuddy(AIMBuddy buddy);

    public void banBuddy(AIMBuddy buddy);

    public void addListener(AIMListener listener);

    // in AIMClient this method didn't do anything except
    // sleep() while run() was doing stuff
    //public void signOn();
    public void signOff();

    public void setPermitMode(PermitDenyMode mode);
    public PermitDenyMode getPermitMode();
    public void denyBuddy(AIMBuddy buddy);
    public void permitBuddy(AIMBuddy buddy);

    public void setAvailable();
    public void setUnavailable(String reason);
}
