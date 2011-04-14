/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Green Energy Corp licenses this file
 * to you under the GNU Affero General Public License Version 3.0
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/agpl.html
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.frontend

import org.totalgrid.reef.proto.{ Commands, Measurements }
import org.totalgrid.reef.proto.FEP.{ CommEndpointConnection => ConnProto }
import org.totalgrid.reef.proto.FEP.CommChannel
import org.totalgrid.reef.messaging.Connection
import org.totalgrid.reef.api.scalaclient.ClientSession

import scala.collection.JavaConversions._
import org.totalgrid.reef.util.Conversion.convertIterableToMapified

import org.totalgrid.reef.protocol.api.{ IProtocol, IPublisher, ICommandHandler, IResponseHandler, IChannelListener, IEndpointListener }
import org.totalgrid.reef.api._

// Data structure for handling the life cycle of connections
class FrontEndConnections(comms: Seq[IProtocol], conn: Connection) extends KeyedMap[ConnProto] {

  def getKey(c: ConnProto) = c.getUid

  val protocols = comms.mapify { _.name }

  val maxAttemptsToRetryMeasurements = 1

  private def getProtocol(name: String): IProtocol = protocols.get(name) match {
    case Some(p) => p
    case None => throw new IllegalArgumentException("Unknown protocol: " + name)
  }

  def hasChangedEnoughForReload(updated: ConnProto, existing: ConnProto) = {
    updated.hasRouting != existing.hasRouting ||
      (updated.hasRouting && updated.getRouting.getServiceRoutingKey != existing.getRouting.getServiceRoutingKey)
  }

  def addEntry(c: ConnProto) = {

    val protocol = getProtocol(c.getEndpoint.getProtocol)
    val endpoint = c.getEndpoint
    val port = c.getEndpoint.getChannel

    val publisher = newPublisher(c.getRouting.getServiceRoutingKey)
    val channelListener = newChannelListener(port.getUid)
    val endpointListener = newEndpointListener(c.getUid)

    // add the device, get the command issuer callback
    if (protocol.requiresChannel) protocol.addChannel(port, channelListener)
    val cmdHandler = protocol.addEndpoint(endpoint.getName, port.getName, endpoint.getConfigFilesList.toList, publisher, endpointListener)
    val service = new SingleEndpointCommandService(cmdHandler)
    conn.bindService(service, AddressableService(c.getRouting.getServiceRoutingKey))

    info("Added endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name + " routing key: " + c.getRouting.getServiceRoutingKey)
  }

  def removeEntry(c: ConnProto) {
    debug("Removing endpoint " + c.getEndpoint.getName)
    val protocol = getProtocol(c.getEndpoint.getProtocol)
    protocol.removeEndpoint(c.getEndpoint.getName)
    if (protocol.requiresChannel) protocol.removeChannel(c.getEndpoint.getChannel.getName)
    info("Removed endpoint " + c.getEndpoint.getName + " on protocol " + protocol.name)
  }

  private def newEndpointListener(endpointUid: String) = new IEndpointListener {

    val session = conn.getClientSession

    override def onStateChange(state: ConnProto.State) = {
      val update = ConnProto.newBuilder.setUid(endpointUid).setState(state).build
      try {
        val result = session.postOneOrThrow(update)
        debug { "Updated connection state: " + result }
      } catch {
        case ex: ReefServiceException => error(ex)
      }
    }
  }

  private def newChannelListener(channelUid: String) = new IChannelListener {

    val session = conn.getClientSession

    override def onStateChange(state: CommChannel.State) = {
      val update = CommChannel.newBuilder.setUid(channelUid).setState(state).build
      try {
        val result = session.postOneOrThrow(update)
        debug { "Updated channel: " + result }
      } catch {
        case ex: ReefServiceException => error(ex)
      }
    }

  }

  /**
   * push measurement batchs to the addressable service
   */
  private def batchPublish(client: ClientSession, attempts: Int, dest: IDestination)(x: Measurements.MeasurementBatch): Unit = {
    try {
      client.putOrThrow(x, destination = dest)
    } catch {
      case a: ResponseTimeoutException =>
        if (attempts >= maxAttemptsToRetryMeasurements) {
          error("Multiple timeouts publishing measurements to MeasurementProcessor at: " + dest)
          error(a)
        } else {
          info("Retrying publishing measurements : " + x.getMeasCount)
          batchPublish(client, attempts + 1, dest)(x)
        }
      case e: Exception =>
        error("Error publishing measurements to MeasurementProcessor at: " + dest)
        error(e)
    }
  }

  private def newPublisher(routingKey: String) = new IPublisher {

    val session = conn.getClientSession

    override def publish(batch: Measurements.MeasurementBatch) = batchPublish(session, 0, AddressableService(routingKey))(batch)

    override def close() = session.close
  }
}

