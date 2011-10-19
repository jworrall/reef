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

import org.totalgrid.reef.api.proto.Model.ReefUUID
import org.totalgrid.reef.api.proto.FEP.{ CommEndpointRouting, CommEndpointConfig, CommChannel, CommEndpointConnection }
import org.totalgrid.reef.util.Cancelable
import org.totalgrid.reef.app.SubscriptionHandler
import org.totalgrid.reef.executor.Executor
import org.totalgrid.reef.api.sapi.client.rest.SubscriptionResult
import org.totalgrid.reef.api.japi.client.SubscriptionEventAcceptor
import net.agileautomata.executor4s.testing.MockFuture
import org.totalgrid.reef.api.japi.ReefServiceException
import org.totalgrid.reef.api.sapi.client._

object FrontEndTestHelpers {

  private def makeUuid(str: String) = ReefUUID.newBuilder.setUuid(str).build
  implicit def makeUuidFromString(str: String): ReefUUID = makeUuid(str)

  def getConnectionProto(enabled: Boolean, routingKey: Option[String]) = {
    val pt = CommChannel.newBuilder.setUuid("port").setName("port")
    val cfg = CommEndpointConfig.newBuilder.setProtocol("mock").setUuid("config").setChannel(pt).setName("endpoint1")
    val b = CommEndpointConnection.newBuilder.setUid("connection").setEndpoint(cfg).setEnabled(enabled)
    routingKey.foreach { s => b.setRouting(CommEndpointRouting.newBuilder.setServiceRoutingKey(s).build) }
    b.build
  }

  class MockSubscriptionHandler[A] extends SubscriptionHandler[A] with Cancelable {
    var sub = Option.empty[SubscriptionResult[List[A], A]]
    var canceled = false

    def setSubscription(result: SubscriptionResult[List[A], A], executor: Executor) = {
      sub = Some(result)
      this
    }
    def cancel() = {
      canceled = true
    }
  }

  class MockCancelable extends Cancelable {
    var canceled = false
    def cancel() = canceled = true
  }

  class MockSubscription[A](val id: String = "queue") extends Subscription[A] {
    var canceled = false
    var acceptor = Option.empty[Event[A] => Unit]
    def start(acc: Event[A] => Unit) = {
      acceptor = Some(acc)
      this
    }

    def cancel() = canceled = true
  }

  class MockSubscriptionResult[A](result: List[A], val mockSub: MockSubscription[A]) extends SubscriptionResult[List[A], A](result, mockSub) {

    def this(one: A) = this(one :: Nil, new MockSubscription[A]())
    def this(many: List[A]) = this(many, new MockSubscription[A]())
  }

  // TODO: fix promise and futures to be interfaces
  //  class ThrowsPromise[A <: ReefServiceException, B](ex: A) extends Promise(new MockFuture(Some(FailureResponse(ex.getStatus, ex.getMessage))))
  //
  //  class FixedPromise[A](result: A) extends Promise(new MockFuture(Some(SuccessResponse(list = result))))
}