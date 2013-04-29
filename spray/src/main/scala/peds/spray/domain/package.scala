/*
 * Copyright (C) 2013 Damon Rolfs.  All rights reserved.
 * Heavily based on code copyright (C) 2013 by CakeSolutions.  All rights reserved.
 * see https://github.com/janm399/akka-patterns/tree/master/server/domain/src/main/scala/org/cakesolutions/akkapatterns/domain
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

package peds.spray

import com.eaio.uuid.UUID


package object domain {
  // import java.util.Date // DMR change to Joda LocalDate
  // import java.text.{ParseException, SimpleDateFormat}

  case class Implementation( title: String, version: String, build: String )

  /**
   * The common base for all errors in our application
   */
  trait ApplicationFailure

  /**
   * Convenience type alias for any kind of user
   */
  type UserDetail = UserDetailT[UserKind]

  /**
   * Type alias for customer identity
   *
   * NOTE: consider the alternative, using a `case class CustomerReference(id: UUID)`
   *       which is slightly more verbose but ensures type safety throughout the code.
   *       If your code has lots of UUIDs, you'll be *really* glad of type safe ids,
   *       trust me!
   */
  type CustomerReference = UUID

  /**
   * Type alias for user identity
   */
  type UserReference = UUID
}