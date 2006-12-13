/* HippoExplorerUtil.h: Common utility functions for HippoExplorer.h
 *
 * Copyright Red Hat, Inc. 2006
 */

#pragma once

#include <HippoUtil.h>

/**
 * Verifies that the  string has the correct form for a DumbHippo
 * GUID. It doesn't verify that it actually refers to a user, a 
 * post, or anything else in the database.
 *
 * @param guid the string to check
 * @return true if the string is of the right form for a GUID
 */
bool hippoVerifyGuid(const HippoBSTR &guid);

/**
 * Checks to see whether a hostname is a trusted server. This
 * currently means a check for a *.dumbhippo.com server, but 
 * we might later want to compare with registry settings for
 * currently configured servers.
 * 
 * @param host the host name to check
 * @return true if the host name is a trusted server
 **/
bool hippoIsOurServer(const HippoBSTR &host);
