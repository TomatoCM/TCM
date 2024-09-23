/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2024 - 2024 by Hitachi Vantara : http://www.pentaho.com
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
package org.pentaho.di.www;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.junit.rules.RestorePDIEngineEnvironment;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.*;

/**
 * Created by ccaspanello on 5/31/2016.
 */
public class CarteTest {
  @ClassRule public static RestorePDIEngineEnvironment env = new RestorePDIEngineEnvironment();

  private MockedStatic<ClientBuilder> clientBuilderMockedStatic;

  @Before
  public void setup() {
    clientBuilderMockedStatic = mockStatic( ClientBuilder.class );
  }

  @After
  public void tearDown() {
    clientBuilderMockedStatic.close();
  }

  @Ignore( "This test isn't consistent/doesn't work." )
  @Test
  public void test() throws Exception {

    CountDownLatch latch = new CountDownLatch( 1 );

    System.setProperty( Const.KETTLE_SLAVE_DETECTION_TIMER, "100" );

    SlaveServer master = new SlaveServer();
    master.setHostname( "127.0.0.1" );
    master.setPort( "9000" );
    master.setUsername( "cluster" );
    master.setPassword( "cluster" );
    master.setMaster( true );

    SlaveServerConfig config = new SlaveServerConfig();
    config.setSlaveServer( master );

    Carte carte = new Carte( config );

    SlaveServerDetection slaveServerDetection = mock( SlaveServerDetection.class );
    carte.getWebServer().getDetections().add( slaveServerDetection );

    SlaveServer slaveServer = mock( SlaveServer.class, RETURNS_MOCKS );
    when( slaveServerDetection.getSlaveServer() ).thenReturn( slaveServer );
    when( slaveServer.getStatus() ).thenAnswer((Answer<SlaveServerStatus>) invocation -> {
      SlaveServerDetection anotherDetection = mock( SlaveServerDetection.class );
      carte.getWebServer().getDetections().add( anotherDetection );
      latch.countDown();
      return new SlaveServerStatus();
    });

    latch.await( 10, TimeUnit.SECONDS );
    assertEquals( carte.getWebServer().getDetections().size(), 2 );
    carte.getWebServer().stopServer();
  }

  @Test
  public void callStopCarteRestService() throws Exception {

    WebTarget status = mock( WebTarget.class );
    Invocation.Builder statusBuilder = mock( Invocation.Builder.class );
    when( status.request() ).thenReturn( statusBuilder );
    when( statusBuilder.get( String.class ) ).thenReturn( "<serverstatus>" );

    WebTarget stop = mock( WebTarget.class );
    Invocation.Builder stopBuilder = mock( Invocation.Builder.class );
    when( stop.request() ).thenReturn( stopBuilder );
    when( stopBuilder.get( String.class ) ).thenReturn( "Shutting Down" );

    Client client = mock( Client.class );

    when( ClientBuilder.newClient() ).thenReturn( client );

    when( client.target( "http://localhost:8080/kettle/status/?xml=Y" ) ).thenReturn( status );
    when( client.target( "http://localhost:8080/kettle/stopCarte" ) ).thenReturn( stop );

    Carte.callStopCarteRestService( "localhost", "8080", "admin", "Encrypted 2be98afc86aa7f2e4bb18bd63c99dbdde" );

    verify( client, times( 1 ) ).register( any( HttpAuthenticationFeature.class ) );
  }
}
