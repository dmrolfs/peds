package omnibus.commons.repository


trait Criteria[Q] extends (Q => Q) {
  // type Query
  type Predicate

  def filter: Option[Predicate]
  // def sortBy: Option[E => T]
  def pageStart: Option[Int]
  def pageSize: Option[Int]
}
