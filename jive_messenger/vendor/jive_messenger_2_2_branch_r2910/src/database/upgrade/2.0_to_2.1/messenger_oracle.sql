REM // $RCSfile$
REM // $Revision: 795 $
REM // $Date: 2005-01-06 05:44:42 -0500 (Thu, 06 Jan 2005) $

REM // upgrades from Messenger 2.0.x to 2.1.0

REM // jiveGroup: Recreate table from scratch
DROP TABLE jiveGroup;
CREATE TABLE jiveGroup (
  groupName             VARCHAR2(50)    NOT NULL,
  description           VARCHAR2(255),
  CONSTRAINT jiveGroup_pk PRIMARY KEY (groupName)
);


REM // jiveGroupProp: Recreate table from scratch
DROP TABLE jiveGroupProp;
CREATE TABLE jiveGroupProp (
  groupName             VARCHAR(50)     NOT NULL,
  name                  VARCHAR2(100)   NOT NULL,
  propValue             VARCHAR2(4000)  NOT NULL,
  CONSTRAINT jiveGroupProp_pk PRIMARY KEY (groupName, name)
);


REM // jiveGroupUser: Recreate table from scratch
DROP TABLE jiveGroupUser;
CREATE TABLE jiveGroupUser (
  groupName             VARCHAR(50)     NOT NULL,
  username              VARCHAR2(32)    NOT NULL,
  administrator         INTEGER         NOT NULL,
  CONSTRAINT jiveGroupUser PRIMARY KEY (groupName, username, administrator)
);


REM // mucRoom: Add new columns: "lockedDate" and "emptyDate". Rename column "invitationRequired" to "membersOnly". Delete columns: "lastActiveDate" and "inMemory".
ALTER TABLE mucRoom ADD lockedDate CHAR(15) NOT NULL;
ALTER TABLE mucRoom ADD emptyDate CHAR(15) NULL;
ALTER TABLE mucRoom RENAME COLUMN invitationRequired TO membersOnly;
ALTER TABLE mucRoom DROP COLUMN lastActiveDate;
ALTER TABLE mucRoom DROP COLUMN inMemory;

REM // mucMember: Add new columns
ALTER TABLE mucMember ADD firstName VARCHAR2(100) NULL;
ALTER TABLE mucMember ADD lastName  VARCHAR2(100) NULL;
ALTER TABLE mucMember ADD url VARCHAR2(100) NULL;
ALTER TABLE mucMember ADD email VARCHAR2(100) NULL;
ALTER TABLE mucMember ADD faqentry VARCHAR2(100) NULL;

REM // mucConversationLog: Add new index
CREATE INDEX mucLog_time_idx ON mucConversationLog (time);

REM // Deletes no longer needed entries
DELETE FROM jiveID where idType = 3;
DELETE FROM jiveID where idType = 4;

REM // Add jiveVersion table
CREATE TABLE jiveVersion (
  majorVersion  INTEGER  NOT NULL,
  minorVersion  INTEGER  NOT NULL
);
INSERT INTO jiveVersion (majorVersion, minorVersion) VALUES (2, 1);