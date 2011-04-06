/**
 * Copyright 2011 Green Energy Corp.
 *
 * Licensed to Green Energy Corp (www.greenenergycorp.com) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Green Energy Corp licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.totalgrid.reef.api.request

import org.totalgrid.reef.proto.Events.Event
import org.totalgrid.reef.proto.Alarms.Alarm
import org.totalgrid.reef.api.{ ISubscription, ReefServiceException }
import org.totalgrid.reef.api.javaclient.IEventAcceptor

/**
 * This service is used to get and produce Events on the reef system. Events are generated by the system in response
 * to unusual or interesting occurances, usually they are interesting to an operator but do not require immediate action.
 * When an event is published the system may "upgrade" an Event to also generate an alarm.
 */
trait EventService {
  /**
   * get the most recent events
   * @param limit the number of incoming events
   */
  @throws(classOf[ReefServiceException])
  def getRecentEvents(limit: Int): java.util.List[Event]

  /**
   * get the most recent events and setup a subscription to all future events
   * @param limit the number of incoming events
   * @param sub a subscription object that consumes the new Events coming in
   */
  @throws(classOf[ReefServiceException])
  def getRecentEvents(limit: Int, sub: ISubscription[Event]): java.util.List[Event]

  /**
   * get the most recent alarms
   * @param limit the number of incoming alarms
   */
  @throws(classOf[ReefServiceException])
  def getRecentAlarms(limit: Int): java.util.List[Alarm]

  /**
   * get the most recent alarms and setup a subscription to all future alarms
   * @param limit the number of incoming events
   * @param sub a subscription object that consumes the new Alarms coming in
   */
  @throws(classOf[ReefServiceException])
  def getRecentAlarms(limit: Int, sub: ISubscription[Alarm]): java.util.List[Alarm]

  /**
   * publish a new Event to the system
   */
  @throws(classOf[ReefServiceException])
  def publishEvent(event: Event): Event

  /**
   * silences an audible alarm
   */
  @throws(classOf[ReefServiceException])
  def silenceAlarm(alarm: Alarm): Alarm

  /**
   * acknowledge the alarm (silences if not already silenced)
   */
  @throws(classOf[ReefServiceException])
  def acknowledgeAlarm(alarm: Alarm): Alarm

  /**
   * "remove" an Alarm from the active list.
   */
  @throws(classOf[ReefServiceException])
  def removeAlarm(alarm: Alarm): Alarm

  /**
   * Create a subscription object that can receive Events.
   * @return "blank" subscription object, needs to have the subscription configured by passing it with another request
   */
  def createEventSubscription(callback: IEventAcceptor[Event]): ISubscription[Event]

  /**
   * Create a subscription object that can receive Alarms.
   * @return "blank" subscription object, needs to have the subscription configured by passing it with another request
   */
  def createAlarmSubscription(callback: IEventAcceptor[Alarm]): ISubscription[Alarm]
}