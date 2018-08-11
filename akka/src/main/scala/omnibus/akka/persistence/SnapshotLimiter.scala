package omnibus.akka.persistence

import akka.actor.ActorLogging
import akka.persistence.PersistentActor

trait SnapshotLimiter extends PersistentActor { outer: ActorLogging =>
  private var _lastSnapshotNr: Long = 0L

  private def resetSnapshotMonitor(): Unit = _lastSnapshotNr = lastSequenceNr

  def isRedundantSnapshot: Boolean = lastSequenceNr <= _lastSnapshotNr

  protected def journaledEventsSinceSnapshot(): Long = lastSequenceNr - _lastSnapshotNr

  override def saveSnapshot( snapshot: Any ): Unit = {
    if (!isRedundantSnapshot) {
      super.saveSnapshot( snapshot )
      resetSnapshotMonitor()
    } else {
      log.debug(
        "[{}] ignoring snapshot request last-snapshot:[{}] last-journaled:[{}]",
        self.path.name,
        _lastSnapshotNr,
        lastSequenceNr
      )
    }
  }
}
