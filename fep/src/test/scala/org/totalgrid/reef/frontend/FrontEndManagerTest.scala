/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the GNU Affero General Public License
 * Version 3.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.frontend

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito

import org.totalgrid.reef.client.service.proto.Application.ApplicationConfig
import org.totalgrid.reef.test.MockitoStubbedOnly
import org.totalgrid.reef.client.service.proto.Model.ReefUUID
import org.totalgrid.reef.client.service.proto.FEP.{ EndpointConnection, FrontEndProcessor }
import org.totalgrid.reef.client.exception.BadRequestException

import org.totalgrid.reef.client.sapi.client.ServiceTestHelpers._
import FrontEndTestHelpers._

import net.agileautomata.executor4s._
import net.agileautomata.executor4s.testing.MockExecutor
import org.totalgrid.reef.client.{ Promise, SubscriptionResult }

@RunWith(classOf[JUnitRunner])
class FrontEndManagerTest extends FunSuite with ShouldMatchers {

  val applicationUuid: ReefUUID = "0"
  val protocolList = List("mock")

  def fixture(services: FrontEndProviderServices, autoStart: Boolean = true)(test: (FrontEndProviderServices, MockExecutor, MockSubscriptionHandler[EndpointConnection], FrontEndManager) => Unit) = {

    val exe = new MockExecutor

    val mp = new MockSubscriptionHandler[EndpointConnection]
    val appConfig = ApplicationConfig.newBuilder.setUuid(applicationUuid).build
    val fem = new FrontEndManager(services, exe, mp, appConfig, protocolList, 5000)

    if (autoStart) fem.start()
    test(services, exe, mp, fem)
  }

  def responses(fepResult: => Promise[FrontEndProcessor], subResult: => Promise[SubscriptionResult[List[EndpointConnection], EndpointConnection]]) = {
    val services = Mockito.mock(classOf[FrontEndProviderServices], new MockitoStubbedOnly)

    Mockito.doReturn(fepResult).when(services).registerApplicationAsFrontEnd(applicationUuid, protocolList)
    Mockito.doReturn(subResult).when(services).subscribeToEndpointConnectionsForFrontEnd(fepResult.await)

    services
  }

  test("Announces on startup") {
    val fep = success(FrontEndProcessor.newBuilder.setUuid("someuid").build)
    val sub = subSuccessInterface[EndpointConnection](Nil)
    fixture(responses(fep, sub), false) { (services, exe, mp, fem) =>
      fem.start()
      mp.sub should equal(Some(sub.await))
      mp.canceled should equal(false)
      fem.stop()
      mp.canceled should equal(true)
    }
  }

  test("Retries announces with executor delay") {
    val fep = success(FrontEndProcessor.newBuilder.setUuid("someuid").build)
    val sub = subFailureInterface(new BadRequestException("Intentional Failure")).asInstanceOf[Promise[SubscriptionResult[List[EndpointConnection], EndpointConnection]]] // TODO: fix type problems
    fixture(responses(fep, sub), false) { (services, exe, mp, fem) =>
      fem.start()
      mp.sub should equal(None)
      (0 to 3).foreach { i =>
        exe.numQueuedTimers should equal(1)
        exe.tick(5000.milliseconds)
        exe.numQueuedTimers should equal(1)
      }
    }
  }
}