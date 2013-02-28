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

  class IsDirective[X, V, T, R, A](val directive:(X, V) => Directive[T, R, A])

  object IsDirective {

    // the given machinery is just for getting reasonable type inference when defining IsDirective instances
    def given[X, V] = new Given[X, V]

    class Given[X, V]{
      def apply[T, R, A](f:(X, V) => Directive[T, R, A]) = new IsDirective[X, V, T, R, A](f)
    }

    implicit val requestContentType = given[RequestContentType.type, String]{ (_, value) =>
      when { case RequestContentType(`value`) => value } orElse NotAcceptable
    }
  }

  for {
    _ <- POST
    _ <- RequestContentType is "application/json"
  } yield ()
}
