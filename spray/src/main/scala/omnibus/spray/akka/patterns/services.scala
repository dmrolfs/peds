/*
 * Copyright (C) 2013 Damon Rolfs.  All rights reserved.
 * Heavily based on code copyright (C) 2013 by CakeSolutions.  All rights reserved.
 * see https://github.com/janm399/akka-patterns/tree/master/server/api/src/main/scala/org/cakesolutions/akkapatterns/api
 *
 * This product may contain certain open source code licensed to Damon Rolfs.
 * Please open the license.txt file, read the contents and abide by the terms of all
 * such open source licenses.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package omnibus.spray.akka.patterns

import spray.http.StatusCodes._
import spray.http._
import spray.routing._
import spray.util.LoggingContext


/** Provides a hook to catch exceptions and rejections from routes, allowing custom
  * responses to be provided, logs to be captured, and potentially remedial actions.
  *
  * Note that this is not marshalled, but it is possible to do so allowing for a fully
  * JSON API (e.g. see how Foursquare do it).
  */
trait FailureHandling {
  this: HttpService =>

  // For Spray > 1.1-M7 use routeRouteResponse
  // see https://groups.google.com/d/topic/spray-user/zA_KR4OBs1I/discussion
  def rejectionHandler: RejectionHandler = RejectionHandler.Default

  def exceptionHandler( implicit log: LoggingContext ) = ExceptionHandler.fromPF {
    case e: IllegalArgumentException => {
      ctx => 
          loggedFailureResponse( ctx, e,
            message = "The server was asked a question that didn't make sense: " + e.getMessage,
            error = NotAcceptable
          )
    }

    case e: NoSuchElementException => {
      ctx =>
          loggedFailureResponse( ctx, e,
            message = "The server is missing some information. Try again in a few moments.",
            error = NotFound
          )
    }

    case t: Throwable => {
      ctx =>
          // note that toString here may expose information and cause a security leak, so don't do it.
          loggedFailureResponse( ctx, t )
    }
  }

  private def loggedFailureResponse(
    ctx: RequestContext,
    thrown: Throwable,
    message: String = "The server is having problems.",
    error: StatusCode = InternalServerError
  )(implicit log: LoggingContext) {
    log.error( thrown, ctx.request.toString() )
    ctx.complete( error, message )
  }

}
