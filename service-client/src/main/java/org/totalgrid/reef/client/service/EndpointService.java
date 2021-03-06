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
package org.totalgrid.reef.client.service;

import org.totalgrid.reef.client.exception.ReefServiceException;
import org.totalgrid.reef.client.SubscriptionResult;
import org.totalgrid.reef.client.service.proto.FEP.FrontEndProcessor;
import org.totalgrid.reef.client.service.proto.FEP.Endpoint;
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection;
import org.totalgrid.reef.client.service.proto.FEP.EndpointConnection.State;
import org.totalgrid.reef.client.service.proto.Model.ReefUUID;
import org.totalgrid.reef.client.service.proto.Model.ReefID;

import java.util.List;

/**
 * Communication Endpoints are the "field devices" that reef communicates with using legacy protocols
 * to acquire measurements from the field. Every point and command in the system is associated with
 * at most one endpoint at a time. Endpoint includes information about the protocol, associated
 * points, associated commands, communication channels, config files.
 * <p/>
 * For protocols that have reef front-end support there is an auxiliary service associated with Endpoints that
 * tracks which front-end each endpoint is assigned to. It also tracks the current state of the legacy protocol
 * connection which is how the protocol adapters tell reef if they are successfully communicating with the field
 * devices. We can also disable (and re-enable) the endpoint connection attempts, this is useful for devices that
 * can only talk with one "master" at a time so we can disable reefs protocol adapters temporarily to allow
 * another master to connect.
 *
 * Tag for api-enhancer, do not delete: !api-definition!
 */
public interface EndpointService
{

    /**
     * @return list of all endpoints in the system
     */
    List<Endpoint> getEndpoints() throws ReefServiceException;

    /**
     * @param name name of endpoint
     * @return the endpoint with that name or throws an exception
     */
    Endpoint getEndpointByName( String name ) throws ReefServiceException;

    /**
     * @param endpointUuid uuid of endpoint
     * @return the endpoint with that uuid or throws an exception
     */
    Endpoint getEndpointByUuid( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * @param names name of endpoint
     * @return the endpoint with that name or throws an exception
     */
    List<Endpoint> getEndpointsByNames( List<String> names ) throws ReefServiceException;

    /**
     * @param endpointUuids uuid of endpoint
     * @return the endpoint with that uuid or throws an exception
     */
    List<Endpoint> getEndpointsByUuids( List<ReefUUID> endpointUuids ) throws ReefServiceException;

    /**
     * disables automatic protocol adapter assignment and begins stopping any running protocol adapters.
     * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    EndpointConnection disableEndpointConnection( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * enables any automatic protocol adapter assignment and begins starting any available protocol adapters.
     * service NOTE doesn't wait for protocol adapter to report a state change so don't assume state will have changed
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    EndpointConnection enableEndpointConnection( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * Alter an endpoint to determine if it is autoAssigned to a frontend by the coordinator or is manually claimed.
     * Endpoints that are manuallyAssigned are marked COMMS_DOWN by the coordinator only if the application that
     * has claimed the endpoint timesout the heartbeat, all other transitions become the responsiblity of the protocol
     * adapters.
     * @param endpointUuid endpoint uuid
     * @param autoAssigned whether the front end is assigned by the coordinator based on protocol and location (true) or
     *                     if frontends will manually claim endpoints they communicating with (false)
     * @return the updated endpoint proto
     */
    Endpoint setEndpointAutoAssigned( ReefUUID endpointUuid, boolean autoAssigned ) throws ReefServiceException;

    /**
     * Claims an endpoint as being handled by a particular application. This provides two functions:
     * - It indicates to other, possibly competing, protocol adapters that the endpoint is handled
     * - If the protocol adapter is heartbeating and loses contact with the server the endpoint will automatically
     *   be marked as COMMS_DOWN.
     * @param endpointUuid endpoint uuid
     * @param applicationUuid uuid of the protocol adapter application
     * @return the endpoint connection reflecting the assignment or an error if assignment fails
     */
    EndpointConnection setEndpointConnectionAssignedProtocolAdapter( ReefUUID endpointUuid, ReefUUID applicationUuid ) throws ReefServiceException;

    /**
     * @return List of all protocol adapters registered with the system
     */
    List<FrontEndProcessor> getProtocolAdapters() throws ReefServiceException;

    /**
     * get all of the objects representing endpoint to protocol adapter connections. Sub protos - Endpoint and frontend
     * will be filled in with name and uuid
     *
     * @return list of all endpoint connection objects
     */
    List<EndpointConnection> getEndpointConnections() throws ReefServiceException;

    /**
     * Same as getEndpointConnections but subscribes the user to all changes
     *
     * @return list of all endpoint connection objects
     * @see #getEndpointConnections()
     */
    SubscriptionResult<List<EndpointConnection>, EndpointConnection> subscribeToEndpointConnections() throws ReefServiceException;

    /**
     * Get current endpoint connection state for an endpoint
     *
     * @param endpointUuid uuid of endpoint
     * @return the connection object representing the current connection state
     */
    EndpointConnection getEndpointConnectionByUuid( ReefUUID endpointUuid ) throws ReefServiceException;

    /**
     * Get current endpoint connection state for an endpoint
     *
     * @param endpointName name of endpoint
     * @return the connection object representing the current connection state
     */
    EndpointConnection getEndpointConnectionByEndpointName( String endpointName ) throws ReefServiceException;

    /**
     * Protocol Adapters will update the endpoint connection state to indicate when the endpoint
     * changes communication state.
     * @param connectionId  string id for the endpoint connection
     * @param state          COMMS_UP, COMMS_DOWN, COMMS_ERROR ...
     * @return updated state
     */
    EndpointConnection alterEndpointConnectionState( ReefID connectionId, State state ) throws ReefServiceException;

    /**
     * Protocol Adapters will update the endpoint connection state to indicate when the endpoint
     * changes communication state.
     * @param endpointUuid  id for the endpoint
     * @param state          COMMS_UP, COMMS_DOWN, COMMS_ERROR ...
     * @return updated state
     */
    EndpointConnection alterEndpointConnectionStateByEndpoint( ReefUUID endpointUuid, State state ) throws ReefServiceException;

}