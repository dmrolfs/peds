// /*
//  * Copyright (C) 2013 Damon Rolfs.  All rights reserved.
//  * Heavily based on code copyright (C) 2013 by CakeSolutions.  All rights reserved.
//  * see https://github.com/janm399/akka-patterns/tree/master/server/api/src/main/scala/org/cakesolutions/akkapatterns/api
//  *
//  * This product may contain certain open source code licensed to Damon Rolfs.
//  * Please open the license.txt file, read the contents and abide by the terms of all
//  * such open source licenses.
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package omnibus.spray.akka.patterns.api

// import java.util.UUID //DMR: change
// import scala.concurrent.Future
// import akka.actor.ActorRef
// import akka.util.Timeout
// import spray.routing.{HttpService, RequestContext, AuthenticationFailedRejection, AuthenticationRequiredRejection}
// import spray.routing.authentication.Authentication
// import spray.http.HttpRequest
// import omnibus.spray.domain._
// import org.cakesolutions.akkapatterns.core.authentication.TokenCheck


// /**
//  * Mix in this trait to get the authentication directive. The ``validUser`` function can be used in Spray's
//  * ``authentication`` function.
//  *
//  * The usual pattern is
//  *
//  * {{{
//  * ... with AuthenticationDirectives {
//  *
//  *   ...
//  *
//  *   path("users/me/portfolio") {
//  *     authenticate(validUser) { userDetail =>
//  *     }
//  *   }
//  *   ...
//  * }
//  * }}}
//  *
//  * @author janmachacek
//  */
// trait AuthenticationDirectives extends UserFormats {
//   this: HttpService =>

//   /**
//    * @return a `User` that has been previously identified with the `Token` we have been given.
//    */
//   def doAuthenticate(token: UUID): Future[Option[UserDetailT[_]]]

//   /**
//    * @return the function that is usable in Spray's ``authenticate`` function, giving
//    *         routes access to a ``UserDetail`` instance
//    */
//   private def doValidUser[A <: UserKind](map: UserDetailT[_] => Authentication[UserDetailT[A]]): RequestContext => Future[Authentication[UserDetailT[A]]] = {
//     ctx: RequestContext =>
//       getToken(ctx.request) match {
//         case None => Future(Left(AuthenticationRequiredRejection("https", "patterns")))
//         case Some(token) => doAuthenticate(token) .map {
//           case Some(user) => map(user)
//           case None       => Left(AuthenticationFailedRejection("Patterns"))
//         }
//       }
//   }

//   // http://en.wikipedia.org/wiki/Universally_unique_identifier
//   val uuidRegex = """^\p{XDigit}{8}(-\p{XDigit}{4}){3}-\p{XDigit}{12}$""".r
//   def isUuid(token: String) = token.length == 36 && uuidRegex.findPrefixOf(token).isDefined

//   def getToken(request: HttpRequest): Option[UUID] = {
//     val query = request.queryParams.get("token")
//     if (query.isDefined && isUuid(query.get))
//       Some(UUID.fromString(query.get))
//     else {
//       val header = request.headers.find(_.name == "x-token")
//       if (header.isDefined && isUuid(header.get.value))
//         Some(UUID.fromString(header.get.value))
//       else
//         None
//     }
//   }

//   /**
//    * Checks that the token represents a valid user; i.e. someone is logged in. We make no assumptions about the roles
//    *
//    * @return the authentication of any user kind
//    */
//   def validUser: RequestContext => Future[Authentication[UserDetail]] = doValidUser(x => Right(x.asInstanceOf[UserDetailT[UserKind]]))

//   /**
//    * Checks that the token represents a valid superuser
//    *
//    * @return the authentication for superuser
//    */
//   def validSuperuser: RequestContext => Future[Authentication[UserDetailT[SuperuserKind.type]]] =
//     doValidUser { udc: UserDetailT[_] =>
//       udc.kind match {
//         case SuperuserKind => Right(new UserDetailT(udc.userReference, SuperuserKind))
//         case _ => Left(AuthenticationFailedRejection("Akka-Patterns"))
//       }
//     }

//   /**
//    * Checks that the token represents a valid customer
//    *
//    * @return the authentication for superuser
//    */
//   def validCustomer: RequestContext => Future[Authentication[UserDetailT[CustomerUserKind]]] = {
//     doValidUser { udc: UserDetailT[_] =>
//       udc.kind match {
//         case k: CustomerUserKind => Right(new UserDetailT(udc.userReference, k))
//         case _ => Left(AuthenticationFailedRejection("Akka-Patterns"))
//       }
//     }
//   }
// }


// trait DefaultAuthenticationDirectives extends AuthenticationDirectives {
//   this: HttpService =>

//   import akka.pattern.ask
//   implicit val timeout: Timeout
//   def loginActor: ActorRef

//   override def doAuthenticate(token: UUID) = (loginActor ? TokenCheck(token)).mapTo[Option[UserDetailT[_]]]

// }

