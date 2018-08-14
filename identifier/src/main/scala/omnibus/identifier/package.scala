package omnibus

import io.estatico.newtype.macros.newtype

package object identifier {

  @newtype case class Identifier[IdValue]( value: IdValue ) {
    def map[B]( f: IdValue => B ): Identifier[B] = Identifier( f( value ) )
    def flatMap[B]( f: IdValue => Identifier[B] ): Identifier[B] = f( value )
  }

//  type AlphaNum = Format.AlphaNum
//  type Num = Format.Num
//  type UUID = Format.UUID
//  type Regex = Format.Regex
}
