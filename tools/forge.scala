//> using file project.scala
//> using file lib.scala
//> using file releaselib.scala
//> using jvm 21
//> using dep com.lihaoyi::requests:0.9.3
//> using dep com.lihaoyi::ujson:4.4.3
//> using dep com.lihaoyi::os-lib:0.11.8

// forge — typed client for a Forgejo/Gitea forge (default: Codeberg). Replaces hand-curling the REST API
// (a dual-use `curl` carrying a token on the command line) with a narrow, effect-declared tool.
//   READ verbs (releases, tags) need NO auth (public repos) → safe to allowlist (`Bash(tt forge releases *)`).
//   The one EFFECTFUL verb (release-create) reads its token from a fixed set of human-set env vars
//   (GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN) and, failing those, from an OS
//   credential helper (`keyring get` for Gitea, `gh auth token` for GitHub) — NEVER a flag, and never a
//   source the AGENT can name, so it still cannot point the tool at an arbitrary secret. The helper
//   fallback is a deliberate 2026-07-25 widening so the human need keep NO token in the environment at
//   all; see the CREDENTIAL HELPERS note below for the full trade. It prints an [audit] line before acting,
//   and is deliberately NOT blanket-allowlistable (creating a release should stay a visible, confirmed op).
//   tt forge whoami   [--gh | --gl | --url BASE]            # verify auth: print the token's login (never the token)
//   tt forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]   # lists asset names too
//   tt forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]
//   tt forge release-create <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
//                           [--prerelease] [--draft] [--target COMMITISH] [--url BASE]
//   tt forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--gh | --url BASE]
//                           # PATCH an existing release (draft-visible lookup); sends ONLY the provided fields
//   tt forge release-download <owner>/<repo> <tag> [--gh | --url BASE] [--pattern GLOB] [--dir D] [--verify]
//   tt forge release-upload <owner>/<repo> <tag> <file> [--name N] [--clobber] [--gh | --url BASE]
//   tt forge release-delete <owner>/<repo> <tag> [--gh | --url BASE] [--yes] [--allow-published]
//   tt forge asset-rm <owner>/<repo> <tag> <asset> [--gh | --url BASE] [--yes] [--allow-published]
//                           # remove ONE asset (issue 006); previews by default, --allow-published for a live release
//   tt forge file <owner>/<repo> <path> [--ref R] [--out F] [--max-bytes N] [--gh | --gl | --url BASE]
//                           # ONE repo file's contents (issue 007) — the remote sibling of `tt git show`
//   READ verbs for issues/PRs/branch protection (both dialects; --gh = GitHub, see GitHubApi below):
//   tt forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]
//   tt forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]
//   tt forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   # GitHub login/contribs/type
//                                                                                 # or GitLab name/email/commits
//   tt forge issue  <owner>/<repo> <n> [--gh | --url BASE]           # body + comments
//   tt forge pr     <owner>/<repo> <n> [--gh | --url BASE]           # merge state + body
//   tt forge pr-files <owner>/<repo> <n> [--gh | --url BASE]         # changed files: status, +/-, path
//   tt forge pr-diff  <owner>/<repo> <n> [--gh | --url BASE]         # raw unified diff
//   tt forge pr-commits <owner>/<repo> <n> [--gh | --url BASE] [--limit N]  # commits + credit-trailer check
//   tt forge pr-merge <owner>/<repo> <n> [--gh | --url BASE] [--method M] [--subject S] [--body-file F] [--yes]
//                                                                    # EFFECTFUL: previews by default, merges with --yes
//   tt forge protection <owner>/<repo> <branch> [--gh | --url BASE]  # protection rule (needs a token)
//   BASE defaults to https://codeberg.org
import scala.util.Try
import agenttools.{Lib, ReleaseLib}

// Helpers (die/token/hostOf/getJson/splitRepo/… and the opts types) scoped in this object so their generic
// names don't collide with other tools when the toolbox compiles together. Only the @main entry is top-level.
object Forge {
  private val DefaultBase = "https://codeberg.org"

  private def die(msg: String): Nothing = { System.err.println(s"forge: $msg"); sys.exit(2) }

  private def forgeUsage(): Nothing = die(
    "usage:\n" +
      "  forge whoami   [--gh | --gl | --url BASE]                 (verify auth: prints the token's login)\n" +
      "  forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]\n" +
      "  forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]\n" +
      "  forge issues <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]\n" +
      "  forge prs    <owner>/<repo> [--gh | --url BASE] [--state open|closed|all] [--limit N]\n" +
      "  forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]   (--gh/--gl only)\n" +
      "  forge issue  <owner>/<repo> <n> [--gh | --url BASE]             (body + comments)\n" +
      "  forge pr     <owner>/<repo> <n> [--gh | --url BASE]             (merge state + body)\n" +
      "  forge pr-files <owner>/<repo> <n> [--gh | --url BASE]           (changed files: status, +/-, path)\n" +
      "  forge pr-diff  <owner>/<repo> <n> [--gh | --url BASE]           (raw unified diff)\n" +
      "  forge pr-commits <owner>/<repo> <n> [--gh | --url BASE] [--limit N]   (commits + credit-trailer check)\n" +
      "  forge pr-merge <owner>/<repo> <n> [--gh | --url BASE] [--method merge|squash|rebase]\n" +
      "                 [--subject S] [--body-file F] [--yes]           (PREVIEWS by default; merges with --yes)\n" +
      "  forge protection <owner>/<repo> <branch> [--gh | --url BASE]    (needs a token)\n" +
      "  forge release-create <owner>/<repo> <tag> [--gh | --gl | --url BASE] [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--target C]\n" +
      "  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F] [--prerelease] [--draft] [--gh | --url BASE]\n" +
      "  forge release-download <owner>/<repo> <tag> [--gh | --url BASE] [--pattern GLOB] [--dir D] [--verify]   (finds DRAFTS too; --verify checks the .sha256)\n" +
      "  forge release-upload <owner>/<repo> <tag> <file> [--name N] [--clobber] [--gh | --url BASE]   (attach ONE file; refuses duplicate names unless --clobber)\n" +
      "  forge release-delete <owner>/<repo> <tag> [--gh | --url BASE] [--yes] [--allow-published]   (PREVIEWS by default; never deletes the git tag)\n" +
      "  forge asset-rm <owner>/<repo> <tag> <asset> [--gh | --url BASE] [--yes] [--allow-published]   (remove ONE asset; PREVIEWS by default)\n" +
      "  forge file <owner>/<repo> <path> [--ref R] [--out F] [--max-bytes N] [--gh | --gl | --url BASE]   (read ONE repo file; READ, but carries a token)\n" +
      "  Dialects for release-create: default = Gitea/Forgejo (--url BASE, default https://codeberg.org); --gh = GitHub (fixed api.github.com); --gl = GitLab (--url BASE, default https://gitlab.com).\n" +
      "  Tokens come ONLY from fixed env names (never a flag): Gitea = CODEBERG_TOKEN/FORGE_TOKEN, GitHub = GITHUB_TOKEN/GH_TOKEN, GitLab = GITLAB_TOKEN (GENSCALATOR_-prefixed variants win first)."
  )

  private val Help: String =
    """tt forge — Forgejo/Gitea forge client (default: Codeberg)
      |
      |Talks to a forge's REST API without hand-curling it: list releases and tags
      |(no auth needed on public repos), verify a token, and create or edit releases.
      |The token is read ONLY from human-set env vars — never from a flag.
      |
      |Usage:
      |  forge whoami   [--gh | --gl | --url BASE]               verify auth: print the token's
      |                                                          login (never the token itself);
      |                                                          --gh/--gl check the GitHub/GitLab
      |                                                          token the other verbs use
      |  forge releases <owner>/<repo> [--gh | --url BASE] [--limit N]
      |                                                          list releases, with each release's
      |                                                          asset names indented under it (READ)
      |  forge tags     <owner>/<repo> [--gh | --url BASE] [--limit N]  list tags (READ)
      |  forge issues <owner>/<repo> [--gh | --url BASE] [--state S] [--limit N]
      |                                                          list issues   (READ)
      |  forge prs    <owner>/<repo> [--gh | --url BASE] [--state S] [--limit N]
      |                                                          list PRs, head branch in [brackets]
      |  forge contributors <owner>/<repo> [--gh | --gl | --url BASE] [--limit N]
      |                                                          list contributors (READ; --gh/--gl only —
      |                                                          Gitea has no such endpoint)
      |  forge issue  <owner>/<repo> <n> [--gh | --url BASE]     show an issue + comments (READ)
      |  forge pr     <owner>/<repo> <n> [--gh | --url BASE]     show a PR: merge state + body (READ)
      |  forge pr-files <owner>/<repo> <n> [--gh | --url BASE]   list a PR's changed files:
      |                                                          status, +adds/-dels, path (READ)
      |  forge pr-diff  <owner>/<repo> <n> [--gh | --url BASE]   print a PR's raw unified diff (READ;
      |                                                          can be large — capture and Read)
      |  forge pr-commits <owner>/<repo> <n> [--gh | --url BASE] [--limit N]
      |                                                          list a PR's commits: short sha, date,
      |                                                          author, headline (READ); surfaces
      |                                                          Co-Authored-By/"Generated with" lines
      |                                                          and gives a one-line verdict — the
      |                                                          CONTRIBUTING.md pre-merge check
      |  forge pr-merge <owner>/<repo> <n> [--gh | --url BASE] [--method merge|squash|rebase]
      |                 [--subject S] [--body-file F] [--yes]
      |                       (EFFECTFUL: merge a PR. PREVIEWS by default — PR, state,
      |                        the exact merge subject — and applies only with --yes;
      |                        the default subject "Merge PR #<n>: <title>" satisfies
      |                        CONTRIBUTING.md's name-the-PR rule; the body comes from
      |                        a FILE so prose never rides a command line; refuses an
      |                        unmergeable PR; NEVER deletes the source branch)
      |  forge protection <owner>/<repo> <branch> [--gh | --url BASE]
      |                                                          show the protection rule (token)
      |  forge release-create <owner>/<repo> <tag> [--gh | --gl | --url BASE]
      |                       [--name S] [--body S | --body-file F]
      |                       [--prerelease] [--draft] [--target COMMITISH]
      |                       (create a release; three dialects — see below)
      |  forge release-edit   <owner>/<repo> <tag> [--name S] [--body S | --body-file F]
      |                       [--tag S] [--prerelease] [--draft] [--gh | --url BASE]
      |                       (PATCH an existing release; sends ONLY the provided fields;
      |                        --gh edits on GitHub and finds DRAFTS too; --tag re-points a
      |                        draft's tag, e.g. after the UI reset it to untagged-...)
      |  forge release-upload <owner>/<repo> <tag> <file> [--name N] [--clobber] [--gh | --url BASE]
      |                       (attach ONE file to an existing release, drafts included;
      |                        refuses a duplicate asset name unless --clobber, which REPLACES
      |                        the bytes under that name — a conscious flag, never silent, and
      |                        loudly audited when the release is already PUBLISHED)
      |  forge asset-rm <owner>/<repo> <tag> <asset> [--gh | --url BASE]
      |                       [--yes] [--allow-published]
      |                       (DESTRUCTIVE: remove ONE asset from a release. Previews by
      |                        default, applies with --yes; a PUBLISHED release additionally
      |                        needs --allow-published, because removing an asset makes a
      |                        download URL 404 that may sit in someone's install script.
      |                        The release and its other assets are untouched. To swap bytes
      |                        under the same name, prefer release-upload --clobber)
      |  forge file <owner>/<repo> <path> [--ref R] [--out F] [--max-bytes N]
      |                       [--gh | --gl | --url BASE]
      |                       (READ ONE file's contents out of a repo — the remote sibling of
      |                        `tt git show`. Unlike `tt web get`, this one CARRIES A TOKEN, so
      |                        it reaches a private repo without a shallow clone or a raw curl;
      |                        same fixed-env token machinery and trusted-host guard as the
      |                        effectful verbs. --ref defaults to the default branch; without
      |                        --out the bytes print to stdout, capped at 5 MB)
      |  forge release-download <owner>/<repo> <tag> [--gh | --url BASE]
      |                       [--pattern GLOB] [--dir D] [--verify]
      |                       (download release assets; finds DRAFTS too, which the tags
      |                        endpoint cannot; --verify checks each payload against its
      |                        downloaded .sha256 and says UNVERIFIED when there is none)
      |  forge release-delete <owner>/<repo> <tag> [--gh | --url BASE]
      |                       [--yes] [--allow-published]
      |                       (DESTRUCTIVE: previews by default like `tt sub`, applies only
      |                        with --yes; a PUBLISHED release additionally needs
      |                        --allow-published; the git TAG is never deleted)
      |Flags:
      |  --url BASE        forge base URL (Gitea default https://codeberg.org; GitLab default https://gitlab.com)
      |  --limit N         max items for releases/tags (default 50)
      |  --name S          release title (default: the tag)
      |  --body S          release notes inline; --body-file F reads them from a file
      |  --prerelease      mark as prerelease (Gitea/GitHub only; GitLab has no such flag)
      |  --draft           mark as draft (Gitea/GitHub only; GitLab has no such flag)
      |  --target C        commitish/ref the new tag points at (release-create only)
      |  --gh              talk to the GitHub API (fixed api.github.com) instead of a Gitea forge
      |  --gl              talk to the GitLab API (--url BASE, default https://gitlab.com)
      |  --state S         open | closed | all for issues/prs (default open)
      |  --method M        pr-merge: merge | squash | rebase (default merge; squash discards
      |                    the contributors' commit boundaries — see pr-commits)
      |  --subject S       pr-merge: merge commit subject (default "Merge PR #<n>: <title>")
      |  --yes             pr-merge/release-delete/asset-rm: actually apply (all PREVIEW by default)
      |  --allow-published release-delete/asset-rm: the second flag a live release needs
      |  --clobber         release-upload: replace an existing asset's bytes under the same name
      |  --ref R           file: branch, tag or sha to read from (default: the default branch)
      |  --out F           file: write the bytes to F instead of printing them
      |  --max-bytes N     file: refuse to print more than N bytes (default 5000000)
      |
      |Token: whoami, release-create/edit/upload/delete and protection read the token from env
      |GENSCALATOR_CODEBERG_TOKEN, then CODEBERG_TOKEN, then FORGE_TOKEN — never a flag,
      |and it is only ever sent to a trusted host (codeberg.org; the human may extend
      |the set via env TT_FORGE_HOSTS). Effectful verbs print an [audit] line first.
      |GitHub verbs (--gh) read their token from env GENSCALATOR_GITHUB_TOKEN, GITHUB_TOKEN
      |or GH_TOKEN and only ever send it to api.github.com; reads work anonymously too
      |(60 requests/h), protection requires the token (admin-scoped read). release-create
      |--gh needs it too (Contents: read-and-write, or the classic `repo` scope).
      |GitLab release-create (--gl) reads its token from env GENSCALATOR_GITLAB_TOKEN or
      |GITLAB_TOKEN (scope: api), sends it as PRIVATE-TOKEN, and only ever to a trusted host
      |(gitlab.com; the HUMAN extends the set via env TT_FORGE_GITLAB_HOSTS or TT_FORGE_HOSTS
      |for self-managed instances like git.cs.lth.se).
      |
      |Examples:
      |  tt forge releases bjornregnell/genscalator --limit 5    # latest 5 releases
      |  tt forge tags bjornregnell/genscalator                  # tag list with short SHAs
      |  tt forge release-create bjornregnell/genscalator v0.9.0 --name "v0.9.0: title" \
      |           --body-file NOTES.md --prerelease                # Gitea/Codeberg (default)
      |  tt forge release-create bjornregnell/prontopop v0.1.1 --gh --name "v0.1.1" \
      |           --body-file NOTES.md                             # GitHub
      |  tt forge release-create bjornregnell/prontopop v0.1.1 --gl \
      |           --url https://git.cs.lth.se --name "v0.1.1" --body-file NOTES.md   # GitLab
      |
      |Full reference: tools/README.md""".stripMargin

  // ================================================================================================
  // The release client. Credential acquisition, the trusted-host guard, the dialect logic and the whole
  // download/verify path MOVED to `releaselib.scala` on 2026-07-27 (D7a), because `tt update --native`
  // needs exactly the same machinery and the toolbox's dependency graph is deliberately FLAT — tools
  // depend on shared libs, never on each other.
  //
  // What remains below are FORWARDERS, not second definitions: the same move `globMatches` already made
  // to `Lib`. They exist so this file's ~40 existing call sites keep reading the way they did, while
  // there is exactly ONE definition of each security-relevant rule, in one reviewable place.
  // ================================================================================================
  private val rl = ReleaseLib.Client("forge")

  private def TokenEnvNames   = rl.TokenEnvNames
  private def GhTokenEnvNames = rl.GhTokenEnvNames
  private def token           = rl.token
  private def ghToken         = rl.ghToken
  private def trustedHosts    = rl.trustedHosts
  private def ghHeaders       = rl.ghHeaders
  private def isGitHub(base: String)  = rl.isGitHub(base)
  private def hostOf(url: String)     = ReleaseLib.hostOf(url)
  private def apiBase(url: String)    = ReleaseLib.apiBase(url)
  private def splitRepo(s: String)    = rl.splitRepo(s)

  // The GitHub REST root — a FIXED constant, never derived from --url, so the GitHub token can only ever
  // travel to that one host (the same no-redirect rule trustedHosts enforces for the Gitea token).
  private def GitHubApi = rl.GitHubApi

  // GitLab dialect. Unlike GitHub, GitLab has self-managed instances (git.cs.lth.se, …), so the base URL IS
  // configurable via --url — which means the trusted-host guard matters here exactly as it does for the Gitea
  // token: the token only ever travels to a host in gitlabTrustedHosts. Default: gitlab.com; the HUMAN extends
  // the set via env TT_FORGE_GITLAB_HOSTS (or the shared TT_FORGE_HOSTS) — never a flag. Auth header is
  // `PRIVATE-TOKEN` (not `Authorization: token`); token from fixed human-set env names only.
  private val GlTokenEnvNames = List("GENSCALATOR_GITLAB_TOKEN", "GITLAB_TOKEN")
  private def glToken: Option[String] =
    GlTokenEnvNames.iterator.flatMap(sys.env.get).map(_.trim).find(_.nonEmpty)
  private def gitlabTrustedHosts: Set[String] =
    val extra = List("TT_FORGE_GITLAB_HOSTS", "TT_FORGE_HOSTS").iterator
      .flatMap(n => sys.env.getOrElse(n, "").split(",")).map(_.trim).filter(_.nonEmpty).toSet
    Set("gitlab.com") ++ extra

  private def userLogin(v: ujson.Value): String =
    Try(v.obj("user").obj("login").str).getOrElse("?")

  // one issue/PR per line: number, updated, author, title (tab-separated, like releases/tags)
  private def itemLine(v: ujson.Value): String =
    val num     = Try(v.obj("number").num.toLong).getOrElse(0L)
    val title   = Try(v.obj("title").str).getOrElse("?")
    val updated = Try(v.obj("updated_at").str).getOrElse("")
    s"#$num\t$updated\t${userLogin(v)}\t$title"

  // See ReleaseLib.strOr for WHY this predicate exists — it is the null-vs-absent trap a forge walks into.
  private def strOr(v: Option[ujson.Value], default: String) = ReleaseLib.strOr(v, default)
  private def strOrEmpty(v: Option[ujson.Value])             = ReleaseLib.strOrEmpty(v)

  def dispatch(args: String*): Unit =
    if args.contains("--help") || args.contains("-h") then { println(Help); sys.exit(0) }
    args.toList match
      case "whoami" :: rest         => whoami(rest)
      case "releases" :: rest       => listReleases(rest)
      case "tags" :: rest           => listTags(rest)
      case "issues" :: rest         => listIssues(rest)
      case "prs" :: rest            => listPrs(rest)
      case "contributors" :: rest   => listContributors(rest)
      case "issue" :: rest          => showIssue(rest)
      case "pr" :: rest             => showPr(rest)
      case "pr-files" :: rest       => prFiles(rest)
      case "pr-diff" :: rest        => prDiff(rest)
      case "pr-commits" :: rest     => prCommits(rest)
      case "pr-merge" :: rest       => prMerge(rest)
      case "protection" :: rest     => showProtection(rest)
      case "release-create" :: rest => releaseCreate(rest)
      case "release-edit" :: rest   => releaseEdit(rest)
      case "release-download" :: rest => releaseDownload(rest)
      case "release-upload" :: rest => releaseUpload(rest)
      case "release-delete" :: rest => releaseDelete(rest)
      case "asset-rm" :: rest       => assetRm(rest)
      case "file" :: rest           => repoFileRead(rest)
      case _                        => forgeUsage()

  // whoami — authenticated READ (GET /user) to verify the token inherits + is valid. Prints only the login and
  // which source supplied the token (NEVER the token). Trusted-host-guarded like release-create.
  // --gh/--gl added for issue 035: the one verb whose job is "check my auth" could only check the Gitea
  // token, so running it before a GitHub release reported a Codeberg 403 that read as a problem and was
  // not one. Each dialect follows its own verbs' rules exactly: same token sources, same auth header,
  // same fixed-root/trusted-host guard.
  private def whoami(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], base: String, dialect: Dialect): (String, Dialect) =
      rest match
        case Nil                                => (base, dialect)
        case "--url" :: u :: t                  => go(t, u, dialect)
        case "--gh" :: t                        => go(t, base, Dialect.GitHub)
        case "--gl" :: t                        => go(t, base, Dialect.GitLab)
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case other :: _                         => die(s"unexpected argument '$other'")
    val (base, dialect) = go(args, DefaultBase, Dialect.Gitea)
    // ONE reporter for all three dialects, so the output shape cannot drift between them.
    def report(url: String, headers: Map[String, String], loginKey: String, src: String): Unit =
      val host = hostOf(url)
      val r = Try(requests.get(url, headers = headers,
        check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
      r.statusCode match
        case 200 =>
          val login = Try(ujson.read(r.text()).obj.get(loginKey).map(_.str).getOrElse("?")).getOrElse("?")
          println(s"authenticated as $login on $host (token from $src)")
        case 401 => die(s"token present but rejected (401) by $host — check the token / its scope")
        case c   => die(s"GET $url -> $c ${r.statusMessage}")
    def envSrc(names: List[String], fallback: String): String =
      names.find(n => sys.env.get(n).exists(_.trim.nonEmpty)).map("env " + _).getOrElse(fallback)
    dialect match
      case Dialect.Gitea =>
        val tok  = token.getOrElse(die(s"whoami needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
        val url  = s"${apiBase(base)}/user"
        val host = hostOf(url)
        if !trustedHosts.contains(host) then die(
          s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")} (extend via env TT_FORGE_HOSTS).")
        report(url, Map("Authorization" -> s"token $tok"), "login", envSrc(TokenEnvNames, "the OS keyring"))
      case Dialect.GitHub =>
        if base != DefaultBase then die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
        val tok = ghToken.getOrElse(die(
          s"whoami --gh needs a token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag), or logs in once with `gh auth login`."))
        report(s"$GitHubApi/user",
          Map("Accept" -> "application/vnd.github+json", "Authorization" -> s"Bearer $tok"),
          "login", envSrc(GhTokenEnvNames, "`gh auth token`"))
      case Dialect.GitLab =>
        val glBase = if base == DefaultBase then "https://gitlab.com" else base // default gitlab.com, not codeberg
        val tok = glToken.getOrElse(die(
          s"whoami --gl needs a token — the HUMAN sets one of env ${GlTokenEnvNames.mkString(", ")} (never a flag)."))
        val host = hostOf(glBase)
        if !gitlabTrustedHosts.contains(host) then die(
          s"refusing to send the token to untrusted host '$host'. Trusted: ${gitlabTrustedHosts.toVector.sorted.mkString(", ")} (extend via env TT_FORGE_GITLAB_HOSTS or TT_FORGE_HOSTS).")
        report(s"${glBase.stripSuffix("/")}/api/v4/user", Map("PRIVATE-TOKEN" -> tok),
          "username", envSrc(GlTokenEnvNames, "?"))

  private final case class ReadOpts(repo: Option[String], base: String, limit: Int, state: String)

  private def parseRead(args: List[String]): ReadOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ReadOpts): ReadOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u))
        case "--gh" :: t         => go(t, o.copy(base = "https://github.com"))
        case "--state" :: s :: t =>
          if Set("open", "closed", "all").contains(s) then go(t, o.copy(state = s))
          else die(s"--state must be open, closed or all, got '$s'")
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty            => go(t, o.copy(repo = Some(r)))
        case other :: _                          => die(s"unexpected argument '$other'")
    go(args, ReadOpts(None, DefaultBase, 50, "open"))

  private def getJson(url: String, headers: Map[String, String] = Map.empty): ujson.Value =
    val r = Try(requests.get(url, headers = headers, check = false, readTimeout = 30000, connectTimeout = 10000))
      .getOrElse(die(s"request failed: $url"))
    if r.statusCode != 200 then die(s"GET $url -> ${r.statusCode} ${r.statusMessage}")
    Try(ujson.read(r.text())).getOrElse(die(s"unexpected (non-JSON) response from $url"))

  private def listReleases(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    // --gh sets base to github.com, but this verb used to build a Gitea path against it regardless,
    // producing https://github.com/api/v1/... -> 410 Gone. Route on the dialect like every other
    // dual-dialect read verb does. GitHub spells the page size per_page, Gitea limit.
    val gh = isGitHub(o.base)
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/releases?per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/releases?limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of releases"))
    if arr.isEmpty then println("(no releases)")
    else
      arr.foreach { rel =>
        val tag   = strOr(rel.obj.get("tag_name"), "?")
        val name  = strOrEmpty(rel.obj.get("name"))
        val pre   = rel.obj.get("prerelease").exists(_.bool)
        val draft = rel.obj.get("draft").exists(_.bool)
        val pub   = strOrEmpty(rel.obj.get("published_at"))
        val flags = (if draft then " [draft]" else "") + (if pre then " [prerelease]" else "")
        println(s"$tag\t$pub$flags\t$name")
        // Asset names, indented under their release. Both dialects expose `assets`, and this is
        // what makes "which platforms does that release actually ship?" answerable here instead
        // of by hand-curling the API — the gap that sent a caller back to a raw forge client.
        val assets = rel.obj.get("assets").toList.flatMap(a => Try(a.arr.toList).getOrElse(Nil))
        assets.foreach { a =>
          val an = strOr(a.obj.get("name"), "?")
          println(s"\t  $an")
        }
      }

  private def listTags(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val gh = isGitHub(o.base) // same dialect bug as releases had; see the note there
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/tags?per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/tags?limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of tags"))
    if arr.isEmpty then println("(no tags)")
    else
      arr.foreach { t =>
        val name = strOr(t.obj.get("name"), "?")
        val sha  = Try(t.obj("commit").obj("sha").str).getOrElse("").take(10)
        println(s"$name\t$sha")
      }

  // ---- issue / PR / branch-protection READ verbs (both dialects; --gh = GitHub) ----

  private def listIssues(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val arr =
      if isGitHub(o.base) then
        // GitHub's /issues endpoint interleaves PRs — drop entries carrying a pull_request key
        Try(getJson(s"$GitHubApi/repos/$owner/$repo/issues?state=${o.state}&per_page=${o.limit}", ghHeaders).arr)
          .getOrElse(die("expected a JSON array of issues")).filterNot(_.obj.contains("pull_request"))
      else
        Try(getJson(s"${apiBase(o.base)}/repos/$owner/$repo/issues?state=${o.state}&type=issues&limit=${o.limit}").arr)
          .getOrElse(die("expected a JSON array of issues"))
    arr.foreach(i => println(itemLine(i)))
    println(s"=== ${arr.size} ${o.state} issues")

  private def listPrs(args: List[String]): Unit =
    val o             = parseRead(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val gh            = isGitHub(o.base)
    val url =
      if gh then s"$GitHubApi/repos/$owner/$repo/pulls?state=${o.state}&per_page=${o.limit}"
      else s"${apiBase(o.base)}/repos/$owner/$repo/pulls?state=${o.state}&limit=${o.limit}"
    val arr = Try(getJson(url, if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of pull requests"))
    arr.foreach { p =>
      val head = Try(p.obj("head").obj("ref").str).getOrElse("?")
      println(s"${itemLine(p)}\t[$head]")
    }
    println(s"=== ${arr.size} ${o.state} PRs")

  // contributors — READ the repo's contributor list (the "who does the forge think contributed" verb; born
  // from the SM217 investigation, where verifying it needed raw `tt web get` on the API). Reuses the SM207
  // --gh/--gl dialect routing. GitHub prints login/contributions/type (type = User|Bot — the thing that
  // answers "why is a bot on the list"); GitLab prints name/email/commits. The Gitea/Forgejo REST API has NO
  // contributors endpoint (verified 2026-07-24: Codeberg returns 404), so the default dialect says so plainly
  // rather than 404-ing cryptically. Anonymous read for GitLab (no token → no trusted-host surface); GitHub
  // sends its token only to the fixed api.github.com root (ghHeaders), same no-redirect rule as the other reads.
  private final case class ContribOpts(repo: Option[String], base: String, limit: Int, dialect: Dialect)

  private def listContributors(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: ContribOpts): ContribOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u)) // base only; dialect stays as --gh/--gl set it (default Gitea)
        case "--gh" :: t         => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: t         => go(t, o.copy(dialect = Dialect.GitLab))
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty            => go(t, o.copy(repo = Some(r)))
        case other :: _                          => die(s"unexpected argument '$other'")
    val o             = go(args, ContribOpts(None, DefaultBase, 50, Dialect.Gitea))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    o.dialect match
      case Dialect.GitHub =>
        val arr = Try(getJson(s"$GitHubApi/repos/$owner/$repo/contributors?per_page=${o.limit}", ghHeaders).arr)
          .getOrElse(die("expected a JSON array of contributors"))
        arr.foreach { c =>
          val login = Try(c.obj("login").str).getOrElse("?")
          val n     = Try(c.obj("contributions").num.toLong).getOrElse(0L)
          val typ   = Try(c.obj("type").str).getOrElse("")
          println(s"$login\t$n\t$typ")
        }
        println(s"=== ${arr.size} contributors")
      case Dialect.GitLab =>
        val base = if o.base == DefaultBase then "https://gitlab.com" else o.base // default gitlab.com, not codeberg
        val proj = s"$owner%2F$repo" // GitLab wants the project path URL-encoded ('/' -> %2F)
        val arr = Try(getJson(s"${base.stripSuffix("/")}/api/v4/projects/$proj/repository/contributors?per_page=${o.limit}").arr)
          .getOrElse(die("expected a JSON array of contributors"))
        arr.foreach { c =>
          val name    = Try(c.obj("name").str).getOrElse("?")
          val email   = Try(c.obj("email").str).getOrElse("")
          val commits = Try(c.obj("commits").num.toLong).getOrElse(0L)
          println(s"$name\t$email\t$commits")
        }
        println(s"=== ${arr.size} contributors")
      case Dialect.Gitea =>
        die("contributors is supported for --gh (GitHub) and --gl (GitLab) only; the Gitea/Forgejo REST API\n" +
          "  has no contributors endpoint (Codeberg returns 404) — read the contributor graph from the web UI.")

  private final case class ItemOpts(repo: Option[String], item: Option[String], base: String)

  private def parseItem(args: List[String]): ItemOpts =
    @annotation.tailrec
    def go(rest: List[String], o: ItemOpts): ItemOpts =
      rest match
        case Nil               => o
        case "--url" :: u :: t => go(t, o.copy(base = u))
        case "--gh" :: t       => go(t, o.copy(base = "https://github.com"))
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty => go(t, o.copy(repo = Some(r)))
        case i :: t if o.item.isEmpty => go(t, o.copy(item = Some(i)))
        case other :: _               => die(s"unexpected argument '$other'")
    go(args, ItemOpts(None, None, DefaultBase))

  private def showIssue(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n    = o.item.flatMap(_.toIntOption).getOrElse(die("expected an issue number after <owner>/<repo>"))
    val gh   = isGitHub(o.base)
    val root = if gh then s"$GitHubApi/repos/$owner/$repo/issues/$n" else s"${apiBase(o.base)}/repos/$owner/$repo/issues/$n"
    val hdrs = if gh then ghHeaders else Map.empty[String, String]
    val issue = getJson(root, hdrs)
    println(itemLine(issue))
    println(s"state: ${Try(issue.obj("state").str).getOrElse("?")}")
    println("")
    println(strOrEmpty(issue.obj.get("body")))
    val commentsUrl = if gh then s"$root/comments?per_page=100" else s"$root/comments"
    val comments = Try(getJson(commentsUrl, hdrs).arr).getOrElse(die("expected a JSON array of comments"))
    comments.foreach { c =>
      println(s"\n--- comment by ${userLogin(c)} at ${Try(c.obj("created_at").str).getOrElse("?")} ---")
      println(strOrEmpty(c.obj.get("body")))
    }
    println(s"\n=== ${comments.size} comments")

  private def showPr(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n   = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh  = isGitHub(o.base)
    val url = if gh then s"$GitHubApi/repos/$owner/$repo/pulls/$n" else s"${apiBase(o.base)}/repos/$owner/$repo/pulls/$n"
    val pr  = getJson(url, if gh then ghHeaders else Map.empty)
    println(itemLine(pr))
    val baseRef   = Try(pr.obj("base").obj("ref").str).getOrElse("?")
    val headRef   = Try(pr.obj("head").obj("ref").str).getOrElse("?")
    val state     = Try(pr.obj("state").str).getOrElse("?")
    val merged    = Try(pr.obj("merged").bool).getOrElse(false)
    val mergeable = pr.obj.get("mergeable").map(v => Try(v.bool).map(_.toString).getOrElse("computing")).getOrElse("?")
    val mergeState = if gh then s"  merge_state=${Try(pr.obj("mergeable_state").str).getOrElse("?")}" else ""
    println(s"state: $state  merged=$merged  mergeable=$mergeable$mergeState  $headRef -> $baseRef")
    println("")
    println(strOrEmpty(pr.obj.get("body")))

  /** PURE URL builders for the PR read verbs, public for unit tests (SM207's testable-request rule).
    * GitHub serves a PR's diff by CONTENT NEGOTIATION on the pull itself (Accept:
    * application/vnd.github.diff), so prDiffUrl --gh is the pulls endpoint with no suffix;
    * Gitea/Forgejo serves it at the .diff suffix. Both follow the fixed-GitHubApi-root rule. */
  def prFilesUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, n: Int): String =
    if isGh then s"$GitHubApi/repos/$owner/$repo/pulls/$n/files?per_page=100"
    else s"$apiRoot/repos/$owner/$repo/pulls/$n/files?limit=100"

  def prDiffUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, n: Int): String =
    if isGh then s"$GitHubApi/repos/$owner/$repo/pulls/$n"
    else s"$apiRoot/repos/$owner/$repo/pulls/$n.diff"

  // pr-files / pr-diff — READ a PR's changed-file list and raw unified diff. Born 2026-08-07:
  // reviewing the first external alpha PR needed its content, and the only shapes were a raw
  // `gh pr diff` or the web UI — the raw reach was denied and the typed verbs ordered instead.
  private def prFiles(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n   = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh  = isGitHub(o.base)
    val arr = Try(getJson(prFilesUrl(gh, apiBase(o.base), owner, repo, n), if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of changed files"))
    val rows = arr.toList.map { f =>
      (strOr(f.obj.get("status"), "?"),
        Try(f.obj("additions").num.toLong).getOrElse(0L),
        Try(f.obj("deletions").num.toLong).getOrElse(0L),
        strOr(f.obj.get("filename"), "?"))
    }
    rows.foreach((st, a, d, name) => println(s"$st\t+$a/-$d\t$name"))
    println(s"=== ${rows.size} files changed  +${rows.map(_._2).sum}/-${rows.map(_._3).sum}")
    if rows.size >= 100 then println("(100-file page cap reached — more files may exist; pagination not implemented)")

  private def prDiff(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n    = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh   = isGitHub(o.base)
    val url  = prDiffUrl(gh, apiBase(o.base), owner, repo, n)
    val hdrs = if gh then ghHeaders + ("Accept" -> "application/vnd.github.diff") else Map.empty[String, String]
    val r = Try(requests.get(url, headers = hdrs, check = false, readTimeout = 60000, connectTimeout = 10000))
      .getOrElse(die(s"request failed: $url"))
    r.statusCode match
      case 200 => print(r.text())
      case 404 => die(s"PR #$n not found (404)")
      case c   => die(s"GET $url -> $c ${r.statusMessage}")

  /** PURE URL builder for pr-commits, public for unit tests (SM207's testable-request rule).
    * GitHub spells the page size per_page (server cap 100), Gitea/Forgejo spells it limit; both
    * follow the fixed-GitHubApi-root rule of the other pr-* builders. */
  def prCommitsUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, n: Int, limit: Int): String =
    if isGh then s"$GitHubApi/repos/$owner/$repo/pulls/$n/commits?per_page=$limit"
    else s"$apiRoot/repos/$owner/$repo/pulls/$n/commits?limit=$limit"

  /** PURE trailer scan for pr-commits: the message lines that claim credit — `Co-Authored-By:`
    * trailers and "Generated with ..." lines, case-insensitively. Deliberately BROAD: the tool
    * SURFACES candidate lines and the maintainer judges, because deciding whether a co-author is an
    * assistant is not a string predicate (a human co-author trailer is legitimate; an assistant one
    * is what CONTRIBUTING.md forbids). Public for unit tests. */
  def creditTrailers(message: String): List[String] =
    message.linesIterator.map(_.trim).filter { l =>
      val lc = l.toLowerCase
      lc.startsWith("co-authored-by:") || lc.contains("generated with")
    }.toList

  // pr-commits — READ a PR's commit list (issue 029). CONTRIBUTING.md:63 forbids assistant-credit
  // trailers, and checking that rule before a merge means reading the branch's commit MESSAGES —
  // which pr-files and pr-diff cannot show, so the check used to leave the lane as a raw
  // `gh pr view --json commits`. One line per commit (tab-separated like the other list verbs),
  // credit-candidate lines indented under their commit, and a one-line verdict the caller can act on.
  private final case class CommitsOpts(repo: Option[String], item: Option[String], base: String, limit: Int)

  private def prCommits(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: CommitsOpts): CommitsOpts =
      rest match
        case Nil                 => o
        case "--url" :: u :: t   => go(t, o.copy(base = u))
        case "--gh" :: t         => go(t, o.copy(base = "https://github.com"))
        case "--limit" :: n :: t =>
          n.toIntOption match
            case Some(v) if v > 0 => go(t, o.copy(limit = v))
            case _                => die(s"--limit needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty => go(t, o.copy(repo = Some(r)))
        case i :: t if o.item.isEmpty => go(t, o.copy(item = Some(i)))
        case other :: _               => die(s"unexpected argument '$other'")
    val o             = go(args, CommitsOpts(None, None, DefaultBase, 100))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n   = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh  = isGitHub(o.base)
    val arr = Try(getJson(prCommitsUrl(gh, apiBase(o.base), owner, repo, n, o.limit), if gh then ghHeaders else Map.empty).arr)
      .getOrElse(die("expected a JSON array of commits"))
    // Both dialects nest the git data under `commit` (author name/email/date, full message);
    // the top-level `sha` is the commit id. Shown: short sha, ISO date, author, headline.
    val rows = arr.toList.map { c =>
      val sha   = strOr(c.obj.get("sha"), "?").take(10)
      val cmt   = c.obj.get("commit")
      val name  = cmt.flatMap(v => Try(v.obj("author").obj("name").str).toOption).getOrElse("?")
      val email = cmt.flatMap(v => Try(v.obj("author").obj("email").str).toOption).getOrElse("")
      val date  = cmt.flatMap(v => Try(v.obj("author").obj("date").str).toOption).getOrElse("")
      val msg   = cmt.flatMap(v => Try(v.obj("message").str).toOption).getOrElse("")
      (sha, date, s"$name <$email>", msg.takeWhile(_ != '\n').trim, creditTrailers(msg))
    }
    if rows.isEmpty then println("(no commits)")
    else
      rows.foreach { (sha, date, author, headline, trailers) =>
        println(s"$sha\t$date\t$author\t$headline")
        trailers.foreach(l => println(s"\t  ! $l"))
      }
      val hits = rows.map(_._5.size).sum
      if hits == 0 then println(s"=== ${rows.size} commits, 0 assistant-credit trailers")
      else println(s"=== ${rows.size} commits, $hits credit line(s) flagged (! above) — CONTRIBUTING.md forbids assistant credit")
      if rows.size >= o.limit then
        println(s"(--limit ${o.limit} page cap reached — more commits may exist; pagination not implemented)")

  /** PURE builders for pr-merge, public for unit tests (SM207). The subject default is
    * CONTRIBUTING.md:81's rule made automatic: the merge commit names the PR (number + title), so
    * mirrored history carries the cross-reference without the caller retyping the title. */
  def mergeSubject(n: Int, title: String): String = s"Merge PR #$n: $title"

  def prMergeUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, n: Int): String =
    if isGh then s"$GitHubApi/repos/$owner/$repo/pulls/$n/merge"
    else s"$apiRoot/repos/$owner/$repo/pulls/$n/merge"

  /** PURE payload builder for pr-merge. GitHub PUTs {merge_method, commit_title[, commit_message]};
    * Gitea/Forgejo POSTs {Do, MergeTitleField[, MergeMessageField]} — the capitalized keys are the
    * Gitea API's own spelling, not a style slip. An empty body sends NO message field, so the forge
    * composes its default instead of recording an empty message. */
  def mergePayload(isGh: Boolean, method: String, subject: String, body: String): ujson.Obj =
    if isGh then
      val p = ujson.Obj("merge_method" -> method, "commit_title" -> subject)
      if body.nonEmpty then p("commit_message") = body
      p
    else
      val p = ujson.Obj("Do" -> method, "MergeTitleField" -> subject)
      if body.nonEmpty then p("MergeMessageField") = body
      p

  // pr-merge — the FIRST effectful verb in the pr-* family (issue 030): merge a PR. Follows the
  // safety shape release-delete established — PREVIEW by default, apply only with --yes — because a
  // merge is outward-facing in the same way. The body comes from a FILE only (like release-create's
  // --body-file and `tt git commit --message-file`): prose with shell metacharacters must never ride
  // a command line, which is WHY this verb exists. Refuses a PR the forge reports unmergeable, and
  // NEVER deletes the source branch — that is a separate destructive act with no flag here.
  private final case class MergeOpts(repo: Option[String], item: Option[String], base: String,
      dialect: Dialect, method: String, subject: Option[String], bodyFile: Option[String], yes: Boolean)

  private def prMerge(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: MergeOpts): MergeOpts =
      rest match
        case Nil                     => o
        case "--method" :: m :: t    =>
          if Set("merge", "squash", "rebase").contains(m) then go(t, o.copy(method = m))
          else die(s"--method must be merge, squash or rebase, got '$m'")
        case "--subject" :: s :: t   => go(t, o.copy(subject = Some(s)))
        case "--body-file" :: f :: t => go(t, o.copy(bodyFile = Some(f)))
        case "--yes" :: t            => go(t, o.copy(yes = true))
        case "--url" :: u :: t       => go(t, o.copy(base = u))
        case "--gh" :: t             => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _             => die(
          "pr-merge is not implemented for GitLab (a merge request is a different API shape there).\n" +
            "  Stated rather than faked — use --gh or the default Gitea/Forgejo dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty => go(t, o.copy(repo = Some(r)))
        case i :: t if o.item.isEmpty => go(t, o.copy(item = Some(i)))
        case other :: _               => die(s"unexpected argument '$other'")
    val o = go(args, MergeOpts(None, None, DefaultBase, Dialect.Gitea, "merge", None, None, false))
    if o.dialect == Dialect.GitHub && o.base != DefaultBase then
      die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val n    = o.item.flatMap(_.toIntOption).getOrElse(die("expected a PR number after <owner>/<repo>"))
    val gh   = o.dialect == Dialect.GitHub
    val body = o.bodyFile match
      case Some(f) => Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'"))
      case None    => ""
    // ONE anonymous read serves both the preview and the pre-merge state check (like showPr's).
    val prUrl = if gh then s"$GitHubApi/repos/$owner/$repo/pulls/$n" else s"${apiBase(o.base)}/repos/$owner/$repo/pulls/$n"
    val pr    = getJson(prUrl, if gh then ghHeaders else Map.empty)
    val title      = strOr(pr.obj.get("title"), "?")
    val state      = strOr(pr.obj.get("state"), "?")
    val merged     = Try(pr.obj("merged").bool).getOrElse(false)
    val draft      = Try(pr.obj("draft").bool).getOrElse(false)
    val mergeable  = pr.obj.get("mergeable").flatMap(v => Try(v.bool).toOption) // None = absent OR still computing
    val mergeState = if gh then strOr(pr.obj.get("mergeable_state"), "?") else ""
    val headRef    = Try(pr.obj("head").obj("ref").str).getOrElse("?")
    val baseRef    = Try(pr.obj("base").obj("ref").str).getOrElse("?")
    val changed    = Try(pr.obj("changed_files").num.toLong.toString).getOrElse("?") // Gitea may not report it
    val subject    = o.subject.getOrElse(mergeSubject(n, title))
    // The refusals, named rather than passing the forge's error through raw — and shown in the
    // preview too, so a preview never promises an apply that the next step would refuse (the same
    // rule release-delete's preview follows for --allow-published).
    val blocker =
      if merged then Some("already merged")
      else if state != "open" then Some(s"in state '$state', not open")
      else if draft then Some("a DRAFT — mark it ready for review first")
      else if mergeable.contains(false) then
        Some(s"reported NOT mergeable${if gh then s" (mergeable_state=$mergeState)" else ""} — resolve conflicts first")
      else if gh && mergeable.isEmpty then Some("still being computed by GitHub (mergeability unknown) — re-run in a moment")
      else None
    if !o.yes then
      println(s"would MERGE PR #$n: $title")
      println(s"  author: ${userLogin(pr)}  $headRef -> $baseRef  files changed: $changed")
      println(s"  state: $state  merged=$merged  mergeable=${mergeable.map(_.toString).getOrElse("?")}${if gh then s"  merge_state=$mergeState" else ""}")
      println(s"  method: ${o.method}  subject: $subject")
      println(s"  body: ${o.bodyFile.map(f => s"from $f (${body.length} chars)").getOrElse("(none — the forge composes its default)")}")
      if o.method == "squash" then
        println("  note: squash discards the contributors' commit boundaries and per-commit attribution")
      println("  the source branch is NOT deleted — that is a separate act, deliberately without a flag here")
      println(blocker.map(b => s"  would REFUSE: the PR is $b").getOrElse("  re-run with --yes to apply"))
    else
      blocker.foreach(b => die(s"refusing to merge PR #$n: it is $b"))
      val url     = prMergeUrl(gh, apiBase(o.base), owner, repo, n)
      val hdrs    = rl.writeHeaders(o.dialect, o.base, "pr-merge") + ("Content-Type" -> "application/json")
      val payload = mergePayload(gh, o.method, subject, body)
      System.err.println(s"forge: [audit] ${if gh then "PUT" else "POST"} $url  pr=#$n method=${o.method} subject=$subject")
      val r =
        if gh then Try(requests.put(url, data = ujson.write(payload), headers = hdrs,
          check = false, readTimeout = 60000, connectTimeout = 10000)).getOrElse(die("request failed"))
        else Try(requests.post(url, data = ujson.write(payload), headers = hdrs,
          check = false, readTimeout = 60000, connectTimeout = 10000)).getOrElse(die("request failed"))
      r.statusCode match
        case 200 =>
          val sha = Try(ujson.read(r.text()).obj("sha").str).getOrElse("")
          println(s"merged PR #$n (${o.method})${if sha.nonEmpty then s"  merge sha $sha" else ""}")
          println(s"  subject: $subject")
        case 405 => die(s"the forge refused the merge (405) — the PR became unmergeable or a protection rule blocks it.\n${r.text().take(500)}")
        case 409 => die(s"the head branch changed since the check (409) — re-read the PR and re-run.\n${r.text().take(500)}")
        case c   => die(s"${if gh then "PUT" else "POST"} $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  private def showProtection(args: List[String]): Unit =
    val o             = parseItem(args)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val branch        = o.item.getOrElse(die("expected a branch name after <owner>/<repo>"))
    if isGitHub(o.base) then
      if ghToken.isEmpty then die(
        s"protection on GitHub needs an admin-read token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag).")
      val p = getJson(s"$GitHubApi/repos/$owner/$repo/branches/$branch/protection", ghHeaders)
      def enabled(key: String): String =
        Try(p.obj(key).obj("enabled").bool).toOption.map(b => if b then "yes" else "no").getOrElse("?")
      p.obj.get("required_status_checks") match
        case Some(c) =>
          val strict = Try(c.obj("strict").bool).getOrElse(false)
          val ctxs   = Try(c.obj("contexts").arr.map(_.str).toList).getOrElse(Nil)
          println(s"required status checks: ${if ctxs.isEmpty then "(none selected)" else ctxs.mkString(", ")}  strict=$strict")
        case None => println("required status checks: NONE")
      println(s"enforce admins: ${enabled("enforce_admins")}")
      println(s"required PR reviews: ${if p.obj.contains("required_pull_request_reviews") then "yes" else "no"}")
      p.obj.get("restrictions") match
        case Some(r) =>
          val users = Try(r.obj("users").arr.map(_.obj("login").str).toList).getOrElse(Nil)
          val teams = Try(r.obj("teams").arr.map(_.obj("slug").str).toList).getOrElse(Nil)
          println(s"push restricted to: users=[${users.mkString(", ")}] teams=[${teams.mkString(", ")}]")
        case None => println("push restrictions: none")
      println(s"force pushes allowed: ${enabled("allow_force_pushes")}")
      println(s"deletions allowed: ${enabled("allow_deletions")}")
    else
      val tok = token.getOrElse(die(
        s"protection needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")} (never a flag)."))
      val url  = s"${apiBase(o.base)}/repos/$owner/$repo/branch_protections"
      val host = hostOf(url)
      if !trustedHosts.contains(host) then die(
        s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.")
      val r = Try(requests.get(url, headers = Map("Authorization" -> s"token $tok"),
        check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
      if r.statusCode != 200 then die(s"GET $url -> ${r.statusCode} ${r.statusMessage}")
      val arr  = Try(ujson.read(r.text()).arr).getOrElse(die("expected a JSON array of branch protections"))
      val hits = arr.filter(p =>
        Try(p.obj("branch_name").str).toOption.orElse(Try(p.obj("rule_name").str).toOption).contains(branch))
      if hits.isEmpty then println(s"no branch protection rule matches '$branch' (${arr.size} rules total)")
      else hits.foreach { p =>
        val checksOn = Try(p.obj("enable_status_check").bool).getOrElse(false)
        val ctxs     = Try(p.obj("status_check_contexts").arr.map(_.str).toList).getOrElse(Nil)
        println(s"rule: $branch  status-checks=${if checksOn then ctxs.mkString(", ") else "off"}")
        println(s"push whitelist: ${Try(p.obj("push_whitelist_usernames").arr.map(_.str).toList).getOrElse(Nil).mkString(", ")}")
        println(s"force pushes allowed: ${Try(p.obj("enable_force_push").bool).getOrElse(false)}")
      }

  // Which forge dialect release-create/edit speaks. Gitea (default, --url) posts the Gitea payload with an
  // `Authorization: token` header to a trustedHosts host; GitHub (--gh) posts to the FIXED api.github.com root;
  // GitLab (--gl) posts the /api/v4 payload with a `PRIVATE-TOKEN` header to a gitlabTrustedHosts host.
  //
  // Defined in ReleaseLib because it appears in the shared download signatures; aliased here so every
  // `Dialect.Gitea` in this file still reads the same. ONE enum, two tools.
  private type Dialect = ReleaseLib.Dialect
  private val Dialect  = ReleaseLib.Dialect

  private final case class CreateOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], prerelease: Boolean, draft: Boolean,
      target: Option[String], base: String, dialect: Dialect)

  private def releaseCreate(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: CreateOpts): CreateOpts =
      rest match
        case Nil                       => o
        case "--name" :: s :: t        => go(t, o.copy(name = Some(s)))
        case "--body" :: s :: t        => go(t, o.copy(body = Some(s)))
        case "--body-file" :: f :: t   => go(t, o.copy(bodyFile = Some(f)))
        case "--prerelease" :: t       => go(t, o.copy(prerelease = true))
        case "--draft" :: t            => go(t, o.copy(draft = true))
        case "--target" :: c :: t      => go(t, o.copy(target = Some(c)))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: t               => go(t, o.copy(dialect = Dialect.GitLab))
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, CreateOpts(None, None, None, None, None, false, false, None, DefaultBase, Dialect.Gitea))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val bodyText = o.bodyFile match
      case Some(f) => Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'"))
      case None    => o.body.getOrElse("")
    o.dialect match
      case Dialect.Gitea  => createGitea(owner, repo, tag, bodyText, o)
      case Dialect.GitHub => createGitHub(owner, repo, tag, bodyText, o)
      case Dialect.GitLab => createGitLab(owner, repo, tag, bodyText, o)

  // Gitea/Forgejo dialect (default): POST <base>/api/v1/repos/<o>/<r>/releases, `Authorization: token`, GitHub-shaped payload.
  private def createGitea(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    val tok = token.getOrElse(die(
      s"release-create needs a token — the HUMAN sets one of env ${TokenEnvNames.mkString(", ")}; it is deliberately\n" +
        "  NOT a flag, so the agent cannot self-authorize. Create one at Codeberg → Settings → Applications (write:repository)."))
    val url  = s"${apiBase(o.base)}/repos/$owner/$repo/releases"
    val host = hostOf(url)
    if !trustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${trustedHosts.toVector.sorted.mkString(", ")}.\n" +
        "  The HUMAN may extend the set via env TT_FORGE_HOSTS (comma-separated) — not a flag.")
    val payload = ujson.Obj(
      "tag_name"   -> tag,
      "name"       -> o.name.getOrElse(tag),
      "body"       -> bodyText,
      "prerelease" -> o.prerelease,
      "draft"      -> o.draft
    )
    o.target.foreach(c => payload("target_commitish") = c)
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)} prerelease=${o.prerelease} draft=${o.draft}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "Authorization" -> s"token $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"created release $tag  $html")
      case 409 => die(s"a release for tag '$tag' already exists (409)")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // GitHub dialect (--gh): POST to the FIXED api.github.com root (never derived from --url, so the token cannot be
  // redirected — same no-exfiltration rule as the read verbs). Payload is already GitHub-shaped (Gitea copied it).
  private def createGitHub(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    if o.base != DefaultBase then die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
    val tok = ghToken.getOrElse(die(
      s"release-create --gh needs a token — the HUMAN sets one of env ${GhTokenEnvNames.mkString(", ")} (never a flag).\n" +
        "  Create a fine-grained token with Contents: read-and-write (or a classic token with the `repo` scope)."))
    val url     = s"$GitHubApi/repos/$owner/$repo/releases"
    val payload = ujson.Obj(
      "tag_name"   -> tag,
      "name"       -> o.name.getOrElse(tag),
      "body"       -> bodyText,
      "prerelease" -> o.prerelease,
      "draft"      -> o.draft
    )
    o.target.foreach(c => payload("target_commitish") = c)
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)} prerelease=${o.prerelease} draft=${o.draft}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "Accept" -> "application/vnd.github+json", "Authorization" -> s"Bearer $tok"),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"created release $tag  $html")
      case 422 => die(s"GitHub rejected the release (422) — a release for tag '$tag' may already exist, or the ref is invalid.\n${r.text().take(500)}")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // GitLab dialect (--gl): POST <base>/api/v4/projects/<owner%2Frepo>/releases, `PRIVATE-TOKEN` header. Payload keys
  // differ (description, not body); GitLab has no prerelease/draft concept. Base is configurable (self-managed
  // instances) so the token only travels to a gitlabTrustedHosts host.
  private def createGitLab(owner: String, repo: String, tag: String, bodyText: String, o: CreateOpts): Unit =
    if o.prerelease || o.draft then die(
      "GitLab releases have no prerelease/draft flag — drop --prerelease/--draft when using --gl.")
    val base = if o.base == DefaultBase then "https://gitlab.com" else o.base // default gitlab.com, not codeberg
    val tok  = glToken.getOrElse(die(
      s"release-create --gl needs a token — the HUMAN sets one of env ${GlTokenEnvNames.mkString(", ")} (never a flag).\n" +
        "  Create a personal/project access token with the `api` scope."))
    val host = hostOf(base)
    if !gitlabTrustedHosts.contains(host) then die(
      s"refusing to send the token to untrusted host '$host'. Trusted: ${gitlabTrustedHosts.toVector.sorted.mkString(", ")}.\n" +
        "  The HUMAN may extend the set via env TT_FORGE_GITLAB_HOSTS or TT_FORGE_HOSTS (comma-separated) — not a flag.")
    val proj    = s"$owner%2F$repo" // GitLab wants the project path URL-encoded ('/' -> %2F)
    val url     = s"${base.stripSuffix("/")}/api/v4/projects/$proj/releases"
    val payload = ujson.Obj("tag_name" -> tag, "name" -> o.name.getOrElse(tag), "description" -> bodyText)
    o.target.foreach(c => payload("ref") = c) // GitLab creates the tag from `ref` when it doesn't yet exist
    System.err.println(s"forge: [audit] POST $url  tag=$tag name=${o.name.getOrElse(tag)}")
    val r = Try(requests.post(url, data = ujson.write(payload),
      headers = Map("Content-Type" -> "application/json", "PRIVATE-TOKEN" -> tok),
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 201 =>
        val link = Try(ujson.read(r.text()).obj("_links").obj("self").str).getOrElse("")
        println(s"created release $tag  $link")
      case 409 => die(s"a release for tag '$tag' already exists (409)")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  private final case class EditOpts(repo: Option[String], tag: Option[String], name: Option[String],
      body: Option[String], bodyFile: Option[String], setPrerelease: Boolean, setDraft: Boolean, base: String,
      dialect: Dialect, newTag: Option[String])

  /** PURE payload builder for release-edit: ONLY the provided fields, so unspecified fields are left
    * unchanged by the forge. Public so the request shape is unit-testable without a network (SM207).
    * `tagName` exists because a DRAFT's tag is just a text field the GitHub UI can silently reset to an
    * `untagged-...` placeholder on a save with the selector unpicked (it happened to v0.10.0 on
    * 2026-07-28, minutes after a body-only PATCH had matched the tag) — restoring it must be possible
    * without the UI. */
  def editPayload(name: Option[String], body: Option[String],
      setPrerelease: Boolean, setDraft: Boolean, tagName: Option[String] = None): ujson.Obj =
    val payload = ujson.Obj()
    body.foreach(b => payload("body") = b)
    name.foreach(n => payload("name") = n)
    tagName.foreach(t => payload("tag_name") = t)
    if setPrerelease then payload("prerelease") = true
    if setDraft then payload("draft") = true
    payload

  // release-edit — PATCH an EXISTING release: look it up by tag (unauth GET on Gitea; token-visible on
  // GitHub so DRAFTS are found), then send ONLY the provided fields (unspecified fields are left
  // unchanged by the forge). Same effectful/token/trusted-host rules as release-create; the write-auth
  // headers come from ReleaseLib.writeHeaders so the rule has ONE definition. `--gh` added 2026-07-28
  // (SM207's edit half): the immediate need was editing the v0.10.0 DRAFT notes, which had no typed shape.
  private def releaseEdit(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: EditOpts): EditOpts =
      rest match
        case Nil                       => o
        case "--name" :: s :: t        => go(t, o.copy(name = Some(s)))
        case "--body" :: s :: t        => go(t, o.copy(body = Some(s)))
        case "--body-file" :: f :: t   => go(t, o.copy(bodyFile = Some(f)))
        case "--tag" :: s :: t         => go(t, o.copy(newTag = Some(s)))
        case "--prerelease" :: t       => go(t, o.copy(setPrerelease = true))
        case "--draft" :: t            => go(t, o.copy(setDraft = true))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _               => die(
          "release-edit is not implemented for GitLab (its release update API differs; use the web UI\n" +
            "  or extend this verb). Stated rather than faked — use --gh or the default Gitea dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, EditOpts(None, None, None, None, None, false, false, DefaultBase, Dialect.Gitea, None))
    if o.dialect == Dialect.GitHub && o.base != DefaultBase then
      die("--gh targets the fixed GitHub API root; drop --url (it is not used with --gh).")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val bodyText = o.bodyFile match
      case Some(f) => Some(Try(os.read(os.Path(f, os.pwd))).getOrElse(die(s"cannot read --body-file '$f'")))
      case None    => o.body
    val payload = editPayload(o.name, bodyText, o.setPrerelease, o.setDraft, o.newTag)
    if payload.obj.isEmpty then die("nothing to edit — provide --body/--body-file, --name, --tag, --prerelease, or --draft")
    // Look up the release id via findRelease, which LISTS first and so can see a DRAFT. The previous
    // by-tag-only lookup made this verb structurally unable to edit a draft — the state you are most
    // likely to be editing, since a draft is by definition unfinished.
    val relJson = findRelease(owner, repo, tag, o.dialect, o.base)
    val id      = Try(relJson.obj("id").num.toLong).getOrElse(die(s"no release id found for tag '$tag'"))
    val url     = if o.dialect == Dialect.GitHub then s"$GitHubApi/repos/$owner/$repo/releases/$id"
                  else s"${apiBase(o.base)}/repos/$owner/$repo/releases/$id"
    val hdrs    = rl.writeHeaders(o.dialect, o.base, "release-edit") + ("Content-Type" -> "application/json")
    System.err.println(s"forge: [audit] PATCH $url  tag=$tag fields=${payload.obj.keys.mkString(",")}")
    val r = Try(requests.patch(url, data = ujson.write(payload),
      headers = hdrs,
      check = false, readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 200 =>
        val html = Try(ujson.read(r.text()).obj.get("html_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"edited release $tag  $html")
      case 404 => die(s"release for tag '$tag' not found (404)")
      case c   => die(s"PATCH $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // ---- release ASSETS: download (+ integrity) and delete -------------------------------------
  //
  // WHY these exist: the release-asset lifecycle was the one part of shipping that had no typed
  // verb, so every "does that release actually carry a binary?" and every cleanup went out as a
  // raw `gh` call the guard cannot inspect. Built 2026-07-27 during the first release rehearsal,
  // where the missing shapes were hit twice within minutes of each other.

  private final case class AssetOpts(repo: Option[String], tag: Option[String], base: String,
      dialect: Dialect, pattern: Option[String], dir: String, verify: Boolean, yes: Boolean,
      allowPublished: Boolean)

  private def parseAsset(args: List[String], verb: String): AssetOpts =
    @annotation.tailrec
    def go(rest: List[String], o: AssetOpts): AssetOpts =
      rest match
        case Nil                        => o
        case "--pattern" :: p :: t      => go(t, o.copy(pattern = Some(p)))
        case "--dir" :: d :: t          => go(t, o.copy(dir = d))
        case "--verify" :: t            => go(t, o.copy(verify = true))
        case "--yes" :: t               => go(t, o.copy(yes = true))
        case "--allow-published" :: t   => go(t, o.copy(allowPublished = true))
        case "--url" :: u :: t          => go(t, o.copy(base = u))
        case "--gh" :: t                => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _                => die(
          s"$verb is not implemented for GitLab: GitLab releases carry LINKS to external artifacts\n" +
            "  rather than uploaded assets, so the same flags would mean something different there.\n" +
            "  Stated rather than faked — use --gh or the default Gitea/Forgejo dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty   => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty   => go(t, o.copy(tag = Some(tg)))
        case other :: _                 => die(s"unexpected argument '$other'")
    go(args, AssetOpts(None, None, DefaultBase, Dialect.Gitea, None, ".", false, false, false))

  // ------------------------------------------------------------------------------------------------
  // Release lookup + download: ONE definition, in ReleaseLib, shared with `tt update --native`.
  // Forwarders below keep this file's call sites unchanged. See releaselib.scala for the WHY of each —
  // the draft-vs-by-tag complementarity, the die-cannot-be-caught reason getJsonOpt exists, and the
  // trusted-host guard that is now written once instead of three times.
  // ------------------------------------------------------------------------------------------------
  private def getJsonOpt(url: String, headers: Map[String, String] = Map.empty) = rl.getJsonOpt(url, headers)
  private def findHeaders(dialect: Dialect, base: String)                        = rl.findHeaders(dialect, base)
  private def assetsOf(rel: ujson.Value)                                         = ReleaseLib.assetsOf(rel)
  private def globMatches(glob: String, name: String)                            = Lib.globMatches(glob, name)
  private def sha256Hex(p: os.Path)                                              = ReleaseLib.sha256Hex(p)
  private def writeHeaders(o: AssetOpts, verb: String)                           = rl.writeHeaders(o.dialect, o.base, verb)
  private def verifyChecksums(files: List[os.Path]): Unit                        = rl.verifyChecksums(files)

  private def findRelease(owner: String, repo: String, tag: String, dialect: Dialect, base: String): ujson.Value =
    rl.findRelease(owner, repo, tag, dialect, base)

  private def releaseDownload(args: List[String]): Unit =
    val o             = parseAsset(args, "release-download")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val written       = rl.downloadAssets(rel, o.pattern, os.Path(o.dir, os.pwd), o.dialect, o.base, "release-download")
    if o.verify then verifyChecksums(written)
    else println("(no --verify: bytes downloaded but NOT checked against a .sha256)")

  /** PURE upload-URL builder, public for unit tests (SM207's testable-request-building rule).
    * GitHub uploads go to a SECOND fixed root (uploads.github.com) — fixed for the same reason as
    * GitHubApi: the token must never travel to a host derived from input (not even from the release
    * JSON's own upload_url, which is attacker-influenceable in principle). The asset name is
    * URL-encoded with %20 for spaces (URLEncoder's '+' is form-encoding, not query-encoding). */
  def uploadAssetUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String, id: Long, name: String): String =
    val enc = java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")
    if isGh then s"https://uploads.github.com/repos/$owner/$repo/releases/$id/assets?name=$enc"
    else s"$apiRoot/repos/$owner/$repo/releases/$id/assets?name=$enc"

  // release-upload — attach ONE file to an existing release (drafts included, via findRelease).
  // Built 2026-07-28: the v0.10.0 draft needed get-genscalator.sc attached and the only shapes were a
  // raw `gh release upload` or the web UI. GitHub sends the bytes as application/octet-stream to the
  // fixed uploads root; Gitea/Forgejo takes multipart form field `attachment` on the API root.
  private final case class UploadOpts(repo: Option[String], tag: Option[String], file: Option[String],
      name: Option[String], base: String, dialect: Dialect, clobber: Boolean)

  private def releaseUpload(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: UploadOpts): UploadOpts =
      rest match
        case Nil                       => o
        case "--clobber" :: t          => go(t, o.copy(clobber = true))
        case "--name" :: n :: t        => go(t, o.copy(name = Some(n)))
        case "--url" :: u :: t         => go(t, o.copy(base = u))
        case "--gh" :: t               => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: _               => die(
          "release-upload is not implemented for GitLab: GitLab releases carry LINKS to external\n" +
            "  artifacts rather than uploaded assets. Stated rather than faked — use --gh or the default dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty  => go(t, o.copy(repo = Some(r)))
        case tg :: t if o.tag.isEmpty  => go(t, o.copy(tag = Some(tg)))
        case f :: t if o.file.isEmpty  => go(t, o.copy(file = Some(f)))
        case other :: _                => die(s"unexpected argument '$other'")
    val o             = go(args, UploadOpts(None, None, None, None, DefaultBase, Dialect.Gitea, false))
    if o.dialect == Dialect.GitHub && o.base != DefaultBase then
      die("--gh targets the fixed GitHub upload root; drop --url (it is not used with --gh).")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val path          = os.Path(o.file.getOrElse(forgeUsage()), os.pwd)
    if !os.isFile(path) then die(s"no such file: $path")
    val bytes         = os.read.bytes(path)
    val assetName     = o.name.getOrElse(path.last)
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val id            = Try(rel.obj("id").num.toLong).getOrElse(die(s"no numeric release id for tag '$tag'"))
    val isDraft       = rel.obj.get("draft").exists(_.bool)
    val clash         = assetsOf(rel).find(a => strOr(a.obj.get("name"), "?") == assetName)
    // ISSUE 006. Refusing a duplicate stays the DEFAULT — silently overwriting a published asset is
    // exactly the surprise a typed tool must not hand out. What changed is that the refusal now names
    // verbs that EXIST: the old message said "delete it first" while nothing in the toolbox could delete
    // an asset, which trains the caller to leave the toolbox at the one moment it should hold the rails.
    clash.foreach: a =>
      if !o.clobber then die(
        s"an asset named '$assetName' already exists on release '$tag' — forges refuse duplicate names.\n" +
          s"  Replace its bytes:  tt forge release-upload <repo> $tag <file> --clobber\n" +
          s"  Remove it first:    tt forge asset-rm <repo> $tag '$assetName'   (previews; --yes to apply)\n" +
          s"  Or keep both:       --name <other-name>")
      val oldId   = Try(a.obj("id").num.toLong).getOrElse(die(s"no numeric asset id for '$assetName'"))
      val oldSize = Try(a.obj("size").num.toLong).getOrElse(-1L)
      // Loud by design: --clobber on a PUBLISHED release changes bytes someone may already have
      // downloaded and checksummed. One conscious flag, but never a quiet one.
      System.err.println(
        s"forge: [audit] --clobber REPLACING asset '$assetName' (id $oldId, $oldSize B) on " +
          s"${if isDraft then "draft" else "PUBLISHED"} release '$tag' — old bytes become unreachable at the same URL")
      deleteAsset(o.dialect, o.base, owner, repo, id, oldId, assetName, "release-upload --clobber")
    val url  = uploadAssetUrl(o.dialect == Dialect.GitHub, apiBase(o.base), owner, repo, id, assetName)
    val hdrs = rl.writeHeaders(o.dialect, o.base, "release-upload")
    System.err.println(s"forge: [audit] POST $url  tag=$tag file=${path.last} bytes=${bytes.length}")
    val r =
      if o.dialect == Dialect.GitHub then
        Try(requests.post(url, data = bytes, headers = hdrs + ("Content-Type" -> "application/octet-stream"),
          check = false, readTimeout = 120000, connectTimeout = 10000)).getOrElse(die("request failed"))
      else
        Try(requests.post(url,
          data = requests.MultiPart(requests.MultiItem("attachment", bytes, assetName)),
          headers = hdrs, check = false, readTimeout = 120000, connectTimeout = 10000))
          .getOrElse(die("request failed"))
    r.statusCode match
      case 201 | 200 =>
        val dl = Try(ujson.read(r.text()).obj.get("browser_download_url").map(_.str).getOrElse("")).getOrElse("")
        println(s"uploaded $assetName (${bytes.length} B) to release $tag  $dl")
      case 422 => die(s"the forge rejected the upload (422) — most often a duplicate asset name.\n${r.text().take(500)}")
      case c   => die(s"POST $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  /** PURE asset-delete URL builder, public for unit tests (SM207's testable-request-building rule).
    * The dialects disagree about what an asset IS: GitHub addresses it by its own id directly under the
    * repo (`/releases/assets/<id>`), Gitea/Forgejo scopes the attachment under its release
    * (`/releases/<rid>/assets/<id>`). Getting that wrong is a 404 at best and someone else's asset at
    * worst, so it is one function with both shapes side by side rather than an inline string per site.
    * The GitHub root is the FIXED constant for the same reason `uploadAssetUrl` fixes the upload root. */
  def assetDeleteUrl(isGh: Boolean, apiRoot: String, owner: String, repo: String,
      releaseId: Long, assetId: Long): String =
    if isGh then s"$GitHubApi/repos/$owner/$repo/releases/assets/$assetId"
    else s"$apiRoot/repos/$owner/$repo/releases/$releaseId/assets/$assetId"

  /** ONE definition of the asset DELETE request, shared by `asset-rm` and `release-upload --clobber`.
    * Both call sites destroy published bytes, so they must not drift apart in audit line or error map. */
  private def deleteAsset(dialect: Dialect, base: String, owner: String, repo: String,
      releaseId: Long, assetId: Long, assetName: String, verb: String): Unit =
    val url  = assetDeleteUrl(dialect == Dialect.GitHub, apiBase(base), owner, repo, releaseId, assetId)
    val hdrs = rl.writeHeaders(dialect, base, verb)
    System.err.println(s"forge: [audit] DELETE $url  asset='$assetName' id=$assetId release=$releaseId")
    val r = Try(requests.delete(url, headers = hdrs, check = false,
      readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
    r.statusCode match
      case 204 | 200 => println(s"deleted asset '$assetName' (id $assetId)")
      case 404       => die(s"asset id $assetId not found (404) — it may already be gone")
      case c         => die(s"DELETE $url -> $c ${r.statusMessage}\n${r.text().take(500)}")

  // asset-rm — remove ONE asset from a release (issue 006). The gap this fills was not convenience:
  // `release-upload` refused duplicate names and told the caller to "delete it first", while
  // `release-delete` removes the WHOLE release and nothing removed a single asset. An error message
  // prescribing an impossible action is worse than no message. Follows release-delete's contract
  // exactly — preview by default, --yes to apply, --yes --allow-published for a published release —
  // because removing an asset from a published release breaks a URL that may sit in an install script.
  private def assetRm(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: AssetOpts, asset: Option[String]): (AssetOpts, Option[String]) =
      rest match
        case Nil                                => (o, asset)
        case "--yes" :: t                       => go(t, o.copy(yes = true), asset)
        case "--allow-published" :: t           => go(t, o.copy(allowPublished = true), asset)
        case "--url" :: u :: t                  => go(t, o.copy(base = u), asset)
        case "--gh" :: t                        => go(t, o.copy(dialect = Dialect.GitHub), asset)
        case "--gl" :: _                        => die(
          "asset-rm is not implemented for GitLab: GitLab releases carry LINKS to external artifacts\n" +
            "  rather than uploaded assets. Stated rather than faked — use --gh or the default dialect.")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty           => go(t, o.copy(repo = Some(r)), asset)
        case tg :: t if o.tag.isEmpty           => go(t, o.copy(tag = Some(tg)), asset)
        case a :: t if asset.isEmpty            => go(t, o, Some(a))
        case other :: _                         => die(s"unexpected argument '$other'")
    val (o, assetOpt) = go(args, AssetOpts(None, None, DefaultBase, Dialect.Gitea, None, ".", false, false, false), None)
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val assetName     = assetOpt.getOrElse(forgeUsage())
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val relId         = Try(rel.obj("id").num.toLong).getOrElse(die(s"no numeric release id for tag '$tag'"))
    val isDraft       = rel.obj.get("draft").exists(_.bool)
    val all           = assetsOf(rel)
    val hit = all.find(a => strOr(a.obj.get("name"), "?") == assetName).getOrElse(die(
      s"release '$tag' has no asset named '$assetName'.\n" +
        "  present: " + (if all.isEmpty then "(none)" else all.map(a => strOr(a.obj.get("name"), "?")).mkString(", "))))
    val assetId = Try(hit.obj("id").num.toLong).getOrElse(die(s"no numeric asset id for '$assetName'"))
    val size    = Try(hit.obj("size").num.toLong).getOrElse(-1L)
    val what    = s"'$assetName' (id $assetId, $size B) from release $tag (${if isDraft then "draft" else "PUBLISHED"})"
    if !o.yes then
      println(s"would DELETE asset $what")
      println("  the release itself and its other assets are NOT touched")
      if !isDraft then
        println("  ⚠ this release is PUBLISHED — the asset's download URL may already be in an install script")
      println(
        if isDraft then "  re-run with --yes to apply"
        else "  re-run with --yes --allow-published to apply (a PUBLISHED release needs BOTH flags)")
    else if !isDraft && !o.allowPublished then
      die(s"refusing to remove an asset from a PUBLISHED release ('$tag'): its download URL may already\n" +
        "  be in someone's install script, and removal makes that URL 404. Pass --allow-published if that\n" +
        "  is truly intended. To REPLACE the bytes under the same name instead, use\n" +
        "  `tt forge release-upload <repo> <tag> <file> --clobber`.")
    else
      deleteAsset(o.dialect, o.base, owner, repo, relId, assetId, assetName, "asset-rm")

  // ------------------------------------------------------------------------------------------------
  // file — read ONE file's contents out of a repo, the remote sibling of `tt git show` (issue 007).
  // The gap: `release-download` fetches ASSETS, `tt web get` NEVER sends credentials by design, so a
  // single file in a PRIVATE repo forced either a raw curl carrying a token on the command line or a
  // whole shallow clone to read one file. This is a READ that CARRIES A CREDENTIAL, which is exactly
  // why the typed shape is the safety argument: same fixed-env + helper token machinery, same fixed
  // GitHub root, same trusted-host guard, and a size cap so a mis-typed path cannot dump a blob.
  // ------------------------------------------------------------------------------------------------

  /** Percent-encode a repo path for a URL PATH position, preserving the slashes as separators. */
  def encodePathSegments(p: String): String =
    p.split("/").filter(_.nonEmpty)
      .map(s => java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20")).mkString("/")

  /** Percent-encode a repo path as a SINGLE URL component (slashes become %2F) — GitLab's files API
    * takes the whole path that way, the same encoding its projects endpoint wants for owner/repo. */
  def encodePathWhole(p: String): String =
    java.net.URLEncoder.encode(p.stripPrefix("/"), "UTF-8").replace("+", "%20")

  /** PURE repo-file URL builder, public for unit tests. `root` is the dialect's already-resolved root
    * (apiBase(base) for Gitea, the instance base for GitLab); GitHub ignores it and uses the FIXED
    * constant, so no input can point the GitHub token at another host. */
  def repoFileUrl(isGh: Boolean, isGl: Boolean, root: String, owner: String, repo: String,
      path: String, ref: Option[String]): String =
    val q = ref.map(r => s"?ref=${java.net.URLEncoder.encode(r, "UTF-8")}").getOrElse("")
    if isGh then s"$GitHubApi/repos/$owner/$repo/contents/${encodePathSegments(path)}$q"
    else if isGl then
      s"${root.stripSuffix("/")}/api/v4/projects/$owner%2F$repo/repository/files/${encodePathWhole(path)}/raw$q"
    else s"$root/repos/$owner/$repo/raw/${encodePathSegments(path)}$q"

  private final case class FileOpts(repo: Option[String], path: Option[String], ref: Option[String],
      out: Option[String], base: String, dialect: Dialect, maxBytes: Long)

  private val DefaultFileMaxBytes = 5_000_000L

  private def repoFileRead(args: List[String]): Unit =
    @annotation.tailrec
    def go(rest: List[String], o: FileOpts): FileOpts =
      rest match
        case Nil                                => o
        case "--ref" :: r :: t                  => go(t, o.copy(ref = Some(r)))
        case "--out" :: f :: t                  => go(t, o.copy(out = Some(f)))
        case "--url" :: u :: t                  => go(t, o.copy(base = u))
        case "--gh" :: t                        => go(t, o.copy(dialect = Dialect.GitHub))
        case "--gl" :: t                        => go(t, o.copy(dialect = Dialect.GitLab))
        case "--max-bytes" :: n :: t            =>
          n.toLongOption match
            case Some(v) if v > 0 => go(t, o.copy(maxBytes = v))
            case _                => die(s"--max-bytes needs a positive integer, got '$n'")
        case flag :: _ if flag.startsWith("--") => die(s"unknown/incomplete flag '$flag'")
        case r :: t if o.repo.isEmpty           => go(t, o.copy(repo = Some(r)))
        case p :: t if o.path.isEmpty           => go(t, o.copy(path = Some(p)))
        case other :: _                         => die(s"unexpected argument '$other'")
    val o             = go(args, FileOpts(None, None, None, None, DefaultBase, Dialect.Gitea, DefaultFileMaxBytes))
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val path          = o.path.getOrElse(forgeUsage())
    val isGh          = o.dialect == Dialect.GitHub
    val isGl          = o.dialect == Dialect.GitLab
    // GitLab's default instance is gitlab.com, not the Gitea default — the same correction listContributors makes
    val root = if isGl && o.base == DefaultBase then "https://gitlab.com" else if isGl then o.base else apiBase(o.base)
    val url  = repoFileUrl(isGh, isGl, root, owner, repo, path, o.ref)
    // Same trusted-host rule as every other credential-carrying verb: the token never travels to a host
    // this tool has not been told to trust. GitHub/GitLab roots are guarded by their own dialect rules.
    val headers: Map[String, String] =
      if isGh then ghHeaders + ("Accept" -> "application/vnd.github.raw")
      else if isGl then
        val host = hostOf(url)
        if !gitlabTrustedHosts.contains(host) then die(
          s"refusing to send the GitLab token to untrusted host '$host'. Trusted: " +
            s"${gitlabTrustedHosts.toVector.sorted.mkString(", ")} (extend via env TT_FORGE_GITLAB_HOSTS).")
        glToken.map(t => Map("PRIVATE-TOKEN" -> t)).getOrElse(Map.empty)
      else
        val host = hostOf(url)
        token match
          case Some(t) =>
            if !trustedHosts.contains(host) then die(
              s"refusing to send the token to untrusted host '$host'. Trusted: " +
                s"${trustedHosts.toVector.sorted.mkString(", ")} (extend via env TT_FORGE_HOSTS).")
            Map("Authorization" -> s"token $t")
          case None => Map.empty // public repo: an anonymous read is fine, and no secret can leak
    val r = Try(requests.get(url, headers = headers, check = false,
      readTimeout = 60000, connectTimeout = 10000)).getOrElse(die(s"request failed: $url"))
    r.statusCode match
      case 200 =>
        val bytes = r.bytes
        if bytes.length > o.maxBytes then die(
          s"file is ${bytes.length} B, over the ${o.maxBytes} B cap — raise it with --max-bytes N, or\n" +
            "  write it out with --out FILE instead of printing it")
        o.out match
          case Some(f) =>
            val dest = os.Path(f, os.pwd)
            os.write.over(dest, bytes, createFolders = true)
            println(s"wrote ${bytes.length} B to $dest  ($path${o.ref.map(" @ " + _).getOrElse("")})")
          case None => print(String(bytes, "UTF-8"))
      case 404 => die(s"no such file (404): '$path' in $owner/$repo${o.ref.map(" @ " + _).getOrElse("")}\n" +
        "  a PRIVATE repo also answers 404 when the token cannot see it — check `tt forge whoami`")
      case 401 | 403 => die(s"GET $url -> ${r.statusCode} ${r.statusMessage} — the token is missing, expired, or lacks scope")
      case c   => die(s"GET $url -> $c ${r.statusMessage}")

  private def releaseDelete(args: List[String]): Unit =
    val o             = parseAsset(args, "release-delete")
    val (owner, repo) = splitRepo(o.repo.getOrElse(forgeUsage()))
    val tag           = o.tag.getOrElse(forgeUsage())
    val rel           = findRelease(owner, repo, tag, o.dialect, o.base)
    val id            = Try(rel.obj("id").num.toLong).getOrElse(die(s"no numeric release id for tag '$tag'"))
    val isDraft       = rel.obj.get("draft").exists(_.bool)
    val nAssets       = assetsOf(rel).size
    val what          = s"$tag (id $id, ${if isDraft then "draft" else "PUBLISHED"}, $nAssets asset(s))"
    // PREVIEW BY DEFAULT — the same contract as `tt sub`, and for the same reason: the cheapest way
    // to destroy the wrong release is a mistyped tag, and a preview costs one extra keystroke.
    if !o.yes then
      println(s"would DELETE release $what")
      println("  assets: " + (if nAssets == 0 then "(none)" else assetsOf(rel).map(a => strOr(a.obj.get("name"), "?")).mkString(", ")))
      println("  the git TAG is NOT touched — deleting a release and deleting its tag are different acts")
      // Tell the caller what will ACTUALLY work. Saying "--yes" for a published release would promise
      // something the next step refuses — a preview that misdescribes its own follow-up is the same
      // class of defect as a stale comment (SM243), and it is worse here because the verb is destructive.
      println(
        if isDraft then "  re-run with --yes to apply"
        else "  re-run with --yes --allow-published to apply (a PUBLISHED release needs BOTH flags)")
    else if !isDraft && !o.allowPublished then
      die(s"refusing to delete a PUBLISHED release ('$tag'): its notes are public and its asset URLs\n" +
        "  may already be in someone's install script. Pass --allow-published if that is truly intended.\n" +
        "  A draft needs no such flag, which is the case this verb was built for.")
    else
      val url  = if o.dialect == Dialect.GitHub then s"$GitHubApi/repos/$owner/$repo/releases/$id"
                 else s"${apiBase(o.base)}/repos/$owner/$repo/releases/$id"
      val hdrs = writeHeaders(o, "release-delete")
      System.err.println(s"forge: [audit] DELETE $url  tag=$tag draft=$isDraft assets=$nAssets")
      val r = Try(requests.delete(url, headers = hdrs, check = false,
        readTimeout = 30000, connectTimeout = 10000)).getOrElse(die("request failed"))
      r.statusCode match
        case 204 | 200 =>
          println(s"deleted release $what")
          println("  the git tag, if one existed, is untouched (tag deletion is out of scope here)")
        case 404 => die(s"release id $id not found (404) — it may already be gone")
        case c   => die(s"DELETE $url -> $c ${r.statusMessage}\n${r.text().take(500)}")
}

@main def forgeClient(args: String*): Unit = Forge.dispatch(args*)
