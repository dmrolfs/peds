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


package peds.spray.akka.patterns

// import java.util.{UUID, Date}
// import java.net.URI
import spray.json._
// import spray.httpx.SprayJsonSupport
// import spray.httpx.marshalling.{CollectingMarshallingContext, MetaMarshallers, Marshaller}
// import spray.http.{HttpEntity, StatusCode}
import peds.spray.domain._
import peds.spray._


// Pure boilerplate - please create a code generator (I'll be your *best* friend!)
trait ApiMarshalling extends DefaultJsonProtocol
with UuidMarshalling with DateMarshalling {
  this: UserFormats =>

  implicit val NotRegisteredUserFormat = jsonFormat1( NotRegisteredUser )
  implicit val RegisteredUserFormat = jsonFormat1( RegisteredUser )

  implicit val ImplementationFormat = jsonFormat3( Implementation )
  // implicit val SystemInfoFormat = jsonFormat3( SystemInfo )
}
