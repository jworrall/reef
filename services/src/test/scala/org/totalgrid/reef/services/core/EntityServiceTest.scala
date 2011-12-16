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
package org.totalgrid.reef.services.core

import java.util.UUID

import SyncServiceShims._
import org.totalgrid.reef.client.exception.ReefServiceException
import org.totalgrid.reef.client.proto.Envelope.Status
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.totalgrid.reef.models.{ ApplicationSchema, DatabaseUsingTestBase }
import scala.collection.JavaConversions._
import org.totalgrid.reef.client.service.proto.Model.{ Relationship, Entity, ReefUUID }

@RunWith(classOf[JUnitRunner])
class EntityServiceTest extends DatabaseUsingTestBase {

  //val service = new EntityService
  val service = new EntityModelService(new EntityServiceModel)

  test("Put Entity with predetermined UUID") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build

    val created = service.put(upload).expectOne

    created.getUuid.getValue.toString should equal(uuid)
  }

  test("Put two entities with same uuids") {

    val uuid = UUID.randomUUID.toString

    val upload = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject").addTypes("TestType").build
    val upload2 = Entity.newBuilder.setUuid(ReefUUID.newBuilder.setValue(uuid)).setName("MagicTestObject2").addTypes("TestType").build

    service.put(upload).expectOne

    intercept[ReefServiceException] {
      service.put(upload2).expectOne
    }
  }

  test("Put Entity with 2 types") {

    val upload1 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType1").addTypes("TestType3").build
    val upload2 = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType4").addTypes("TestType2").build

    service.put(upload1).expectOne(Status.CREATED)
    val updated = service.put(upload2).expectOne(Status.UPDATED)

    val types = List("TestType1", "TestType3", "TestType4", "TestType2")
    updated.getTypesList.toList.diff(types) should equal(Nil)

    service.put(upload1).expectOne(Status.NOT_MODIFIED)
    service.put(upload2).expectOne(Status.NOT_MODIFIED)
  }

  test("Delete Entity") {

    val upload = Entity.newBuilder.setName("MagicTestObject").addTypes("TestType").build

    val created = service.put(upload).expectOne(Status.CREATED)

    val deleteEnt = Entity.newBuilder().setUuid(created.getUuid).build()

    val deleted = service.delete(deleteEnt).expectOne(Status.DELETED)
  }

  import org.squeryl.PrimitiveTypeMode._

  test("Complicated Delete") {

    val regId = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    EntityQuery.addEdge(regId, subId, "owns")
    val devId = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    EntityQuery.addEdge(subId, devId, "owns")

    val edges = ApplicationSchema.edges
    val deriveds = ApplicationSchema.derivedEdges

    val preEdges = edges.where(e => true === true).toList
    val preDeriveds = deriveds.where(e => true === true).toList

    preEdges.size should equal(3)
    preDeriveds.size should equal(1)

    val deleteEnt = Entity.newBuilder().setName("Sub").build()

    val deleted = service.delete(deleteEnt).expectOne(Status.DELETED)

    val postEdges = edges.where(e => true === true).toList
    postEdges should equal(Nil)

    val postDeriveds = deriveds.where(e => true === true).toList
    postDeriveds should equal(Nil)
  }

  test("Get") {

    val regId = EntityQuery.addEntity("Reg", "Region" :: "EquipmentGroup" :: Nil)
    val subId = EntityQuery.addEntity("Sub", "Substation" :: "EquipmentGroup" :: Nil)
    EntityQuery.addEdge(regId, subId, "owns")
    val devId = EntityQuery.addEntity("Bkr", "Breaker" :: "Equipment" :: Nil)
    EntityQuery.addEdge(subId, devId, "owns")

    val req = Entity.newBuilder
      .setName("Reg")
      .addRelations(
        Relationship.newBuilder
          .setRelationship("owns")
          .setDescendantOf(true)
          .setDistance(1))

    val root = service.get(req.build).expectOne(Status.OK)
    root.getName should equal("Reg")

    root.getRelationsCount should equal(1)
    root.getRelations(0).getEntitiesCount should equal(1)
    root.getRelations(0).getEntities(0).getName should equal("Sub")
    root.getRelations(0).getEntities(0).getRelationsCount should equal(0)
  }
}