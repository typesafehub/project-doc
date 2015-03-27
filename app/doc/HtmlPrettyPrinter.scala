package doc

import org.kiama.output.PrettyPrinter

import scala.collection.immutable

object HtmlPrettyPrinter extends PrettyPrinter {
  override val defaultIndent = 4

  def aside(d: Doc): Doc =
    angles("aside") <> nest(d) <> angles(forwslash <> "aside")

  def ul(d: immutable.Seq[Doc]): Doc =
    angles("ul") <> nest(lsep(d, softline)) <> angles(forwslash <> "ul")

  def li(name: String): Doc =
    angles("li") <> name <> angles(forwslash <> "li")
}
