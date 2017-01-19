package peds.akka.persistence

import akka.actor.ActorLogging
import akka.persistence.PersistentActor


trait SnapshotLimiter extends PersistentActor { outer: ActorLogging =>
  private var _lastSnapshotNr: Long = 0L

  private def resetSnapshotMonitor(): Unit = _lastSnapshotNr = lastSequenceNr
  private def journaledEventsSinceSnapshot(): Unit = lastSequenceNr - _lastSnapshotNr
  def isRedundantSnapshot: Boolean = lastSequenceNr <= _lastSnapshotNr

  override def saveSnapshot( snapshot: Any ): Unit = {
    if ( !isRedundantSnapshot ) {
      super.saveSnapshot( snapshot )
      resetSnapshotMonitor()
    } else {
      log.debug( "[{}] ignoring snapshot request last-snapshot:[{}] last-journaled:[{}]", self.path.name, _lastSnapshotNr, lastSequenceNr )
    }
  }
}
