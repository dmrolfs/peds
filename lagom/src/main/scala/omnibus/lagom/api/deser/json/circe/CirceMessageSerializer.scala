package omnibus.lagom.api.deser.json.circe

import akka.{ Done, NotUsed }

import scala.collection.immutable
import scala.util.control.NonFatal
import akka.util.ByteString
import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.{
  NegotiatedDeserializer,
  NegotiatedSerializer
}
import com.lightbend.lagom.scaladsl.api.deser.{ MessageSerializer, StrictMessageSerializer }
import com.lightbend.lagom.scaladsl.api.transport.{
  DeserializationException,
  MessageProtocol,
  SerializationException
}
import io.circe.{ Decoder, Encoder, Json }
import io.circe.parser._
import io.circe.syntax._
import org.slf4j.LoggerFactory

object CirceMessageSerializer extends LowPriorityCirceMessageSerializerImplicits {
  implicit val jsonMessageSerializer: StrictMessageSerializer[Json] = {
    new StrictMessageSerializer[Json] {

      private val defaultProtocol = MessageProtocol( Some( "application/json" ), None, None )

      private class JsonSerializer( override val protocol: MessageProtocol )
          extends NegotiatedSerializer[Json, ByteString] {
        override def serialize( message: Json ): ByteString = {
          try {
            ByteString.fromString( message.noSpaces, protocol.charset.getOrElse( "utf-8" ) )
          } catch {
            case NonFatal( ex ) => throw SerializationException( ex )
          }
        }
      }

      private object JsonDeserializer extends NegotiatedDeserializer[Json, ByteString] {
        override def deserialize( wire: ByteString ): Json = {
          log.error( s"""parsing wire:"${wire.decodeString( "utf-8" )}"  """ )
          val r1 = parse( wire.decodeString( "utf-8" ) )
          log.error( s"parsed wire: ${r1}" )

          r1.fold(
            ex => {
              log.error( s"parsed error", ex )
              throw DeserializationException( ex )
            },
            json => {
              log.error( s"GOOD parsed json: ${json.noSpaces}" )
              json
            }
          )
        }
      }

      override def deserializer(
        protocol: MessageProtocol
      ): NegotiatedDeserializer[Json, ByteString] = {
        JsonDeserializer
      }

      override def serializerForResponse(
        acceptedMessageProtocols: immutable.Seq[MessageProtocol]
      ): NegotiatedSerializer[Json, ByteString] = {
        new JsonSerializer(
          acceptedMessageProtocols
            .find { _.contentType.contains( "application/json" ) }
            .getOrElse { defaultProtocol }
        )
      }

      override def serializerForRequest: NegotiatedSerializer[Json, ByteString] = {
        new JsonSerializer( defaultProtocol )
      }

      override val acceptResponseProtocols: immutable.Seq[MessageProtocol] = {
        immutable.Seq( defaultProtocol )
      }
    }
  }
}

trait LowPriorityCirceMessageSerializerImplicits {
  protected val log = LoggerFactory.getLogger( "CirceMessageSerializer" )

  import com.lightbend.lagom.scaladsl.api.deser.MessageSerializer._

  implicit def jsonEncoderMessageSerializer[Message](
    implicit jsonMessageSerializer: MessageSerializer[Json, ByteString],
    encoder: Encoder[Message],
    decoder: Decoder[Message]
  ): StrictMessageSerializer[Message] = new StrictMessageSerializer[Message] {
    private class JsonEncoderSerializer( jsonSerializer: NegotiatedSerializer[Json, ByteString] )
        extends NegotiatedSerializer[Message, ByteString] {
      override def protocol: MessageProtocol = jsonSerializer.protocol

      override def serialize( message: Message ): ByteString = {
        val json = try {
          message.asJson
        } catch {
          case NonFatal( e ) => throw SerializationException( e )
        }
        jsonSerializer serialize json
      }
    }

    private class JsonDecoderDeserializer(
      jsonDeserializer: NegotiatedDeserializer[Json, ByteString]
    ) extends NegotiatedDeserializer[Message, ByteString] {
      override def deserialize( wire: ByteString ): Message = {
        log.error( s"""deserializing wire:"${wire.decodeString( "utf-8" )}"  """ )
        val json = jsonDeserializer deserialize wire
        json
          .as[Message]
          .fold(
            ex => throw DeserializationException( ex ),
            msg => msg
          )
      }
    }

    override def acceptResponseProtocols: immutable.Seq[MessageProtocol] = {
      jsonMessageSerializer.acceptResponseProtocols
    }

    override def deserializer(
      protocol: MessageProtocol
    ): NegotiatedDeserializer[Message, ByteString] = {
      new JsonDecoderDeserializer( jsonMessageSerializer deserializer protocol )
    }

    override def serializerForResponse(
      acceptedMessageProtocols: immutable.Seq[MessageProtocol]
    ): NegotiatedSerializer[Message, ByteString] = {
      new JsonEncoderSerializer(
        jsonMessageSerializer serializerForResponse acceptedMessageProtocols
      )
    }

    override def serializerForRequest: NegotiatedSerializer[Message, ByteString] =
      new JsonEncoderSerializer( jsonMessageSerializer.serializerForRequest )
  }

  implicit val NotUsedMessageSerializer: StrictMessageSerializer[NotUsed] = {
    com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.NotUsedMessageSerializer
  }

  implicit val DoneMessageSerializer: StrictMessageSerializer[Done] = {
    com.lightbend.lagom.scaladsl.api.deser.MessageSerializer.DoneMessageSerializer
  }

  //  implicit def sourceMessageSerializer[Message](
  //    implicit delegate: MessageSerializer[Message, ByteString]
  //  ): StreamedMessageSerializer[Message] = new StreamedMessageSerializer[Message] {
  //    private class SourceSerializer( delegate: NegotiatedSerializer[Message, ByteString] )
  //        extends NegotiatedSerializer[Source[Message, NotUsed], Source[ByteString, NotUsed]] {
  //      override def protocol: MessageProtocol = delegate.protocol
  //      override def serialize( messages: Source[Message, NotUsed] ) =
  //        messages.map( delegate.serialize )
  //    }
  //
  //    private class SourceDeserializer( delegate: NegotiatedDeserializer[Message, ByteString] )
  //        extends NegotiatedDeserializer[Source[Message, NotUsed], Source[ByteString, NotUsed]] {
  //      override def deserialize( wire: Source[ByteString, NotUsed] ) =
  //        wire.map( delegate.deserialize )
  //    }
  //
  //    override def acceptResponseProtocols: immutable.Seq[MessageProtocol] =
  //      delegate.acceptResponseProtocols
  //
  //    override def deserializer(
  //      protocol: MessageProtocol
  //    ): NegotiatedDeserializer[Source[Message, NotUsed], Source[ByteString, NotUsed]] =
  //      new SourceDeserializer( delegate.deserializer( protocol ) )
  //    override def serializerForResponse(
  //      acceptedMessageProtocols: immutable.Seq[MessageProtocol]
  //    ): NegotiatedSerializer[Source[Message, NotUsed], Source[ByteString, NotUsed]] =
  //      new SourceSerializer( delegate.serializerForResponse( acceptedMessageProtocols ) )
  //    override def serializerForRequest: NegotiatedSerializer[
  //      Source[Message, NotUsed],
  //      Source[ByteString, NotUsed]
  //    ] =
  //      new SourceSerializer( delegate.serializerForRequest )
  //  }

}
