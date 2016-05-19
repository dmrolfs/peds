//package peds.akka.stream
//
//import akka.actor.ActorSystem
//import akka.stream.Materializer
//import akka.stream.scaladsl.Flow
//import com.typesafe.scalalogging.LazyLogging
//import peds.commons.Valid
//
//
///**
//  * Created by rolfsd on 5/17/16.
//  */
//trait Batching[T] {
//  type ACC
//  def batch( acc: ACC, elem: T ): Valid[ACC]
//  def seed: (T) => ACC
//}
//
//object BatchingStage extends LazyLogging {
//
//  def batchingFlow[T: Batching, NotUsed](
//    max: Long
//  )(
//    implicit system: ActorSystem,
//    materializer: Materializer
//  ): Flow[T, Batching[T]#ACC, NotUsed] = {
//    val batching = implicitly[Batching[T]]
//    Flow[T]
//    .batch( max, batching.seed ) { (acc, t) =>
//      import scalaz._, Scalaz._
//
//      batching.batch( acc, t ) match {
//        case Success( nacc ) => nacc
//        case Failure( exs ) => {
//          exs foreach { ex => logger.error( s"batching elem:[${t}] failed: {}", ex ) }
//          acc
//        }
//      }
//    }
//  }
//}
//
//
//
//
///**********
//def batchSeriesByPlan(
//max: Long
//)(
//implicit system: ActorSystem,
//materializer: Materializer,
//tsMerging: Merging[TimeSeries]
//): Flow[(TimeSeries, OutlierPlan), (TimeSeries, OutlierPlan), NotUsed] = {
//  type PlanSeriesAccumulator = Map[(Topic, OutlierPlan), TimeSeries]
//  val seed: ((TimeSeries, OutlierPlan)) => (PlanSeriesAccumulator, Int) = (tsp: (TimeSeries, OutlierPlan)) => {
//  val (ts, p) = tsp
//  val key = (ts.topic, p)
//  ( Map( key -> ts ), 0 )
//}
//
//  Flow[(TimeSeries, OutlierPlan)]
//  .batch( max, seed ){ case ((acc, count), (ts, p)) =>
//  import scalaz._, Scalaz._
//
//  val key: (Topic, OutlierPlan) = (ts.topic, p)
//  val existing: Option[TimeSeries]  = acc get key
//
//  val newAcc: V[PlanSeriesAccumulator] = for {
//  updated <- existing.map{ e => tsMerging.merge(e, ts).disjunction } getOrElse ts.right
//} yield {
//  acc + ( key -> updated )
//}
//
//  val resultingAcc = newAcc match {
//  case \/-( a ) => {
//  //          val points: Int = a.values.foldLeft( 0 ){ _ + _.points.size }
//  //          debugLogger.info(
//  //            "batchSeriesByPlan batching count:[{}] topic-plans & points = [{}] [{}] avg pts/topic-plan combo=[{}]",
//  //            count.toString,
//  //            a.keys.size.toString,
//  //            points.toString,
//  //            ( points.toDouble / a.keys.size.toDouble ).toString
//  //          )
//  a
//}
//  case -\/( exs ) => {
//  exs foreach { ex => logger.error( "batching series by plan failed: {}", ex ) }
//  acc
//}
//}
//
//  ( resultingAcc, count + 1 )
//}
//  .map { ec: (PlanSeriesAccumulator, Int) =>
//  //      val (elems, count) = ec
//  //      val recs: Seq[((Topic, OutlierPlan), TimeSeries)] = elems.toSeq
//  //      debugLogger.info(
//  //        "batchSeriesByPlan pushing combos downstream topic:plans combos:[{}] total points:[{}]",
//  //        recs.size.toString,
//  //        recs.foldLeft( 0 ){ _ + _._2.points.size }.toString
//  //      )
//  ec
//}
//  .mapConcat { case (ps, _) => ps.toSeq.to[scala.collection.immutable.Seq].map { case ((topic, plan), ts) => ( ts, plan ) } }
//********/