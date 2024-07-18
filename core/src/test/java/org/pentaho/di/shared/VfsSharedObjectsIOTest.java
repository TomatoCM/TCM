package org.pentaho.di.shared;

import org.apache.commons.vfs2.FileObject;
import org.junit.Test;
import org.junit.BeforeClass;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.plugins.DatabasePluginType;
import org.pentaho.di.core.row.value.ValueMetaPluginType;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Node;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class VfsSharedObjectsIOTest {
  private static final String ROOT_FILE_PATH = "ram:///config";
  private static final String SHARED_FILE = "shared.xml";
  private static final String CONN_STR = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <sharedobjects> "
                  + " <connection> <name>postgres-docker</name> <server>localhost</server> <type>POSTGRESQL</type> "
                  +  " <access>Native</access> <database>sampledata</database> <port>5435</port> <username>postgres</username> "
                  +  " </connection> </sharedobjects>";

  @BeforeClass
  public static void setUpOnce() throws KettlePluginException, KettleException {
    // Register Natives to create a default DatabaseMeta
    DatabasePluginType.getInstance().searchPlugins();
    ValueMetaPluginType.getInstance().searchPlugins();
    KettleClientEnvironment.init();
  }

  @Test
  public void testGetSharedObjects()  throws Exception {
    // Prepare the test shared.xml
    FileObject projectDirectory = KettleVFS.getFileObject( ROOT_FILE_PATH );
    projectDirectory.createFolder();

    FileObject sharedFile = projectDirectory.resolveFile( SHARED_FILE );

    try ( OutputStream outputStream = sharedFile.getContent().getOutputStream() ) {
      outputStream.write( CONN_STR.getBytes( StandardCharsets.UTF_8 ) );
    }

    SharedObjectsIO sharedObjectsIO = new VfsSharedObjectsIO( ROOT_FILE_PATH );
    Map<String, Node> nodesMap = sharedObjectsIO.getSharedObjects( "connection" );
    assertEquals( 1, nodesMap.size() );

    //Get the key
    Map.Entry<String, Node> entry = nodesMap.entrySet().iterator().next();
    String key = entry.getKey();
    Node node = entry.getValue();
    assertEquals( "postgres-docker", key );
    validateNode( node );

    // close the file
    sharedFile.close();
  }
  private void validateNode( Node node ) {
    assertEquals( "localhost", XMLHandler.getTagValue( node, "server" ) );
    assertEquals( "POSTGRESQL", XMLHandler.getTagValue( node, "type" ) );
    assertEquals( "Native", XMLHandler.getTagValue( node, "access" ) );
    assertEquals( "5435", XMLHandler.getTagValue( node, "port" ) );
    assertEquals( "postgres", XMLHandler.getTagValue( node, "username" ) );
  }

  @Test
  public void testSaveSharedObject() throws Exception {
    FileObject projectDirectory = KettleVFS.getFileObject( ROOT_FILE_PATH );
    projectDirectory.createFolder();

    SharedObjectsIO sharedObjectsIO = new VfsSharedObjectsIO( ROOT_FILE_PATH );
    String type = String.valueOf( SharedObjectsIO.SharedObjectType.CONNECTION );
    String connectionName = "NewConn";
    // Create a new DatabaseMeta object
    DatabaseMeta databaseMeta = new DatabaseMeta( connectionName, "Infobright", "JDBC", null, "stub:stub", null, null, null );
    sharedObjectsIO.saveSharedObject( type, connectionName,
      XMLHandler.getSubNode( XMLHandler.loadXMLString( databaseMeta.getXML() ), type ) );

    // Verify that the new connection is saved
    Map<String, Node> nodesMap = sharedObjectsIO.getSharedObjects( type );
    assertEquals( 1, nodesMap.size() );

    //Get the key
    Map.Entry<String, Node> entry = nodesMap.entrySet().iterator().next();
    String key = entry.getKey();
    Node node = entry.getValue();
    assertEquals( connectionName, key );

  }
}
