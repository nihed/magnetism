package com.dumbhippo.services.smugmug;

public enum SmugMugMethodsEnum 
{
    smugmug_login_anonymously("smugmug.login.anonymously"),
    smugmug_login_withHash("smugmug.login.withHash"),
    smugmug_login_withPassword("smugmug.login.withPassword"),

    smugmug_albums_get("smugmug.albums.get"),
    smugmug_albums_getInfo("smugmug.albums.getInfo"),
   
    smugmug_images_get("smugmug.images.get"),
    smugmug_images_getInfo("smugmug.images.getInfo");
    
    private String name = null;
    
    SmugMugMethodsEnum(String nm)
    {
      name = nm;	
    }
    
    @Override
    public String  toString()
    {
    	return name;
    }
}
