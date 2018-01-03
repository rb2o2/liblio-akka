package ru.pangaia.collection.entity

import java.io.{FileInputStream, InputStreamReader}
import java.sql.Timestamp
import java.time.Instant

import scala.collection.generic.Growable
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.{Failure, Success, Try}


case class Collectible(name: String, description: String = "generic collection",
                       private val fields: mutable.Map[String, CardField])(implicit user: User) extends Named with Entity
{
  override val createdBy: User = user

  def recordsArrayFromFields: Array[Record] = fields.values.map((fld: CardField) => Record(fld)).toArray

  def getField(fldName: String): Option[CardField] = fields.get(fldName)

  def fieldsVector: Vector[CardField] = fields.values.toVector

  def addField(fld: CardField)(implicit user: User): Unit =
  {
    fields += (fld.name -> fld)
    modifiedBy = Some(user)
    modifiedOn = Some(Timestamp.from(Instant.now()))
  }
}

case class Record(field: CardField)(implicit user: User) extends Entity
{
  override val createdBy: User = user
  private var valu: String = ""

  def value_=(value: String)(implicit user: User): Unit =
  {
    this.modifiedOn = Some(Timestamp.from(Instant.now))
    this.modifiedBy = Some(user)
    this.valu = value
  }

  def value: String = valu
}

sealed trait CardField extends Named with Entity
{
  type recordType
  val default: recordType
  def writeToRecord(r: Record, s: String)(implicit user: User): Try[Unit] =
  {
    Try {
      if (r.field != this)  throw new IllegalArgumentException(s"wrong record")
      else if (! valid(s)) throw new IllegalArgumentException(s"value $s is invalid")
      else
        r.value = s
    }
  }

  def valid(s: String): Boolean

  def read(record: Record): Option[recordType]
}

case class StringField(override val name: String,
                       override val description: String)(implicit user: User) extends CardField
{
  override type recordType = String
  override val createdBy: User = user
  override val default: String = "--"
  var pattern = ".*"

  override def valid(s: String): Boolean = true

  override def read(record: Record) = Some(record.value)
}

case class IntField(override val name: String,
                    override val description: String)(implicit user: User) extends CardField
{
  override type recordType = Int
  override val createdBy: User = user
  override val default = 0
  var pattern = "[1-9][0-9]*"


  override def valid(s: String): Boolean = Try {s.toInt} match
  {
    case a: Success[_] => true
    case a: Failure[_] => false
  }

  override def read(record: Record) = Some(record.value.toInt)
}

case class BooleanField(override val name: String,
                        override val description: String)(implicit user: User) extends CardField
{
  override type recordType = Boolean
  override val createdBy: User = user
  override val default = false


  override def valid(s: String): Boolean = s.matches("true|false")

  override def read(record: Record): Option[recordType] = record.value match
  {
    case "true" => Some(true)
    case "false" => Some(false)
    case _ => None
  }

}

case class ChoiceString(choices: Set[String], choice: String)
{
  if (!choices.contains(choice)) throw new IllegalArgumentException("Choice does not contain in possible choices")
  override def toString: String = choice
}
case class ChoiceField(override val name: String,
                       override val description: String,
                       possibleChoices: Set[String])(implicit user: User) extends CardField
{
  override type recordType = ChoiceString
  override val createdBy: User = user
  override val default: ChoiceString = ChoiceString(possibleChoices, possibleChoices.head)
  var pattern = ".*"

  override def read(record: Record) = Some(ChoiceString(possibleChoices, record.value))

  override def valid(s: String): Boolean = possibleChoices.contains(s)
}

case class Cat(index: String,
               override val name: String,
               override val description: String)
              (implicit user: User) extends Named with Entity
{
  override val createdBy: User = user

  override def toString: String = index
}

case class CategoryNode(index: String,
                        value: Cat,
                        var children: collection.mutable.Seq[CategoryNode] with Growable[CategoryNode])
{
  def containsDeeper(v: String): Boolean =
  {
    index == v || children.exists(_.containsDeeper(v))
  }

  def findByIndex(ind: String): Option[Cat] =
  {
    if (value.index == ind)
      Some(value)
    else if (children.isEmpty)
      None
    else
    {
      val m = children.map(c => c.findByIndex(ind))
      if (m.isEmpty) None
      else m.head
    }
  }

  def containsCat(c: Cat): Boolean =
  {
    if (value == c) true
    else children.exists(cn => cn.containsCat(c))
  }
}

case class TaxonField(override val name: String,
                      override val description: String,
                      root: CategoryNode)(implicit user: User) extends CardField//TODO
{
  override type recordType = Cat
  override val createdBy: User = user
  override val default: Cat = root.value
  var pattern: String = ".*"

  override def valid(s: String): Boolean = root.containsDeeper(s)

  override def read(record: Record): Option[recordType] = root.findByIndex(record.value)
}

case class CatalogCard(coll: Collectible)(implicit user: User) extends Entity
{

  override val createdBy: User = user
  private var records = coll.recordsArrayFromFields

  def getRecordValueByFieldName(fldName: String): Option[String] =
    records.find((r) => r.field == coll.getField(fldName).get).map((r) => r.value)

  def writeToFieldRecord(field: CardField)(value: String): Unit =
  {
    records.find(r => r.field == field).foreach ((r) =>
    {
      field.writeToRecord(r, value)
    })
  }

  override def toString: String = records.map((r: Record) =>
  {
    r.field.name + ": " + r.value
  }).mkString("; ")
}

