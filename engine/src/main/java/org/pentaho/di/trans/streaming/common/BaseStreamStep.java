/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
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

package org.pentaho.di.trans.streaming.common;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.exception.KettleMissingPluginsException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.SubtransExecutor;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepStatus;
import org.pentaho.di.trans.steps.transexecutor.TransExecutorData;
import org.pentaho.di.trans.steps.transexecutor.TransExecutorParameters;
import org.pentaho.di.trans.streaming.api.StreamSource;
import org.pentaho.di.trans.streaming.api.StreamWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

public class BaseStreamStep extends BaseStep {

  private static final Class<?> PKG = BaseStreamStep.class;
  private BaseStreamStepMeta stepMeta;

  protected final Logger logger = LoggerFactory.getLogger( getClass() );
  protected SubtransExecutor subtransExecutor;
  protected StreamWindow<List<Object>, Result> window;
  protected StreamSource<List<Object>> source;

  public BaseStreamStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
                         TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  public boolean init( StepMetaInterface stepMetaInterface, StepDataInterface stepDataInterface ) {
    Preconditions.checkNotNull( stepMetaInterface );
    stepMeta = (BaseStreamStepMeta) stepMetaInterface;

    String ktr = getFilePath( stepMeta.getTransformationPath() );

    try {
      subtransExecutor = new SubtransExecutor(
        getTrans(), new TransMeta( ktr ), true,
        new TransExecutorData(), new TransExecutorParameters() );

    } catch ( KettleXMLException | KettleMissingPluginsException e ) {
      logger.error( e.getLocalizedMessage(), e );
    }

    return super.init( stepMetaInterface, stepDataInterface );
  }


  @Override public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    Preconditions.checkArgument( first,
      BaseMessages.getString( PKG, "BaseStreamStep.ProcessRowsError" ) );
    Preconditions.checkNotNull( source );
    Preconditions.checkNotNull( window );

    source.open();

    bufferStream().forEach( result -> {
        if ( result.getNrErrors() > 0 ) {
          stopAll();
        } else {
          putRows( result.getRows() );
        }
      }
    );
    return false;
  }

  private Iterable<Result> bufferStream() {
    return window.buffer( source.rows() );
  }

  @Override
  public void stopRunning( StepMetaInterface stepMetaInterface, StepDataInterface stepDataInterface )
    throws KettleException {
    if ( source != null ) {
      source.close();
    }
    super.stopRunning( stepMetaInterface, stepDataInterface );
  }

  @Override public void resumeRunning() {
    if ( source != null ) {
      source.resume();
    }
    super.resumeRunning();
  }

  @Override public void pauseRunning() {
    if ( source != null ) {
      source.pause();
    }
    super.pauseRunning();
  }

  private void putRows( List<RowMetaAndData> rows ) {
    rows.forEach( row -> {
      try {
        putRow( row.getRowMeta(), row.getData() );
      } catch ( KettleStepException e ) {
        Throwables.propagate( e );
      }
    } );
  }

  private String getFilePath( String path ) {
    try {
      final FileObject fileObject = KettleVFS.getFileObject( environmentSubstitute( path ) );
      if ( !fileObject.exists() ) {
        throw new FileNotFoundException( path );
      }
      return Paths.get( fileObject.getURL().toURI() ).normalize().toString();
    } catch ( URISyntaxException | FileNotFoundException | FileSystemException | KettleFileException e ) {
      propagate( e );
    }
    return null;
  }

  protected int getBatchSize() {
    try {
      return Integer.parseInt( stepMeta.getBatchSize() );
    } catch ( NumberFormatException nfe ) {
      return 50;
    }
  }

  protected long getDuration() {
    try {
      return Long.parseLong( stepMeta.getBatchDuration() );
    } catch ( NumberFormatException nfe ) {
      return 5000l;
    }
  }

  @Override public Collection<StepStatus> subStatuses() {
    return subtransExecutor != null ? subtransExecutor.getStatuses().values() : Collections.emptyList();
  }


}
