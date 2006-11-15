/* This is a clone of TreeCacheProviderHook, changed to use OptimisticTreeCacheProvider
 * instead. Not Currently Used, but here in case we change our mind later.
  * 
  * JBoss, Home of Professional Open Source
  * Copyright 2005, JBoss Inc., and individual contributors as indicated
  * by the @authors tag. See the copyright.txt in the distribution for a
  * full listing of individual contributors.
  *
  * This is free software; you can redistribute it and/or modify it
  * under the terms of the GNU Lesser General Public License as
  * published by the Free Software Foundation; either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this software; if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
  */
package com.dumbhippo.persistence;

import java.util.Properties;

import javax.management.ObjectName;

import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.OptimisticTreeCache;
import org.jboss.cache.TreeCacheMBean;
import org.jboss.mx.util.MBeanProxyExt;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * Support for a standalone JBossCache (TreeCache) instance.  The JBossCache is configured
 * via a local config resource.
 *
 * @author Gavin King
 */
public class OptimisticTreeCacheProviderHook implements CacheProvider
{

   private org.jboss.cache.TreeCache cache;

   /**
    * Construct and configure the Cache representation of a named cache region.
    *
    * @param regionName the name of the cache region
    * @param properties configuration settings
    * @return The Cache representation of the named cache region.
    * @throws org.hibernate.cache.CacheException
    *          Indicates an error building the cache region.
    */
   public Cache buildCache(String regionName, Properties properties) throws CacheException
   {
      return new OptimisticTreeCache(cache, regionName);
   }

   public boolean isMinimalPutsEnabledByDefault()
   {
      return false;
   }

   public long nextTimestamp()
   {
      return System.currentTimeMillis() / 100;
   }

   /**
    * Prepare the underlying JBossCache TreeCache instance.
    *
    * @param properties All current config settings.
    * @throws org.hibernate.cache.CacheException
    *          Indicates a problem preparing cache for use.
    */
   public void start(Properties properties)
   {
      try
      {
         ObjectName mbeanObjectName = new ObjectName((String) properties.get("hibernate.treecache.mbean.object_name"));
         TreeCacheMBean mbean = (TreeCacheMBean) MBeanProxyExt.create(TreeCacheMBean.class, mbeanObjectName, MBeanServerLocator.locateJBoss());
         cache = mbean.getInstance();
      }
      catch (Exception e)
      {
         throw new CacheException(e);
      }
   }

   public void stop()
   {
   }

}
