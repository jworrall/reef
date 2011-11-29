/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. Green Energy
 * Corp licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.totalgrid.reef.client.sapi.rpc.impl

import org.totalgrid.reef.proto.Model.{ ReefID, ReefUUID }

import org.totalgrid.reef.proto.OptionalProtos._

import org.totalgrid.reef.proto.FEP.{ CommEndpointConfig, CommEndpointConnection }

import net.agileautomata.executor4s.{ Failure, Success }
import org.totalgrid.reef.client.sapi.rpc.EndpointManagementService
import org.totalgrid.reef.clientapi.sapi.client.Promise
import org.totalgrid.reef.proto.Descriptors
import org.totalgrid.reef.clientapi.sapi.client.rpc.framework.HasAnnotatedOperations

trait EndpointManagementServiceImpl extends HasAnnotatedOperations with EndpointManagementService {

  override def getEndpoints() = ops.operation("Couldn't get list of all endpoints") {
    _.get(CommEndpointConfig.newBuilder.setUuid(ReefUUID.newBuilder.setValue("*")).build).map(_.many)
  }

  override def getEndpointByName(name: String) = ops.operation("Couldn't get endpoint with name: " + name) {
    _.get(CommEndpointConfig.newBuilder.setName(name).build).map(_.one)
  }

  override def getEndpointByUuid(endpointUuid: ReefUUID) = ops.operation("Couldn't get endpoint with uuid: " + endpointUuid.getValue) {
    _.get(CommEndpointConfig.newBuilder.setUuid(endpointUuid).build).map(_.one)
  }

  override def disableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, false)

  override def enableEndpointConnection(endpointUuid: ReefUUID) = alterEndpointEnabled(endpointUuid, true)

  private def alterEndpointEnabled(endpointUuid: ReefUUID, enabled: Boolean): Promise[CommEndpointConnection] = {
    ops.operation("Couldn't alter endpoint: " + endpointUuid.getValue + " to enabled: " + enabled) { client =>
      val f1 = client.get(CommEndpointConnection.newBuilder.setEndpoint(CommEndpointConfig.newBuilder.setUuid(endpointUuid)).build).map(_.one)

      // this tricky little SOB creates another future based on the result of the last one, either by
      f1.flatMap { r =>
        r match {
          case Success(conn) => client.post(CommEndpointConnection.newBuilder.setUid(conn.getUid).setEnabled(enabled).build).map(_.one)
          case Failure(ex) => f1
        }
      }
    }
  }

  override def alterEndpointConnectionState(uid: ReefID, state: CommEndpointConnection.State) = {
    ops.operation("Couldn't alter endpoint connection: " + uid + " to : " + state) {
      _.post(CommEndpointConnection.newBuilder.setUid(uid).setState(state).build).map(_.one)
    }
  }

  override def getEndpointConnections() = ops.operation("Couldn't get list of all endpoint connections") {
    _.get(CommEndpointConnection.newBuilder.setUid(ReefID.newBuilder.setValue("*")).build).map(_.many)
  }

  override def subscribeToEndpointConnections() = {
    ops.subscription(Descriptors.commEndpointConnection, "Couldn't subscribe to all endpoint connections") { (sub, client) =>
      client.get(CommEndpointConnection.newBuilder.setUid(ReefID.newBuilder.setValue("*")).build, sub).map(_.many)
    }
  }

  override def getEndpointConnectionByUuid(endpointUuid: ReefUUID) = ops.operation("Couldn't get endpoint connection uuid: " + endpointUuid.getValue) {
    _.get(CommEndpointConnection.newBuilder.setEndpoint(CommEndpointConfig.newBuilder.setUuid(endpointUuid)).build).map(_.one)
  }

}