package org.pentaho.di.engine.kettlenative.impl;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleMissingPluginsException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.engine.api.ITransformation;
import org.pentaho.di.trans.TransMeta;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TransformationTest {

  TransMeta testMeta;

  @Before
  public void before() throws KettleException {
    KettleEnvironment.init();
    testMeta = new TransMeta( getClass().getClassLoader().getResource( "lorem.ktr" ).getFile() );
  }

  @Test
  public void testConversion() throws KettleXMLException, KettleMissingPluginsException {
    TransMeta meta = new TransMeta( getClass().getClassLoader().getResource( "lorem.ktr" ).getFile() );
    ITransformation trans = Transformation.convert( meta );
    assertThat( trans.getOperations().size(), is( 7 ) );
    assertThat( trans.getSourceOperations().size(), is( 1 ) );
    assertThat( trans.getSinkOperations().size(), is( 1 ) );
  }

}