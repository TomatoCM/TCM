/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2024 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.ui.spoon.delegates;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.dnd.DragAndDropContainer;
import org.pentaho.di.core.dnd.XMLTransfer;
import org.pentaho.di.core.extension.ExtensionPointHandler;
import org.pentaho.di.core.extension.KettleExtensionPoint;
import org.pentaho.di.core.plugins.JobEntryPluginType;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.TreeSelection;

public class SpoonTreeDelegate extends SpoonDelegate {
  public SpoonTreeDelegate( Spoon spoon ) {
    super( spoon );
  }

  /**
   * @return The object that is selected in the tree or null if we couldn't figure it out. (titles etc. == null)
   */
  public TreeSelection[] getTreeObjects( final Tree tree, Tree selectionTree, Tree coreObjectsTree ) {
    List<TreeSelection> objects = new ArrayList<TreeSelection>();

    if ( selectionTree != null && !selectionTree.isDisposed() && tree.equals( selectionTree ) ) {
      TreeItem[] selection = selectionTree.getSelection();
      for ( int s = 0; s < selection.length; s++ ) {
        TreeItem treeItem = selection[s];
        String[] path = ConstUI.getTreeStrings( treeItem );

        TreeSelection object = null;

        switch ( path.length ) {
          case 0:
            break;
          case 1: // ------complete-----
            if ( path[0].equals( Spoon.STRING_CONFIGURATIONS ) ) { // the top level config entry

              // nothing to do by selecting the Configurations node.
            }
            break;

          case 2: // ------complete-----
            if ( path[0].equals( Spoon.STRING_CONFIGURATIONS ) ) {
              if ( path[1].equals( Spoon.STRING_CONNECTIONS ) ) {
                object = new TreeSelection( path[1], DatabaseMeta.class );
              }
              if ( path[1].equals( Spoon.STRING_PARTITIONS ) ) {
                object = new TreeSelection( path[1], PartitionSchema.class );
              }
              if ( path[1].equals( Spoon.STRING_SLAVES ) ) {
                object = new TreeSelection( path[1], SlaveServer.class );
              }
              if ( path[1].equals( Spoon.STRING_CLUSTERS ) ) {
                object = new TreeSelection( path[1], ClusterSchema.class );
              }
              executeExtensionPoint( new SpoonTreeDelegateExtension( null, path, 2, objects ) );
            }
            break;

          case 3: // ------complete-----
            if ( path[0].equals( Spoon.STRING_CONFIGURATIONS ) ) {

              // TODO BACKLOG-??? edit and delete for databases
              // if ( path[1].equals( Spoon.STRING_CONNECTIONS ) ) {
              //   String dbName = path[2];
              //   DatabaseMeta databaseMeta = transMeta.findDatabase( dbName );
              //   if ( databaseMeta != null ) {
              //     dbName = databaseMeta.getName();
              //   }
              //
              //   object = new TreeSelection( dbName, databaseMeta );
              // }
              // if ( path[1].equals( Spoon.STRING_PARTITIONS ) ) {
              //   object = new TreeSelection( path[2], transMeta.findPartitionSchema( path[2] ) );
              // }
              // if ( path[1].equals( Spoon.STRING_SLAVES ) ) {
              //   object = new TreeSelection( path[2], transMeta.findSlaveServer( path[2] ) );
              // }
              // if ( path[1].equals( Spoon.STRING_CLUSTERS ) ) {
              //   object = new TreeSelection( path[2], transMeta.findClusterSchema( path[2] ) );
              // }
              executeExtensionPoint( new SpoonTreeDelegateExtension( null, path, 3, objects ) );
            }
            break;

          case 4:
            if ( path[0].equals( Spoon.STRING_CONFIGURATIONS ) ) {

              // TODO BACKLOG-??? edit and delete for clusters
              //if ( path[1].equals( Spoon.STRING_CLUSTERS ) ) {
              //  ClusterSchema clusterSchema = transMeta.findClusterSchema( path[2] );
              //  object =
              //    new TreeSelection( path[3], clusterSchema.findSlaveServer( path[3]), clusterSchema, transMeta );
              //}
            }
            break;
          default:
            break;
        }

        if ( object != null ) {
          objects.add( object );
        }
      }
    }
    if ( tree != null && coreObjectsTree != null && tree.equals( coreObjectsTree ) ) {
      TreeItem[] selection = coreObjectsTree.getSelection();
      for ( int s = 0; s < selection.length; s++ ) {
        TreeItem treeItem = selection[s];
        String[] path = ConstUI.getTreeStrings( treeItem );

        TreeSelection object = null;

        switch ( path.length ) {
          case 0:
            break;
          case 2: // Job entries
            if ( spoon.showJob ) {
              PluginRegistry registry = PluginRegistry.getInstance();
              Class<? extends PluginTypeInterface> pluginType = JobEntryPluginType.class;
              PluginInterface plugin = registry.findPluginWithName( pluginType, path[1] );

              // Retry for Start
              //
              if ( plugin == null ) {
                if ( path[1].equalsIgnoreCase( JobMeta.STRING_SPECIAL_START ) ) {
                  plugin = registry.findPluginWithId( pluginType, JobMeta.STRING_SPECIAL );
                }
              }
              // Retry for Dummy
              //
              if ( plugin == null ) {
                if ( path[1].equalsIgnoreCase( JobMeta.STRING_SPECIAL_DUMMY ) ) {
                  plugin = registry.findPluginWithId( pluginType, JobMeta.STRING_SPECIAL );
                }
              }

              if ( plugin != null ) {
                object = new TreeSelection( path[1], plugin );
              }
            }

            if ( spoon.showTrans ) {
              String stepId = (String) treeItem.getData( "StepId" );

              if ( stepId != null ) {
                object = new TreeSelection( path[1], PluginRegistry.getInstance().findPluginWithId( StepPluginType.class, stepId ) );
              } else {
                object = new TreeSelection( path[1], PluginRegistry.getInstance().findPluginWithName( StepPluginType.class, path[1] ) );
              }
            }
            break;
          default:
            break;
        }

        if ( object != null ) {
          objects.add( object );
        }
      }
    }

    return objects.toArray( new TreeSelection[objects.size()] );
  }

  public void addDragSourceToTree( final Tree tree, final Tree selectionTree, final Tree coreObjectsTree ) {
    // Drag & Drop for steps
    Transfer[] ttypes = new Transfer[] { XMLTransfer.getInstance() };

    DragSource ddSource = new DragSource( tree, DND.DROP_MOVE );
    ddSource.setTransfer( ttypes );
    ddSource.addDragListener( new DragSourceListener() {
      public void dragStart( DragSourceEvent event ) {
        TreeSelection[] treeObjects = getTreeObjects( tree, selectionTree, coreObjectsTree );
        if ( treeObjects.length == 0 ) {
          event.doit = false;
          return;
        }

        spoon.hideToolTips();

        TreeSelection treeObject = treeObjects[0];
        Object object = treeObject.getSelection();
        TransMeta transMeta = spoon.getActiveTransformation();
        // JobMeta jobMeta = spoon.getActiveJob();

        if ( object instanceof StepMeta
          || object instanceof PluginInterface || ( object instanceof DatabaseMeta && transMeta != null )
          || object instanceof TransHopMeta || object instanceof JobEntryCopy ) {
          event.doit = true;
        } else {
          event.doit = false;
        }
      }

      public void dragSetData( DragSourceEvent event ) {
        TreeSelection[] treeObjects = getTreeObjects( tree, selectionTree, coreObjectsTree );
        if ( treeObjects.length == 0 ) {
          event.doit = false;
          return;
        }

        int type = 0;
        String id = null;
        String data = null;

        TreeSelection treeObject = treeObjects[0];
        Object object = treeObject.getSelection();

        if ( object instanceof StepMeta ) {
          StepMeta stepMeta = (StepMeta) object;
          type = DragAndDropContainer.TYPE_STEP;
          data = stepMeta.getName(); // name of the step.
        } else if ( object instanceof PluginInterface ) {
          PluginInterface plugin = (PluginInterface) object;
          Class<? extends PluginTypeInterface> pluginType = plugin.getPluginType();
          if ( Const.classIsOrExtends( pluginType, StepPluginType.class ) ) {
            type = DragAndDropContainer.TYPE_BASE_STEP_TYPE;
            id = plugin.getIds()[ 0 ];
            data = plugin.getName(); // Step type name
          } else {
            type = DragAndDropContainer.TYPE_BASE_JOB_ENTRY;
            data = plugin.getName(); // job entry type name
            if ( treeObject.getItemText().equals( JobMeta.createStartEntry().getName() ) ) {
              data = treeObject.getItemText();
            } else if ( treeObject.getItemText().equals( JobMeta.createDummyEntry().getName() ) ) {
              data = treeObject.getItemText();
            }
          }
        } else if ( object instanceof DatabaseMeta ) {
          DatabaseMeta databaseMeta = (DatabaseMeta) object;
          type = DragAndDropContainer.TYPE_DATABASE_CONNECTION;
          data = databaseMeta.getName();
        } else if ( object instanceof TransHopMeta ) {
          TransHopMeta hop = (TransHopMeta) object;
          type = DragAndDropContainer.TYPE_TRANS_HOP;
          data = hop.toString(); // nothing for really ;-)
        } else if ( object instanceof JobEntryCopy ) {
          JobEntryCopy jobEntryCopy = (JobEntryCopy) object;
          type = DragAndDropContainer.TYPE_JOB_ENTRY;
          data = jobEntryCopy.getName(); // name of the job entry.
        } else {
          event.doit = false;
          return; // ignore anything else you drag.
        }

        event.data = new DragAndDropContainer( type, data, id );
      }

      public void dragFinished( DragSourceEvent event ) {
      }
    } );

  }

  private void executeExtensionPoint( SpoonTreeDelegateExtension extension ) {
    try {
      ExtensionPointHandler
          .callExtensionPoint( log, KettleExtensionPoint.SpoonTreeDelegateExtension.id, extension );
    } catch ( Exception e ) {
      log.logError( "Error handling SpoonTreeDelegate through extension point", e );
    }
  }

}
