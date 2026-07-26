//> using file ../project.scala
//> using dep org.scalameta::munit::1.3.3

// Unit tests for web.scala's PURE failure classifier. No network is touched: the exceptions are
// constructed directly, exactly as the JDK throws them, which is the only way to test the offline
// classes (dns, refused, timeout) deterministically and fast.
//
// The wrapped-cause tests are the ones that matter. requests/HttpURLConnection bury the real
// transport error several layers deep, so a classifier that only looks at the outermost throwable
// reports "other" for nearly every real failure — which is the uninformative behaviour this replaced.

class WebFailSuite extends munit.FunSuite:
  import Web.NetFail

  test("a name that does not resolve is dns") {
    assertEquals(Web.classify(java.net.UnknownHostException("no-such-host.invalid")), NetFail.Dns)
  }

  test("nothing listening is refused") {
    assertEquals(Web.classify(java.net.ConnectException("Connection refused")), NetFail.Refused)
  }

  test("a connect that timed out is timeout, NOT refused (both are ConnectException)") {
    assertEquals(Web.classify(java.net.ConnectException("Connection timed out")), NetFail.Timeout)
  }

  test("a read timeout is timeout") {
    assertEquals(Web.classify(java.net.SocketTimeoutException("Read timed out")), NetFail.Timeout)
  }

  test("tls problems classify as tls, from either exception family") {
    assertEquals(Web.classify(javax.net.ssl.SSLHandshakeException("bad cert")), NetFail.Tls)
    assertEquals(Web.classify(java.security.cert.CertificateExpiredException("expired")), NetFail.Tls)
  }

  test("routing failures are unreachable") {
    assertEquals(Web.classify(java.net.NoRouteToHostException("no route")), NetFail.Unreachable)
  }

  test("a peer reset is reset, and does not fall through to refused") {
    assertEquals(Web.classify(java.net.SocketException("Connection reset")), NetFail.Reset)
  }

  test("an unrecognised failure is other, never a wrong confident class") {
    assertEquals(Web.classify(RuntimeException("something odd")), NetFail.Other)
    assertEquals(Web.classify(RuntimeException()), NetFail.Other)
  }

  test("the class is found through a WRAPPED cause — the real-world shape") {
    val wrapped = RuntimeException("requests failed", java.net.UnknownHostException("nope.invalid"))
    assertEquals(Web.classify(wrapped), NetFail.Dns)
  }

  test("the class is found several layers deep") {
    val deep = RuntimeException("outer",
      java.io.IOException("middle", java.net.ConnectException("Connection refused")))
    assertEquals(Web.classify(deep), NetFail.Refused)
  }

  test("causes walks the chain and terminates on a self-referencing cause") {
    final class Loop extends RuntimeException("loop"):
      override def getCause: Throwable = this
    assertEquals(Web.causes(Loop()).size, 1)
  }

  test("a null message never crashes the classifier or the line") {
    assertEquals(Web.classify(java.net.ConnectException()), NetFail.Refused)
    assert(Web.failureLine("http://x", java.net.ConnectException()).contains("[refused]"))
  }

  test("failureLine carries the class, the url, and a concrete next step") {
    val line = Web.failureLine("https://nope.invalid/x", java.net.UnknownHostException("nope.invalid"))
    assert(line.contains("[dns]"), line)
    assert(line.contains("https://nope.invalid/x"), line)
    assert(line.contains("did not resolve"), line)
  }

  // REGRESSION PIN. The first version of this classifier matched only JDK exception TYPES, and every
  // test above passed — while the real `tt web get <bad-host>` printed [other]. Cause: `requests` throws
  // its own requests.UnknownHostException, with the JDK exception nowhere in the chain. These cases
  // reproduce that shape, so a future refactor cannot quietly delete the text fallback.
  test("a client library's OWN exception type still classifies, via class name") {
    class UnknownHostException(msg: String) extends RuntimeException(msg) // shadows the JDK one on purpose
    val real = UnknownHostException("Unknown host null in url http://nope.invalid/")
    assertEquals(Web.classify(real), NetFail.Dns)
  }

  test("classification survives a null-message wrapper by reading the cause chain") {
    val outer = RuntimeException(null: String, java.net.ConnectException("Connection refused"))
    assertEquals(Web.classify(outer), NetFail.Refused)
  }

  test("classifyByText recognises the phrasings the JDK and libraries actually emit") {
    def cls(msg: String) = Web.classifyByText(RuntimeException(msg))
    assertEquals(cls("Name or service not known"), Some(NetFail.Dns))
    assertEquals(cls("Temporary failure in name resolution"), Some(NetFail.Dns))
    assertEquals(cls("Connection refused"), Some(NetFail.Refused))
    assertEquals(cls("connect timed out"), Some(NetFail.Timeout))
    assertEquals(cls("No route to host"), Some(NetFail.Unreachable))
    assertEquals(cls("Connection reset by peer"), Some(NetFail.Reset))
    assertEquals(cls("PKIX path building failed: certificate expired"), Some(NetFail.Tls))
    assertEquals(cls("something entirely unrelated"), None)
  }

  test("every class has a distinct label and a hint that is not just the label") {
    val all = NetFail.values.toVector
    assertEquals(all.map(Web.label).distinct.size, all.size)
    all.foreach(f => assert(Web.hint(f).length > Web.label(f).length + 8, s"weak hint for ${Web.label(f)}"))
  }
