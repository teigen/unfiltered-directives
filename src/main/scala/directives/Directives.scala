package directives

import unfiltered.request._
import unfiltered.response._
import unfiltered.Cycle
import unfiltered.request.Accepts.Accepting

class Directive[-T, -R, +A](run:HttpRequest[T] => Result[R, A]) extends (HttpRequest[T] => Result[R, A]){
  import Directives.directive

  def apply(request: HttpRequest[T]) = run(request)

  def map[TT <: T, RR <: R, B](f:A => B):Directive[TT, RR, B] =
    directive(r => apply(r).map(f))

  def flatMap[TT <: T, RR <: R, B](f:A => Directive[TT, RR, B]):Directive[TT, RR, B] =
    directive(r => apply(r).flatMap(a => f(a)(r)))

  def orElse[TT <: T, RR <: R, B >: A](next: => Directive[TT, RR, B]):Directive[TT, RR, B] =
    directive(r => apply(r).orElse(next(r)))

  def | [TT <: T, RR <: R, B >: A](next: => Directive[TT, RR, B]):Directive[TT, RR, B] =
    orElse(next)

  def fail:FailDirective[T, R, A] = new FailDirective[T, R, A]{
    def map[X](f: ResponseFunction[R] => ResponseFunction[X]) =
      directive(apply(_).fail.map(f))
  }
}

trait FailDirective[-T, -R, +A]{
  def map[X](f:ResponseFunction[R] => ResponseFunction[X]):Directive[T, X, A]
  def ~> [RR <: R](and:ResponseFunction[RR]) = map(_ ~> and)
}

object Directives extends Directives with Intents

trait Directives {

  def directive[T, R, A](f:HttpRequest[T] => Result[R, A]) = new Directive(f)

  def result[R, A](r:Result[R, A]) = directive[Any, R, A](_ => r)

  def success[A](value:A) = result[Any, A](Success(value))

  def failure[R](r:ResponseFunction[R]) = result[R, Nothing](Failure(r))

  def error[R](r:ResponseFunction[R]) = result[R, Nothing](Error(r))

  def request[T] = directive[T, Any, HttpRequest[T]](Success(_))

  def commit[T, R, A](d:Directive[T, R, A]) = directive[T, R, A]{ r => d(r) match {
    case Failure(response) => Error(response)
    case result            => result
  }}

  def autocommit[T, R, A](d:Directive[T, R, A]):Directive[T, R, A] = new Directive[T, R, A](d){
    override def flatMap[TT <: T, RR <: R, B](f: (A) => Directive[TT, RR, B]) = commit(super.flatMap(f))
  }

  def getOrElse[R, A](opt:Option[A], orElse: => ResponseFunction[R]) = opt.map(success).getOrElse(failure(orElse))

  implicit def uMethod(M:Method) = autocommit(directive[Any, Any, Method]{
    case M(_) => Success(M)
    case _    => Failure(MethodNotAllowed)
  })

  def inputStream = autocommit(request[Any] map { _.inputStream })

  def reader = autocommit(request[Any] map { _.reader })

  def protocol = request[Any] map { _.protocol }

  def method = request[Any] map { _.method }

  def uri = request[Any] map { _.uri }

  def parameterNames = request[Any] map { _.parameterNames }

  def parameterValues(param: String) = request[Any] map { _.parameterValues(param) }

  def headerNames = request[Any] map { _.headerNames }

  def headers(name: String) = request[Any] map { _.headers(name) }

  def cookies = request[Any] map { case Cookies(cookies) => cookies }

  def isSecure = request[Any] map { _.isSecure }

  def remoteAddr = request[Any] map { _.remoteAddr }

  // extra helpers
  def contentType(what:String) = directive[Any, Any, String]{
    case RequestContentType(`what`) => Success(what)
    case _ => Failure(UnsupportedMediaType)
  }

  implicit def accepting(A:Accepting) = directive[Any, Any, Accepting]{
    case A(_) => Success(A)
    case _    => Failure(NotAcceptable)
  }

  implicit def function[A, X](f:HttpRequest[A] => X) = request[A].map(f)

  def parameter(name:String) = parameterValues(name).flatMap{
    p => getOrElse(p.headOption, BadRequest)
  }
}

trait Intents {
  def Intent[A, B](intent:PartialFunction[String, HttpRequest[A] => Result[B, ResponseFunction[B]]]):Cycle.Intent[A, B] = {
    case req if intent.isDefinedAt(req.uri) => intent(req.uri)(req) match {
      case Success(response) => response
      case Failure(response) => response
      case Error(response)   => response
    }
  }
}
