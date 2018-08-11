package omnibus

import io.estatico.newtype.macros.newtype

package object identifier {

  @newtype case class Identifier[IdValue <: Serializable]( value: IdValue ) {
    def map[B <: Serializable]( f: IdValue => B ): Identifier[B] = Identifier.this( f( value ) )
    def flatMap[B <: Serializable]( f: IdValue => Identifier[B] ): Identifier[B] = f( value )
  }

//  type AlphaNum = Format.AlphaNum
//  type Num = Format.Num
//  type UUID = Format.UUID
//  type Regex = Format.Regex
}
