/*
 * Copyright (C) 2013 Damon Rolfs.  All rights reserved.
 * Heavily based on code copyright (C) 2013 by CakeSolutions.  All rights reserved.
 * see https://github.com/janm399/scalad/tree/master/src/main/scala/org/eigengo/scalad/mongo/sprayjson
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

package omnibus

import java.util.Date
import java.util.{UUID => JUUID}
import java.text.{ParseException, SimpleDateFormat}

package object spray {
  trait JUuidChecker {
    def parseUuidString( token: String ): Option[JUUID] = {
      if ( token.length != 36 ) None
      else try Some( JUUID.fromString( token ) )
      catch {
        case p: IllegalArgumentException => return None
      }
    }
  }

  trait IsoDateChecker {
    private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
      override def initialValue() = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" )
    }

    def dateToIsoString( date: Date ) = localIsoDateFormatter.get.format( date )

    def parseIsoDateString( date: String ): Option[Date] = {
      if ( date.length != 28 ) None
      else try Some( localIsoDateFormatter.get.parse( date ) )
      catch {
        case p: ParseException => None
      }
    }
  }

}
