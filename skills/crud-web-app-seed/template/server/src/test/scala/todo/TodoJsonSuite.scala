package todo

// A beginner-friendly test suite for the SHARED JSON codec (defined in `common`). Uses munit: each
// `test("name") { ... }` is one case, and `assertEquals(actual, expected)` checks a value. Run all tests with
// `sbt test`.
class TodoJsonSuite extends munit.FunSuite:

  test("a Todo round-trips through JSON (encode then parse gives back the same Todo)") {
    val t = Todo(1, "buy milk", done = false)
    assertEquals(Todo.parse(Todo.toJson(t)), t)
  }

  test("special characters in the title are escaped and restored") {
    val t = Todo(2, "say \"hi\"\nand \\ bye", done = true)
    assertEquals(Todo.parse(Todo.toJson(t)), t)
  }

  test("a list of Todos round-trips too") {
    val ts = List(Todo(1, "a", true), Todo(2, "b", false))
    assertEquals(Todo.parseList(Todo.listToJson(ts)), ts)
  }

  test("the encoded shape is exactly the JSON we expect") {
    assertEquals(Todo.toJson(Todo(7, "x", true)), """{"id":7,"title":"x","done":true}""")
  }
