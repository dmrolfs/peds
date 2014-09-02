// package peds.akka.persistence

// import akka.actor.ActorLogging
// import akka.actor.Actor.Receive
// import akka.persistence.PersistentActor


// trait PersistentActorStack extends PersistentActor { outer: ActorLogging =>
//   def aroundReceiveCommand( r: Receive ): Receive = {
//     case c if r.isDefinedAt( c ) => {
// log error s"PersistentActorStack.receiveCommand WRAPPED_RECIEVED IS DEFN for: ${c}"
//       r( c )
//     }
    
//     case c => {
// log error s"PersistentActorStack.receiveCommand WRAPPED_RECIEVED IS NOT DEFN for: ${c}"
//       unhandled( c )
//     }
//   }

//   def aroundReceiveRecover( r: Receive ): Receive = {
//     case e if r.isDefinedAt( e ) => r( e )
//     case e => unhandled( e )
//   }
// }
