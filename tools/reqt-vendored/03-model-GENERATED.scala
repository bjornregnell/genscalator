//--- THIS IS A GENERATED FILE! DO NOT EDIT AS CHANGES ARE LOST ON RE-GENERATION 
//--- Edit the code below by changing `def generate` in reqt.meta
//--- Generate this file in sbt> `meta`
//--- or by `println(reqt.meta.generate)` in sbt console and copy-paste

package reqt

/** A tree-like data structure for requirements models. */
final case class Model(elems: Vector[Elem]) extends ModelMembers
case object Model extends ModelCompanion

/** A base type for all elements of Model. */
sealed trait Elem:
  def t: ElemType
  def isEnt: Boolean = this.isInstanceOf[Ent]
  def isRel: Boolean = this.isInstanceOf[Rel]
  def isNode: Boolean = this.isInstanceOf[Node]
  def isAttr: Boolean = this.isInstanceOf[Attr[?]]
  def isIntAttr: Boolean = this.isInstanceOf[IntAttr]
  def isStrAttr: Boolean = this.isInstanceOf[StrAttr]
  def isUndefined: Boolean = this.isInstanceOf[Undefined[?]]

object Elem:
  given elemOrd: Ordering[Elem] = Ordering.by:
    case IntAttr(t, value) => (t.toString,   value, "",         "",         "")
    case StrAttr(t, value) => (t.toString,   0,     value,      "",         "")
    case Undefined(t)      => ("Undefined",  0,     t.toString, "",         "")
    case Ent(t, id)        => (t.toString,   0,     id,         "",         "") 
    case Rel(e, t, sub)    => (e.t.toString, 1,     e.id,       t.toString, sub.showDense)

sealed trait Node extends Elem:
  def t: NodeType

sealed trait ElemType:
  def conceptGroup: meta.ConceptGroup = meta.groupMap(this.toString)

sealed trait NodeType extends ElemType
sealed trait AttrType[T] extends NodeType:
  def apply(value: T): Attr[T]

final case class Link(e: Ent, t: RelType)

final case class Ent private (t: EntType, id: String) extends Node:
  override def toString = s"Ent(${t},${id.quotedEscaped})"
object Ent:
  val emptyId = "???"
  def apply(t: EntType, id: String): Ent = 
    new Ent(t, if id.isEmpty then emptyId else id)

final case class LinkType(et: EntType, rt: RelType) 

sealed trait Attr[T] extends Node:
  def t: AttrType[T]
  def value: T

final case class StrAttr(t: StrAttrType, value: String) extends Attr[String]:
  override def toString = s"StrAttr(${t},${value.quotedEscaped})"
case object StrAttr extends meta.ConceptGroup:
  def types: Seq[StrAttrType] = meta.strAttrTypes.values.toSeq

final case class IntAttr(t: IntAttrType, value: Int) extends Attr[Int]
case object IntAttr extends meta.ConceptGroup:
  def types: Seq[IntAttrType] = meta.intAttrTypes.values.toSeq

final case class Undefined[T](t: AttrType[T]) extends Attr[T]:
  def value: T = throw new java.util.NoSuchElementException

final case class Rel(e: Ent, t: RelType, sub: Model) extends Elem:
  def link: Link = Link(e, t)
  def subnodes: Vector[Node] = sub.elems.collect{ case n: Node => n }
  def subrels: Vector[Rel] = sub.elems.collect{ case r: Rel => r }
  def expandSubnodes: Vector[Rel] = sub.elems.collect{ case n: Node => Rel(e, t, Model(n)) }

enum EntType extends NodeType:
  def apply(id: String): Ent = Ent(this, id)
  case Actor, App, Barrier, Breakpoint, Class, Component, Configuration, Data, Design, Domain, Epic, Event, Feature, Field, Function, Goal, Idea, Image, Interface, Issue, Item, Label, Member, Module, Product, Prototype, Quality, Relationship, Release, Req, Resource, Risk, Scenario, Screen, Section, Service, Stakeholder, State, Story, System, Target, Task, Term, Test, UseCase, User, Variant, VariationPoint, WorkPackage

enum StrAttrType extends AttrType[String]:
  def apply(value: String): StrAttr = StrAttr(this, value)
  case Comment, Constraints, Deprecated, Example, Expectation, Failure, Gist, Input, Location, Output, Spec, Text, Title, Why

enum IntAttrType extends AttrType[Int]:
  def apply(value: Int):    IntAttr = IntAttr(this, value)
  case Benefit, Capacity, Cost, Damage, Frequency, Max, Min, Order, Prio, Probability, Profit, Value

enum RelType extends ElemType:
  case Binds, Deprecates, Excludes, Has, Helps, Hurts, Impacts, Implements, InteractsWith, Is, Precedes, RelatesTo, Requires, Verifies

export EntType.*
export StrAttrType.*
export IntAttrType.*
export RelType.*

extension (e: Ent)
  infix def binds(sub: Elem*): Rel = Rel(e, Binds, Model(sub*))
  infix def binds: Link = Link(e, Binds)

  infix def deprecates(sub: Elem*): Rel = Rel(e, Deprecates, Model(sub*))
  infix def deprecates: Link = Link(e, Deprecates)

  infix def excludes(sub: Elem*): Rel = Rel(e, Excludes, Model(sub*))
  infix def excludes: Link = Link(e, Excludes)

  infix def has(sub: Elem*): Rel = Rel(e, Has, Model(sub*))
  infix def has: Link = Link(e, Has)

  infix def helps(sub: Elem*): Rel = Rel(e, Helps, Model(sub*))
  infix def helps: Link = Link(e, Helps)

  infix def hurts(sub: Elem*): Rel = Rel(e, Hurts, Model(sub*))
  infix def hurts: Link = Link(e, Hurts)

  infix def impacts(sub: Elem*): Rel = Rel(e, Impacts, Model(sub*))
  infix def impacts: Link = Link(e, Impacts)

  infix def implements(sub: Elem*): Rel = Rel(e, Implements, Model(sub*))
  infix def implements: Link = Link(e, Implements)

  infix def interactsWith(sub: Elem*): Rel = Rel(e, InteractsWith, Model(sub*))
  infix def interactsWith: Link = Link(e, InteractsWith)

  infix def is(sub: Elem*): Rel = Rel(e, Is, Model(sub*))
  infix def is: Link = Link(e, Is)

  infix def precedes(sub: Elem*): Rel = Rel(e, Precedes, Model(sub*))
  infix def precedes: Link = Link(e, Precedes)

  infix def relatesTo(sub: Elem*): Rel = Rel(e, RelatesTo, Model(sub*))
  infix def relatesTo: Link = Link(e, RelatesTo)

  infix def requires(sub: Elem*): Rel = Rel(e, Requires, Model(sub*))
  infix def requires: Link = Link(e, Requires)

  infix def verifies(sub: Elem*): Rel = Rel(e, Verifies, Model(sub*))
  infix def verifies: Link = Link(e, Verifies)
