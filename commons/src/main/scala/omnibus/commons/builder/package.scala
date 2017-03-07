package omnibus.commons

import com.github.harveywi.{ builder => HBuilder }


/*
 *   Many thanks to:
 *   shapeless-builder
 *   (c) William Harvey 2013
 *   harveywi@cse.ohio-state.edu
 *   
 *   shapeless-builder is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   shapeless-builder is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with shapeless-builder.  If not, see <http://www.gnu.org/licenses/>.
 */

package object builder {

  /**
   * Enables method-chaining builders for case classes of type `CC`.
   *
   * @example {{{
   *
   * // Define a case class
   * case class Foo(x: Int, y: String, z: Char)
   *
   * // Mix the HasBuilder trait in with its companion object
   * object Foo extends HasBuilder[Foo] {
   *   // Define objects corresponding to the case class constructor parameters:
   *   // X is a required parameter of type Int
   *   object X extends Param[Int](5)
   *
   *   // Y is an optional parameter of type String with default value "5"
   *   object Y extends OptParam[String]("5")
   *
   *   // Z is an optional parameter of type Char with default value '5'
   *   object Z extends OptParam[Char]('5')
   *
   *   // Establish the case class <=> HList isomorphism
   *   override val gen = Generic[Foo]
   *
   *   // Define the "fieldsContainer" by passing in an HList of the above objects.  The order of the
   *   // objects in the HList must correspond to the order of the case class constructor parameters.
   *   override val fieldsContainer = createFieldsContainer( X :: Y :: Z :: HNil )
   * }
   *
   * // [...]
   *
   * // Now you can create instances of the case class by using method-chaining builder incantations
   * import Foo._
   * val test = Foo.builder.set(X, 42).set(Z, '#').build
   *
   * // Yessssssss!
   * assert(foo == Foo(42, "5", '#'), "Nooooooooooo!")
   * }}}
   *
   * imported in omnibus library from original shapeless-builder project by William Harvey
   * @author William Harvey
   */
  type HasBuilder[CC] = HBuilder.HasBuilder[CC]
}
