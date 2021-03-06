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
package org.totalgrid.reef.client.operations;

import org.totalgrid.reef.client.Promise;
import org.totalgrid.reef.client.SubscriptionBinding;

/**
 * Similar to BasicRequest with the added argument of a subscription object so we can tell the server
 * our locally generated queue name to make the bindings. The execute block needs to make atleast one
 * request to the server with the subscription name attached to actually setup the subscription so it
 * will receive events.
 *
 * @see BasicRequest
 */
public interface SubscriptionBindingRequest<T>
{
    Promise<T> execute( SubscriptionBinding subscription, RestOperations operations );

    /**
     * @see BasicRequest
     */
    String errorMessage();
}
