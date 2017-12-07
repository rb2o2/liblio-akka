package ru.pangaia.collection.entity

import spray.json.{JsArray, JsObject, JsString, JsValue, RootJsonFormat}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

object Marshallers
{
  // formats for unmarshalling and marshalling
  implicit val categoryFormat: RootJsonFormat[Cat] = new RootJsonFormat[Cat]
  {
    override def write(obj: Cat): JsValue = JsObject(Map(
      "id" -> LongJsonFormat.write(obj.id),
      "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime),
      "name" -> JsString(obj.name),
      "description" -> JsString(obj.description)))

    override def read(json: JsValue): Cat = ???
  }

  implicit val treeFormat: RootJsonFormat[CategoryNode] = new RootJsonFormat[CategoryNode]
  {
    override def write(obj: CategoryNode): JsValue =

      JsObject(Map(
        "index" -> JsString(obj.index),
        "value" -> categoryFormat.write(obj.value),
        "children" ->
          {
            if (obj.children.isEmpty) JsArray()
            else JsArray(obj.children.map(write).toVector)
          }))

    override def read(json: JsValue): CategoryNode = ???
  }

  implicit object fieldFormat extends RootJsonFormat[CardField]
  {
    override def read(json: JsValue): CardField = ???

    override def write(obj: CardField): JsValue = obj match
    {
      case i:IntField=> JsObject(Map(
        "name" -> JsString(i.name),
        "description" -> JsString(i.description),
        "id" -> LongJsonFormat.write(obj.id),
        "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime)))
      case i:StringField => JsObject(Map(
        "name" -> JsString(i.name),
        "description" -> JsString(i.description),
        "id" -> LongJsonFormat.write(obj.id),
        "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime)))
      case i:ChoiceField => JsObject(Map(
        "name" -> JsString(i.name),
        "description" -> JsString(i.description),
        "possibleValues" -> JsArray(i.possibleChoices.map((s:String) => JsString(s)).toVector),
        "id" -> LongJsonFormat.write(obj.id),
        "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime)))
      case t:TaxonField => JsObject(Map(
        "name" -> JsString(t.name),
        "description" -> JsString(t.description),
        "root" -> treeFormat.write(t.root),
        "id" -> LongJsonFormat.write(obj.id),
        "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime)))
      case c: BooleanField => JsObject(Map(
        "name" -> JsString(c.name),
        "description" -> JsString(c.description),
        "id" -> LongJsonFormat.write(obj.id),
        "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime)
      ))
      case _ => JsArray()
    }
  }
  implicit val recordFormat: RootJsonFormat[Record] = new RootJsonFormat[Record]
  {
    override def write(obj: Record): JsValue = JsObject(Map(
      "id" -> LongJsonFormat.write(obj.id),
      "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime),
      "field" -> fieldFormat.write(obj.field),
      "value" -> JsString(obj.value)))

    override def read(json: JsValue): Record = ???
  }
  implicit val collectibleFormat: RootJsonFormat[Collectible] = new RootJsonFormat[Collectible]
  {
    override def write(obj: Collectible): JsValue = JsObject(Map(
      "id" -> LongJsonFormat.write(obj.id),
      "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime),
      "fields" -> JsArray(obj.fields.map(fieldFormat.write).toVector),
      "name" -> JsString(obj.name),
      "description" -> JsString(obj.description)))

    override def read(json: JsValue): Collectible = ???
  }
  implicit val cardFormat: RootJsonFormat[CatalogCard] = new RootJsonFormat[CatalogCard]
  {
    override def write(obj: CatalogCard): JsValue = JsObject(Map(
      "id" -> LongJsonFormat.write(obj.id),
      "createdOn" -> LongJsonFormat.write(obj.createdOn.getTime),
      "records" -> JsArray(obj.records.map(recordFormat.write).toVector),
      "coll" -> collectibleFormat.write(obj.coll)))

    override def read(json: JsValue): CatalogCard = ???
  }
}