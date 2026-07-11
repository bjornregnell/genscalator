package todo.client

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.Thenable.Implicits.*
import todo.{Todo, Json}

// The Scala.js + Laminar client. Talks to the JDK server's JSON API and shares the exact same `common` codec.
// Direct common style.
object TodoApp:
  private val todos = Var(List.empty[Todo])
  private val draft = Var("")

  private def refresh(): Unit =
    for
      r <- dom.fetch("/api/todos")
      body <- r.text()
    do todos.set(Todo.parseList(body))

  private def send(url: String, method: dom.HttpMethod, payload: Option[String] = None): Unit =
    val init = new dom.RequestInit {}
    init.method = method
    payload.foreach(p => init.body = p)
    for _ <- dom.fetch(url, init) do refresh()

  private def add(title: String): Unit =
    send("/api/todos", dom.HttpMethod.POST, Some(s"""{"title":${Json.quote(title)}}"""))
    draft.set("")

  private def row(t: Todo): HtmlElement =
    li(
      input(typ := "checkbox", checked := t.done, onClick --> (_ => send(s"/api/todos/${t.id}", dom.HttpMethod.PUT))),
      span(t.title, if t.done then textDecoration := "line-through" else emptyMod),
      button("x", onClick --> (_ => send(s"/api/todos/${t.id}", dom.HttpMethod.DELETE))),
    )

  val app: HtmlElement =
    div(
      h1("Todo"),
      div(
        input(placeholder := "new todo", value <-- draft, onInput.mapToValue --> draft),
        button("add", onClick --> (_ => if draft.now().nonEmpty then add(draft.now()))),
      ),
      ul(children <-- todos.signal.map(_.map(row))),
    )

  def init(): Unit =
    renderOnDomContentLoaded(dom.document.getElementById("app"), app)
    refresh()

@main def main(): Unit = TodoApp.init()
