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
package org.totalgrid.reef.measproc.pipeline

import org.totalgrid.reef.proto.Events
import org.totalgrid.reef.metrics.MetricsHookContainer
import org.totalgrid.reef.proto.Measurements.{ MeasurementBatch, Measurement }
import org.totalgrid.reef.measproc.{ MeasBatchProcessor, processing, MeasProcObjectCaches }

class MeasProcessingPipeline(
    caches: MeasProcObjectCaches,
    publish: Measurement => Unit,
    eventSink: Events.Event.Builder => Unit) extends MeasBatchProcessor with MetricsHookContainer {

  // pipeline ends up being defined backwards, output from each step is wired into input of previous step
  // basicProcessingNode -> overrideProc -> triggerProc -> batchOutput

  val batchOutput = new ProcessedMeasBatchOutputCache(publish, eventSink, caches.measCache)

  val triggerFactory = new processing.TriggerProcessingFactory(batchOutput.delayedEventSink)
  val triggerProc = new processing.TriggerProcessor(batchOutput.pubMeas, triggerFactory, caches.stateCache)
  val overProc = new processing.OverrideProcessor(overrideProcess, caches.overCache, caches.measCache.get)

  // start the pipeline
  val processor = new MeasPipelinePump(overProc.process, batchOutput.flushCache)

  addHookedObject(processor :: overProc :: triggerProc :: Nil)

  // Each MeasOverride add/remove is processed seperatley (not in a meas batch)
  def overrideProcess(m: Measurement, flushNow: Boolean) {

    triggerProc.process(m)
    if (flushNow) batchOutput.flushCache()
  }

  override def process(b: MeasurementBatch) = processor.process(b)
}