package omnibus.lagom

import scala.collection.immutable
import play.api.http.Status
import com.lightbend.lagom.scaladsl.api.transport.{ MessageProtocol, ResponseHeader }

package object http {

  val Accepted: ResponseHeader = ResponseHeader(
    status = Status.ACCEPTED,
    protocol = MessageProtocol.empty,
    headers = immutable.Seq.empty[( String, String )]
  )
}
