/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2022 by Hitachi Vantara : http://www.pentaho.com
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
package org.pentaho.di.vfs.connections.ui.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.connections.ConnectionDetails;
import org.pentaho.di.connections.ConnectionManager;
import org.pentaho.di.connections.ui.tree.ConnectionFolderProvider;
import org.pentaho.di.connections.vfs.VFSDetailsComposite;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.vfs.connections.ui.dialog.VFSDetailsCompositeHelper;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.pentaho.di.core.util.Utils.isEmpty;

/**
 * Created by tkafalas
 */
public class ConnectionDialog extends Dialog {
  private static final Class<?> PKG = ConnectionDialog.class; // for i18n purposes, needed by Translator2!!

  private static final Image LOGO = GUIResource.getInstance().getImageLogoSmall();
  private static final int MARGIN = Const.MARGIN;
  private static final ConnectionManager connectionManager = ConnectionManager.getInstance();
  private static final int TEXT_VAR_FLAGS = SWT.SINGLE | SWT.LEFT | SWT.BORDER;

  Supplier<Spoon> spoonSupplier = Spoon::getInstance;
  private String connectionTypeKey;
  private ConnectionDetails connectionDetails;
  private final List<ConnectionManager.Type> connectionTypes;
  private final String[] connectionTypeChoices;

  private final PropsUI props;
  private final int width;
  private final int height;

  private Shell shell;
  private String connectionName;
  private CTabFolder wTabFolder;
  private CTabItem wConnectionTypeTab;
  private VFSDetailsComposite vfsDetailsComposite;
  private Composite wConnectionTypeComp;
  private VFSDetailsCompositeHelper helper;
  private Text wName;
  private Text wDescription;
  private CCombo wConnectionType;

  private Composite wDetailsWrapperComp;
  private ScrolledComposite wScrolledComposite;

  public ConnectionDialog( Shell shell, int width, int height ) {
    super( shell, SWT.NONE );
    this.width = width;
    this.height = height;
    props = PropsUI.getInstance();
    connectionTypes = connectionManager.getItems();
    connectionTypeChoices =
      connectionTypes.stream().map( ConnectionManager.Type::getLabel ).sorted().toArray( String[]::new );
    helper = new VFSDetailsCompositeHelper( PKG, props );
  }

  //This open called for a new connection
  public void open( String title ) {
    open( title, null );
  }

  //This open called for an existing connection
  public void open( String title, String existingConnectionName ) {
    this.connectionName = existingConnectionName;
    Shell parent = getParent();
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    shell.setSize( width, height );
    props.setLook( shell );
    shell.setImage( LOGO );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setText( title );
    shell.setLayout( formLayout );

    // First, add the buttons...
    // Buttons
    Button wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );

    Button wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    Button wTest = new Button( shell, SWT.PUSH );
    wTest.setText( BaseMessages.getString( PKG, "System.Button.Test" ) );

    Button[] buttons = new Button[] { wOK, wCancel, wTest };
    BaseStepDialog.positionBottomButtons( shell, buttons, MARGIN, null );

    // Add listeners
    wOK.addListener( SWT.Selection, e -> ok() );
    wCancel.addListener( SWT.Selection, e -> cancel() );
    wTest.addListener( SWT.Selection, e -> test() );

    // The rest stays above the buttons...

    wTabFolder = new CTabFolder( shell, SWT.BORDER );
    props.setLook( wTabFolder, Props.WIDGET_STYLE_TAB );

    FormData fdTabFolder = new FormData();
    fdTabFolder.top = new FormAttachment( 0, MARGIN );
    fdTabFolder.left = new FormAttachment( 0, 0 );
    fdTabFolder.right = new FormAttachment( 100, 0 );
    fdTabFolder.bottom = new FormAttachment( wCancel, -MARGIN );
    wTabFolder.setLayoutData( fdTabFolder );

    addConnectionTypeTab();

    wConnectionTypeTab.setControl( wConnectionTypeComp );
    wTabFolder.setSelection( 0 );


    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      @Override
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    shell.open();
    while ( !shell.isDisposed() ) {
      Display display = parent.getDisplay();
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
  }

  private void addConnectionTypeTab() {
    wConnectionTypeTab = new CTabItem( wTabFolder, SWT.NONE );
    wConnectionTypeTab.setText( BaseMessages.getString( PKG, "ConnectionDialog.tabs.ConnectionType.Title" ) );

    wConnectionTypeComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wConnectionTypeComp );

    FormLayout genLayout = new FormLayout();
    genLayout.marginWidth = Const.FORM_MARGIN;
    genLayout.marginHeight = Const.FORM_MARGIN;
    wConnectionTypeComp.setLayout( genLayout );

    // Connection Name
    createLabel( "ConnectionDialog.ConnectionName.Label", null );
    wName = createText( null );

    // Connection Type
    createLabel( "ConnectionDialog.ConnectionType.Label", wName );
    wConnectionType = createCCombo( wName, 200 );
    wConnectionType.setItems( connectionTypeChoices );
    wConnectionType.select( 0 );

    //Description
    createLabel( "ConnectionDialog.Description.Label", wConnectionType );
    wDescription = createMultilineText( wConnectionType );
    ( (FormData) wDescription.getLayoutData() ).height = wDescription.computeSize( SWT.DEFAULT, SWT.DEFAULT ).y * 3;

    setConnectionType();
    populateWidgets();

    //add listeners
    wName.addModifyListener( modifyEvent -> setName( wName.getText() ) );
    wConnectionType.addSelectionListener( new SelectionAdapter() {
      @Override public void widgetSelected( SelectionEvent selectionEvent ) {
        String connectionTypeSelected = wConnectionType.getText();
        updateConnectionType( convertTypeLabelToKey( connectionTypeSelected ) );
      }
    } );
    wDescription.addModifyListener( modifyEvent -> connectionDetails.setDescription( wDescription.getText() ) );
  }

  private void setConnectionType() { // When first loaded
    if ( connectionName != null ) {
      connectionDetails = connectionManager.getConnectionDetails( connectionName );
      if ( connectionDetails != null ) {
        connectionTypeKey = connectionDetails.getType();
      }
      return;
    }
    connectionTypeKey = convertTypeLabelToKey( connectionTypeChoices[ 0 ] );
    connectionDetails = connectionManager.createConnectionDetails( connectionTypeKey );
  }

  private void updateConnectionType( String connectionType ) {
    if ( vfsDetailsComposite != null && connectionDetails != null ) {
      connectionDetails.closeDialog();
      vfsDetailsComposite = null;
    } else {
      if ( wDetailsWrapperComp != null && !wDetailsWrapperComp.isDisposed() ) {
        wDetailsWrapperComp.dispose();
        wDetailsWrapperComp = null;
      }
    }
    if ( wScrolledComposite != null && !wScrolledComposite.isDisposed() ) {
      wScrolledComposite.dispose();
    }
    if ( connectionDetails != null && !connectionDetails.getType().equals( connectionType ) ) {
      ConnectionDetails newConnectionDetails = connectionManager.createConnectionDetails( connectionType );
      transferFieldsFromOldToNew( connectionDetails, newConnectionDetails );
      connectionDetails = newConnectionDetails;
    }

    wScrolledComposite = new ScrolledComposite( wConnectionTypeComp, SWT.BORDER + SWT.V_SCROLL );
    FormData fdScrolledComp = new FormData();
    fdScrolledComp.top = new FormAttachment( wDescription, MARGIN * 2 );
    fdScrolledComp.left = new FormAttachment( 0, 0 );
    fdScrolledComp.right = new FormAttachment( 100, 0 );
    fdScrolledComp.bottom = new FormAttachment( 100, 0 );
    wScrolledComposite.setLayoutData( fdScrolledComp );

    HashSet<Control> skipControls = new HashSet<>();
    skipControls.add( wScrolledComposite ); //We don't to adjust this composite
    helper.adjustPlacement( wConnectionTypeComp, skipControls );

    // This composite will contain all the widgets specific to the connection type.  The wScrolledComposite will
    // let it scroll up/down.
    wDetailsWrapperComp = new Composite( wScrolledComposite, SWT.NONE );
    props.setLook( wDetailsWrapperComp );

    //Populate all the widgets associated with the chosen connection type
    if ( connectionDetails != null ) {
      vfsDetailsComposite = (VFSDetailsComposite) connectionDetails.openDialog( wDetailsWrapperComp, props );
    }

    wConnectionTypeComp.layout();

    wDetailsWrapperComp.pack();
    wScrolledComposite.setMinSize( wDetailsWrapperComp.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
    wScrolledComposite.setContent( wDetailsWrapperComp );
    wScrolledComposite.setExpandHorizontal( true );
    wScrolledComposite.setExpandVertical( true );
  }

  private void transferFieldsFromOldToNew( ConnectionDetails connectionDetails,
                                           ConnectionDetails newConnectionDetails ) {
    newConnectionDetails.setName( connectionDetails.getName() );
    newConnectionDetails.setDescription( connectionDetails.getDescription() );
  }

  private void populateWidgets() {
    wConnectionType.select( getIndexOfKey( connectionTypeKey ) );
    updateConnectionType( connectionTypeKey );
    wName.setText( Const.NVL( connectionDetails.getName(), "" ) );
    wDescription.setText( Const.NVL( connectionDetails.getDescription(), "" ) );
  }

  private String convertTypeLabelToKey( String typeLabel ) {
    Optional<ConnectionManager.Type> s =
      connectionTypes.stream().filter( x -> x.getLabel().equals( typeLabel ) ).findFirst();
    if ( s.isPresent() ) {
      return s.get().getValue();
    }
    return connectionTypes.get( 0 ).getValue();  //Only kicks in if typeLabel is invalid
  }

  private String convertKeyToTypeLabel( String key ) {
    Optional<ConnectionManager.Type> s = connectionTypes.stream().filter( x -> x.getValue().equals( key ) ).findFirst();
    if ( s.isPresent() ) {
      return s.get().getLabel();
    }
    return connectionTypes.get( 0 ).getLabel();  //Only kicks in if key is invalid
  }

  private int getIndexOfKey( String key ) {
    String typeLabel = convertKeyToTypeLabel( key );
    return IntStream.range( 0, connectionTypeChoices.length )
      .filter( i -> connectionTypeChoices[ i ].equals( typeLabel ) )
      .findFirst().orElse( -1 );
  }

  private void setName( String name ) {
    connectionDetails.setName( name );
    connectionName = name;
  }

  private void cancel() {
    dispose();
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  private void ok() {
    if ( validateEntries() && connectionManager.save( connectionDetails ) ) {
      refreshMenu();
      dispose();
    }
  }

  private void refreshMenu() {
    spoonSupplier.get().getShell().getDisplay().asyncExec( () -> spoonSupplier.get().refreshTree(
      ConnectionFolderProvider.STRING_VFS_CONNECTIONS ) );
    EngineMetaInterface engineMetaInterface = spoonSupplier.get().getActiveMeta();
    if ( engineMetaInterface instanceof AbstractMeta ) {
      ( (AbstractMeta) engineMetaInterface ).setChanged();
    }
  }

  private boolean validateEntries() {
    String validationMessage = computeValidateMessage();
    if ( !isEmpty( validationMessage ) ) {
      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
      mb.setMessage( validationMessage );
      mb.open();
      return false;
    }
    return true;
  }

  private String computeValidateMessage() {
    if ( connectionDetails == null ) {
      return BaseMessages.getString( PKG, "ConnectionDialog.validate.failure.noDetailsObjectPresent" );
    }
    if ( isEmpty( connectionDetails.getName() ) ) {
      return BaseMessages.getString( PKG, "ConnectionDialog.validate.failure.noName" );
    }
    return vfsDetailsComposite.validate();
  }

  private void test() {
    if ( validateEntries() ) {
      boolean result = connectionManager.test( connectionDetails );
      MessageBox mb;
      if ( !result ) {
        mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
        mb.setMessage( BaseMessages.getString( PKG, "ConnectionDialog.test.failure" ) );
      } else {
        mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
        mb.setMessage( BaseMessages.getString( PKG, "ConnectionDialog.test.success" ) );
      }
      mb.open();
    }
  }

  private void createLabel( String key, Control topWidget ) {
    helper.createLabel( wConnectionTypeComp, SWT.RIGHT | SWT.WRAP, key, topWidget );
  }

  private Text createText( Control topWidget ) {
    return helper.createText( wConnectionTypeComp, TEXT_VAR_FLAGS, topWidget, 0 );
  }

  private Text createMultilineText( Control topWidget ) {
    return helper.createText( wConnectionTypeComp, SWT.MULTI | SWT.LEFT | SWT.BORDER | SWT.V_SCROLL, topWidget, 0 );
  }

  private CCombo createCCombo( Control topWidget, int width ) {
    return helper.createCCombo( wConnectionTypeComp, TEXT_VAR_FLAGS, topWidget, width );
  }
}
