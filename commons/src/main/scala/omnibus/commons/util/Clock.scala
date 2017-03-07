package omnibus.commons.util

import org.joda.{ time => joda }


trait Clock extends ( () => joda.DateTime )


case object SimpleClock extends Clock {
  override def apply(): joda.DateTime = joda.DateTime.now
}
