package civil.services

import org.owasp.html.HtmlPolicyBuilder


trait HTMLSanitizer {
  def sanitize(untrustedHTML: String): String
}

case class HTMLSanitizerLive() extends HTMLSanitizer {
  override def sanitize(untrustedHTML: String): String = {
    val policy = new HtmlPolicyBuilder()
      .allowElements("a")
      .allowElements("b")
      .allowElements("i")
      .allowElements("s")
      .allowElements("p")
      .allowElements("strong")
      .allowElements("u")
      .allowElements("em")
      .allowElements("blockquote")

      .allowUrlProtocols("https")
      .allowAttributes("href")
      .onElements("a")
      .requireRelNofollowOnLinks.toFactory
    val safeHTML = policy.sanitize(untrustedHTML)
    safeHTML
  }
}
