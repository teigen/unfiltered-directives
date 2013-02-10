import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}
import unfiltered.filter.Plan
import unfiltered.jetty.Http
import unfiltered.request._
import unfiltered.response._

trait Demo extends Plan with App {
  Http(8080).filter(this).run()
}

// curl -v http://localhost:8080/example/x
// 405 - method not allowed

// curl -v -XPOST http://localhost:8080/example/x
// 415 - unsupported media type

// curl -v -XPOST http://localhost:8080/example/x -H "Content-Type:application/json" -d '{ "x" : 5 }'
// 406 - not acceptable

// curl -v -XPOST http://localhost:8080/example/x -H "Content-Type:application/json" -d '{ "x" : 5 }' -H "Accept:application/json"

object Demo0 extends Demo {
  // good code, bad http

  def intent = {
    case req @ POST(Path(Seg(List("example", id)))) & Accepts.Json(RequestContentType("application/json")) =>
      Ok ~> JsonContent ~> ResponseBytes(Body.bytes(req))
  }
}

object Demo1 extends Demo {
  // bad code, good http
  def intent = {
    case req @ Path(Seg(List("example", id))) => req match {
      case POST(_) => req match {
        case RequestContentType("application/json") => req match {
          case Accepts.Json(_) =>
            Ok ~> JsonContent ~> ResponseBytes(Body.bytes(req))
          case _ => NotAcceptable
        }
        case _ => UnsupportedMediaType
      }
      case _ => MethodNotAllowed
    }
  }
}

object Demo2 extends Demo {
  // good code, good http
  import directives._, Directives._

  def intent = Intent{
    case Seg(List("example", id)) =>
      for {
        _ <- POST
        _ <- contentType("application/json")
        _ <- Accepts.Json
        b <- Body.bytes _
      } yield Ok ~> JsonContent ~> ResponseBytes(b)
  }
}

object Demo3 extends App {
  val http = Http(8080).filter(new DemoPlan3)
  http.current.setSessionHandler(new SessionHandler)
  http.run()
}

class DemoPlan3 extends Plan {
  import directives._, Directives._
  import javax.servlet.http.HttpServletRequest

  case class User(name:String)

  def servletRequest = request[HttpServletRequest].map(_.underlying)

  def session = servletRequest.map{ _.getSession }

  def user = session.flatMap{ s =>
    val u = Option(s.getAttribute("user")).map(_.asInstanceOf[User])
    getOrElse(u, Redirect("/login"))
  }

  def intent = Intent {
    case "/" =>
      for {
        _ <- GET
        u <- user
      } yield Html5(<h1>Hi {u.name}</h1>)

    case "/login" =>
      val get = for{ _ <- GET } yield
        Html5(
          <form action={"/login"} method="post">
            <input type="text" name="user"/>
            <input type="submit" value="login"/>
          </form>)

      // curl -v http://localhost:8080/login -XPOST
      val post = for{
        _ <- POST
        name <- parameter("user").fail ~> ResponseString("user required")
        s <- session
      } yield {
        s.setAttribute("user", User(name))
        Redirect("/")
      }

      get | post
  }
}