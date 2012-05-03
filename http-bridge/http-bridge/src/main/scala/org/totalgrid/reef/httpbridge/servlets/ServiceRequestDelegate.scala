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
package org.totalgrid.reef.httpbridge.servlets

import org.totalgrid.reef.client.proto.Envelope.Verb
import org.totalgrid.reef.client.{ Promise, Client, RequestHeaders }
import org.totalgrid.reef.client.operations.Response
import org.totalgrid.reef.client.operations.scl.ScalaServiceOperations._

trait ServiceRequestDelegate {
  def makeRequest[A](client: Client, verb: Verb, request: A, headers: RequestHeaders): Promise[Response[A]]
}

object ServiceRequestDelegate extends ServiceRequestDelegate {

  def makeRequest[A](client: Client, verb: Verb, request: A, headers: RequestHeaders): Promise[Response[A]] = {
    client.getServiceOperations.operation("Could not execute servlet request") { ops =>
      ops.request(verb, request, headers)
    }
  }
}
