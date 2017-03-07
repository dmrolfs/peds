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

import akka.actor.{Actor, ActorSystem}
import akka.util.Timeout
import spray._
import spray.routing._
import omnibus.spray._
// import org.cakesolutions.akkapatterns.core.CoreActorRefs
// import org.cakesolutions.akkapatterns.domain.Configured


trait ApiActor extends Actor with HttpServiceActor
// with CoreActorRefs
with FailureHandling
// with Tracking 
// with Configured
with EndpointMarshalling
// with DefaultAuthenticationDirectives 
{

  // used by the Akka ask pattern
  implicit def timeout: Timeout
  // implicit val timeout = Timeout( 10000 )

  // lets the CoreActorRef find the actor system used by Spray
  // (this could potentially be a separate system)
  implicit def actorSystem: ActorSystem

  type Route = RequestContext => Unit 
  def routes: Route
  // override val routes =
  //   customerRoute ~
  //   homeRoute ~
  //   userRoute

  override def receive = runRoute(
    handleRejections( rejectionHandler )(
      handleExceptions( exceptionHandler )(
        // trackRequestResponse(routes)
        routes
      )
    )
  )

}
