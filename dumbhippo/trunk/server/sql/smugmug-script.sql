/*  

1) We need to insert new record to OnlineAccountType for Smugmug
2) CachedSmugmugAlbum is cached table for smugmug media data
3) SmugmugUpdateStatus is cached status table for smugmug media data

*/

insert into OnlineAccountType(accountType, fullName, name, site, siteName, supported, userInfoType, creator_id)
values (19, 'SmugMug', 'smugmug', 'http://www.smugmug.com', 'SmugMug', 0, 'SmugMug username', 'jvRWD8vTAf3vKj');

CREATE TABLE  `CachedSmugmugAlbum` (
  `id` bigint(20) NOT NULL auto_increment,
  `imageId` varchar(20) collate utf8_bin default NULL,
  `imageKey` varchar(255) collate utf8_bin NOT NULL,
  `caption` varchar(255) collate utf8_bin default NULL,
  `lastUpdated` varchar(255) collate utf8_bin NOT NULL,
  `thumbUrl` varchar(255) collate utf8_bin NOT NULL,
  `owner` varchar(255) collate utf8_bin NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `thumbUrl` (`thumbUrl`,`owner`)
);

CREATE TABLE  `SmugmugUpdateStatus` (
  `id` bigint(20) NOT NULL auto_increment,
  `albumHash` varchar(255) collate utf8_bin NOT NULL,
  `username` varchar(255) collate utf8_bin NOT NULL,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `username` (`username`)
);