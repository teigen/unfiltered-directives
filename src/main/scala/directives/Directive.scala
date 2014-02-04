package directives

import unfiltered.request.HttpRequest
import unfiltered.response.ResponseFunction
import unfiltered.Cycle
import annotation.implicitNotFound
import Result.{Success, Failure, Error}

object Directive {

  def apply[T, R, A](run:HttpRequest[T] => Result[R, A]):Directive[T, R, A] =
    new Directive[T, R, A](run)

  trait Fail[-T, -R, +A]{
    def map[X](f:ResponseFunction[R] => ResponseFunction[X]):Directive[T, X, A]
    def ~> [RR <: R](and:ResponseFunction[RR]) = map(_ ~> and)
  }

  case class Intent[-T, +X](from:HttpRequest[T] => X){
    def apply[TT <: T, R](intent:PartialFunction[X, HttpRequest[TT] => Result[R, ResponseFunction[R]]]):Cycle.Intent[TT, R] = {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))(req) match {
        case Success(response) => response
        case Failure(response) => response
        case Error(response)   => response
      }
    }
  }

  @implicitNotFound("implicit instance of Directive.Eq[${X}, ${V}, ?, ?, ?] not found")
  case class Eq[-X, -V, -T, -R, +A](directive:(X, V) => Directive[T, R, A])

  @implicitNotFound("implicit instance of Directive.Gt[${X}, ${V}, ?, ?, ?] not found")
  case class Gt[-X, -V, -T, -R, +A](directive:(X, V) => Directive[T, R, A])

  @implicitNotFound("implicit instance of Directive.Lt[${X}, ${V}, ?, ?, ?] not found")
  case class Lt[-X, -V, -T, -R, +A](directive:(X, V) => Directive[T, R, A])
}

class Directive[-T, -R, +A](run:HttpRequest[T] => Result[R, A]) extends (HttpRequest[T] => Result[R, A]){

  def apply(request: HttpRequest[T]) = run(request)

  def map[TT <: T, RR <: R, B](f:A => B):Directive[TT, RR, B] =
    Directive(r => run(r).map(f))

  def flatMap[TT <: T, RR <: R, B](f:A => Directive[TT, RR, B]):Directive[TT, RR, B] =
    Directive(r => run(r).flatMap(a => f(a)(r)))
    
  def fold[RR,AA](fs:A=>Result[RR,AA], ff:ResponseFunction[R]=>Result[RR,AA], fe:ResponseFunction[R]=>Result[RR,AA]) = 
    Directive[T, RR, AA]{ r => run(r) match {
      case Success(s) => fs(s)
      case Failure(f) => ff(f)
      case Error(e)   => fe(e)
    }}  

  def orElse[TT <: T, RR <: R, B >: A](next: => Directive[TT, RR, B]):Directive[TT, RR, B] =
    Directive(r => run(r).orElse(next(r)))

  def | [TT <: T, RR <: R, B >: A](next: => Directive[TT, RR, B]):Directive[TT, RR, B] =
    orElse(next)

  def fail:Directive.Fail[T, R, A] = new Directive.Fail[T, R, A]{
    def map[B](f: ResponseFunction[R] => ResponseFunction[B]) =
      Directive(run(_).fail.map(f))
  }
}
