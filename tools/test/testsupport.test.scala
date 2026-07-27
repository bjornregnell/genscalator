//> using file ../project.scala
//> using jvm 21

// Shared filesystem helpers for the test suites. No @main, so this is not a tt verb and DispatchSuite
// does not count it; it lives in tools/test/ beside the suites that use it.

object TestFs:

  /** Recursive delete that also works on Windows.
    *
    * git creates its object files READ-ONLY. On POSIX that is irrelevant to deletion — permission to
    * unlink lives on the containing directory — but on Windows the read-only ATTRIBUTE blocks the
    * delete itself, so `os.remove.all` on any tree containing a `.git/` throws
    * AccessDeniedException on `.git\objects\..`. That threw from the `finally` of tests whose
    * assertions had already PASSED, which is why it read as 14 unrelated git failures rather than as
    * one cleanup bug (first seen when CI reached the suite on Windows, 2026-07-27).
    *
    * Clearing the attribute on every entry first is a no-op on POSIX, so the same path runs on all
    * three platforms and there is no OS branch to keep honest.
    */
  def removeAllForce(p: os.Path): Unit =
    if os.exists(p) then
      // walk BEFORE deleting; setWritable on a directory is what lets its entries be unlinked on Windows
      os.walk(p, includeTarget = true).foreach: q =>
        try q.toIO.setWritable(true) catch case _: Throwable => () // best effort: the delete below reports
      os.remove.all(p)
