package ru.pangaia.collection.entity

import java.io.{FileInputStream, InputStreamReader}
import java.sql.Timestamp
import java.time.Instant

import scala.collection.generic.Growable
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


case class Collectible(name: String, description: String = "generic collection",
                       fields: Map[String, CardField])(implicit user: User) extends Named with Entity
{
  override var modifiedBy: Option[User] = None
  override val createdBy: User = user
}

case class Record(field: CardField)(implicit user: User) extends Entity
{
  private var valu: String = ""
  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  def value_=(value: String)(implicit user: User) : Unit =
  {
    this.modifiedOn = Some(Timestamp.from(Instant.now))
    this.modifiedBy = Some(user)
    this.valu = value
  }
  def value: String = valu
}

sealed trait CardField extends Named with Entity
{
  type choiceType = String
  type recordType
  def validate(s: String): Boolean = s.matches(pattern)
  def writeToRecord(r: Record, s: String)(implicit user: User): Unit =
  {
    if (r.field == this && validate(s)) r.value = s.toString
  }
  val default: String
  var pattern: String
  def read(record: Record): Option[recordType]
  def withRegex(re: String): CardField = {pattern = re; this}
}

case class StringField(override val name: String,
                       override val description: String)(implicit user: User) extends CardField
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  override type recordType = String
  override val default: String = "--"

  override def read(record: Record) = Some(record.value)

  var pattern = ".*"
}
case class IntField(override val name: String,
                    override val description: String)(implicit user: User) extends CardField
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  override type recordType = Int
  override val default: String = "0"

  override def read(record: Record) = Some(record.value.toInt)

  var pattern = "[1-9][0-9]*"
}
case class BooleanField(override val name: String,
                        override val description: String)(implicit user: User) extends CardField
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  override type recordType = Boolean
  override val default: String = "false"
  override def read(record: Record) = record.value match
  {
    case "true" => Some(true)
    case "false" => Some(false)
    case _ => None
  }

    override var pattern: String = "true|false"

}
case class ChoiceField(override val name: String,
                       override val description: String,
                       possibleChoices: Seq[String])(implicit user: User) extends CardField
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  override type recordType = String

  override def read(record: Record) = Some(record.value)

  override def validate(s: String): Boolean = possibleChoices.contains(s)

  override val default: String = possibleChoices.head

  var pattern = ".*"
}

case class Cat(override val name: String, override val description: String)(implicit user: User) extends Named with Entity
{
  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
}

case class CategoryNode(index: String,
                        value: Cat,
                        var children: collection.mutable.Seq[CategoryNode] with Growable[CategoryNode])
{
  def containsDeeper(v: String): Boolean =
  {
    index == v || children.exists(_.containsDeeper(v))
  }
}
case class InverseCategoryNode(index: String,
                               value: Cat,
                               parent: InverseCategoryNode)
case object InverseCategoryNode
{
  //  implicit val reader: RootJsonReader[Map[String,InverseCategoryNode]] = new RootJsonReader[Map[String, InverseCategoryNode]]
  //  {
  //    override def read(json: JsValue): Map[String, InverseCategoryNode] =
  //    {
  //      val a: Map[String, InverseCategoryNode] =
  //        json.asJsObject.fields.values.map((js: JsValue) =>
  //        {
  //          val ob = js.asJsObject;
  //          (ob.fields("name").toString,
  //            InverseCategoryNode(
  //              ob.fields("name").toString(),
  //              Cat(ob.fields("name").toString(),ob.fields("desc").toString()),
  //
  //            ))
  //        }).toMap[String, InverseCategoryNode]
  //    }
  //  }
  def readFromJson(filepath: String): Map[String, InverseCategoryNode] =
  {
    val fileIS = new InputStreamReader(new FileInputStream(filepath))
    try
    {
      val cbuff: Array[Char] = new Array(4096)
      val sb: mutable.StringBuilder = new StringBuilder()
      while (fileIS.read(cbuff) != -1)
      {
        sb.append(cbuff)
      }
      ??? //TODO
    }
    catch
    {
      case ex: Exception => fileIS.close(); ???
    }
    finally
    {
      fileIS.close()
    }
  }
}
case object CategoryNode
{
  def rootFromInverse(nodes: Map[String,InverseCategoryNode]): CategoryNode =
  {
    implicit val user:User = RootAuthority
    val root = CategoryNode("UDC", Cat("root","Корень УДК"), ListBuffer[CategoryNode]())
    //    def addBranch(node: InverseCategoryNode, catnode: CategoryNode) =
    //    {
    //      if (node.parent.index == "UDC")
    //        root.children = CategoryNode(node.index, node.value, ListBuffer()) +: root.children
    //    }
    val nodesStraight = collection.mutable.Map[String, CategoryNode]()
    for ((ind, node) <- nodes)
    {
      nodesStraight += (ind -> CategoryNode(node.index, node.value, ListBuffer()))
    }
    nodesStraight += ("UDC" -> root)
    for ((ind, node) <- nodes)
    {
      nodesStraight(node.parent.index).children += nodesStraight(ind)
    }
    root
  }
}

case class TaxonField(override val name: String,
                      override val description: String,
                      root: CategoryNode)(implicit user: User) extends CardField
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None
  override type recordType = String

  override def read(record: Record) = Some(record.value)

  override def validate(s: String): Boolean = root containsDeeper s

  override val default: String = root.index

  var pattern: String = ".*"
}


case class CatalogCard(coll: Collectible, var records: Array[Record])(implicit user: User) extends Entity
{

  override val createdBy: User = user
  override var modifiedBy: Option[User] = None

  records = coll.fields.values.map((fld: CardField) => Record(fld)).toArray

  def getRecordValueByFieldName(fldName: String): Option[String] =
    records.find((r) => r.field == coll.fields(fldName)).map((r) => r.value)

  override def toString: String = records.map((r: Record)=>
  {
    r.field.name +": "+  r.value
  }).mkString("; ")
}

