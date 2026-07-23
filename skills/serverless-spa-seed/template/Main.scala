package todo

import com.raquo.laminar.api.L.*
import org.scalajs.dom

// A serverless (client-only) Scala.js + Laminar todo SPA. There is NO server: the whole state
// lives in the browser's localStorage, reloaded on start and saved on every change, using the
// same dependency-free `Todo` JSON codec the crud seed shares. Direct common style.
object TodoApp:
  private val storageKey = "serverless-spa.todos"

  private def load(): List[Todo] =
    Option(dom.window.localStorage.getItem(storageKey)) match
      case Some(json) if json.nonEmpty => try Todo.parseList(json) catch case _: Throwable => Nil
      case _                           => Nil

  private def save(ts: List[Todo]): Unit =
    dom.window.localStorage.setItem(storageKey, Todo.listToJson(ts))

  private val todos = Var(load())
  private val draft = Var("")

  // Reactive persistence: every change to the list is written straight back to localStorage.
  todos.signal.foreach(save)(using unsafeWindowOwner)

  private def nextId(ts: List[Todo]): Int = if ts.isEmpty then 1 else ts.map(_.id).max + 1

  private def add(title: String): Unit =
    todos.update(ts => ts :+ Todo(nextId(ts), title, false))
    draft.set("")

  private def toggle(id: Int): Unit =
    todos.update(_.map(t => if t.id == id then t.copy(done = !t.done) else t))

  private def remove(id: Int): Unit =
    todos.update(_.filterNot(_.id == id))

  private def row(t: Todo): HtmlElement =
    li(
      input(typ := "checkbox", checked := t.done, onClick --> (_ => toggle(t.id))),
      span(t.title, if t.done then textDecoration := "line-through" else emptyMod),
      button("x", onClick --> (_ => remove(t.id))),
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

@main def main(): Unit = TodoApp.init()
