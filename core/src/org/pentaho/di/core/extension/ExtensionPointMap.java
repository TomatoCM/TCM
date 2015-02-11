/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package org.pentaho.di.core.extension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginTypeListener;

/**
 * This class maintains a map of ExtensionPointInterface object to its name.
 */
public class ExtensionPointMap {

  private static ExtensionPointMap INSTANCE = new ExtensionPointMap();

  private Map<String, ExtensionPointInterface> extensionPointPluginMap;
  private static LogChannelInterface log = new LogChannel( "ExtensionPointMap" );

  private ExtensionPointMap() {
    extensionPointPluginMap = new HashMap<String, ExtensionPointInterface>();
    final PluginRegistry registry = PluginRegistry.getInstance();
    registry.addPluginListener( ExtensionPointPluginType.class, new PluginTypeListener() {

      @Override
      public void pluginAdded( Object serviceObject ) {
        addExtensionPoint( (PluginInterface) serviceObject );
      }

      @Override
      public void pluginRemoved( Object serviceObject ) {
        removeExtensionPoint( (PluginInterface) serviceObject );
      }

      @Override
      public void pluginChanged( Object serviceObject ) {
        removeExtensionPoint( (PluginInterface) serviceObject );
        addExtensionPoint( (PluginInterface) serviceObject );
      }

    } );

    List<PluginInterface> extensionPointPlugins = registry.getPlugins( ExtensionPointPluginType.class );
    for ( PluginInterface extensionPointPlugin : extensionPointPlugins ) {
      addExtensionPoint( extensionPointPlugin );
    }
  }

  public static ExtensionPointMap getInstance() {
    return INSTANCE;
  }

  /**
   * Retrieves the extension point map
   * 
   * @return
   */
  public Map<String, ExtensionPointInterface> getMap() {
    return extensionPointPluginMap;
  }

  /**
   * Add the extension point plugin to the map
   * 
   * @param extensionPointPlugin
   */
  public void addExtensionPoint( PluginInterface extensionPointPlugin ) {
    final PluginRegistry registry = PluginRegistry.getInstance();
    try {
      ExtensionPointInterface extensionPoint = (ExtensionPointInterface) registry.loadClass( extensionPointPlugin );
      addExtensionPoint( extensionPointPlugin.getName(), extensionPoint );
    } catch ( KettlePluginException e ) {
      log.logError( "Unable to load extension point for name = [" + extensionPointPlugin.getName() + "]", e );
    }
  }

  /**
   * Add the extension point plugin to the map
   * 
   * @param id
   * @param extensionPoint
   */
  private void addExtensionPoint( String id, ExtensionPointInterface extensionPoint ) {
    extensionPointPluginMap.put( id, extensionPoint );
  }

  /**
   * Remove the extension point plugin from the map
   * 
   * @param extensionPointPlugin
   */
  public void removeExtensionPoint( PluginInterface extensionPointPlugin ) {
    extensionPointPluginMap.remove( extensionPointPlugin.getName() );
  }

  /**
   * Retrieves the
   * 
   * @param id
   * @return
   */
  public ExtensionPointInterface get( String id ) {
    return extensionPointPluginMap.get( id );
  }

  /**
   * Reinitialize the extension point plugins map
   */
  public void reInitialize() {
    extensionPointPluginMap = new HashMap<String, ExtensionPointInterface>();
    final PluginRegistry registry = PluginRegistry.getInstance();
    List<PluginInterface> extensionPointPlugins = registry.getPlugins( ExtensionPointPluginType.class );
    for ( PluginInterface extensionPointPlugin : extensionPointPlugins ) {
      addExtensionPoint( extensionPointPlugin );
    }
  }
}
