import unfiltered.request._
import unfiltered.response._

import directives._, Directives._

/*
ideas
 */
class ScratchPad {

  implicit def isSyntax[X](x:X) = new IsSyntax[X](x)

  class IsSyntax[X](x:X){
    def is[V, T, R, A](v:V)(implicit isd:IsDirective[X, V, T, R, A]) = isd.directive(x, v)
  }

  case class IsDirective[X, V, T, R, A](directive:(X, V) => Directive[T, R, A])

  object IsDirective {
    implicit val requestContentType = IsDirective{ (R:RequestContentType.type, value:String) =>
      when { case R(`value`) => value } orElse NotAcceptable
    }
  }

  for {
    _ <- POST
    _ <- RequestContentType is "application/json"
  } yield ()
}
