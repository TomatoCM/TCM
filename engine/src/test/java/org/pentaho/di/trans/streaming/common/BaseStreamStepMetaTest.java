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

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BaseStreamStepMetaTest {

  private BaseStreamStepMeta meta;

  @Before
  public void setUp() throws Exception {
    meta = new BaseStreamStepMeta() {
      @Override
      public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
                                    TransMeta transMeta,
                                    Trans trans ) {
        return null;
      }

      @Override public StepDataInterface getStepData() {
        return null;
      }
    };
  }

  @Test
  public void testCheckErrorsOnZeroSizeAndDuration() {
    meta.setBatchDuration( "0" );
    meta.setBatchSize( "0" );
    ArrayList<CheckResultInterface> remarks = new ArrayList<>();
    meta.check( remarks, null, null, null, null, null, null, new Variables(), null, null );
    assertEquals( 1, remarks.size() );
    assertEquals(
      "The \"Number of records\" and \"Duration\" fields can’t both be set to 0. Please set a value of 1 or higher for one of the fields.",
      remarks.get( 0 ).getText() );
  }

  @Test
  public void testCheckErrorsOnNaN() throws Exception {
    List<CheckResultInterface> remarks = new ArrayList<>();
    meta.setBatchDuration( "blah" );
    meta.setBatchSize( "blah" );
    meta.check( remarks, null, null, null, null, null, null, new Variables(), null, null );
    assertEquals( 2, remarks.size() );
    assertEquals( CheckResultInterface.TYPE_RESULT_ERROR, remarks.get( 0 ).getType() );
    assertEquals( "The \"Duration\" field is using a non-numeric value. Please set a numeric value.",
      remarks.get( 0 ).getText() );
    assertEquals( CheckResultInterface.TYPE_RESULT_ERROR, remarks.get( 1 ).getType() );
    assertEquals( "The \"Number of records\" field is using a non-numeric value. Please set a numeric value.",
      remarks.get( 1 ).getText() );
  }

  @Test
  public void testCheckErrorsOnVariables() {
    List<CheckResultInterface> remarks = new ArrayList<>();
    Variables space = new Variables();
    space.setVariable( "something", "1000" );
    meta.setBatchSize( "${something}" );
    meta.setBatchDuration( "0" );
    meta.check( remarks, null, null, null, null, null, null, space, null, null );
    assertEquals( 0, remarks.size() );
  }

  @Test
  public void testCheckErrorsOnVariablesSubstituteError() {
    List<CheckResultInterface> remarks = new ArrayList<>();
    Variables space = new Variables();
    space.setVariable( "something", "0" );
    meta.setBatchSize( "${something}" );
    meta.setBatchDuration( "${something}" );
    meta.check( remarks, null, null, null, null, null, null, space, null, null );
    assertEquals( 1, remarks.size() );
    assertEquals( "The \"Number of records\" and \"Duration\" fields can’t both be set to 0. Please set a value of 1 "
      + "or higher for one of the fields.", remarks.get( 0 ).getText() );
  }
}
