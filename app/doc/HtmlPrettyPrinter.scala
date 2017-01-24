package doc

import java.net.URI

import org.kiama.output.PrettyPrinter

import scala.collection.immutable

object HtmlPrettyPrinter extends PrettyPrinter {
  override val defaultIndent = 4
  override val defaultWidth = 132

  def div(d: Doc, id: Option[String] = None, clazz: Option[String] = None): Doc =
    angles("div" <> parseId(id) <> parseClass(clazz)) <> d <>  angles(forwslash <> "div")

  def aside(d: Doc): Doc =
    angles("aside") <@> indent(d) <@> angles(forwslash <> "aside")

  def ul(d: immutable.Seq[Doc]): Doc =
    angles("ul") <> nest(lsep(d, softbreak)) <@> angles(forwslash <> "ul")

  def li(d: Doc): Doc =
    angles("li") <> d <> angles(forwslash <> "li")
  def li(name: String, d: Doc): Doc =
    angles("li") <> name <@> indent(d) <@> angles(forwslash <> "li")

  def a(name: String, ref: URI): Doc =
    angles("a" <+> "href" <> "=" <> dquotes(ref.toString)) <> name <> angles(forwslash <> "a")

  def select(d: Doc, clazz: Option[String] = None, onChange: String): Doc =
    angles("select" <+> "onchange" <> "=" <> dquotes(onChange) <> parseClass(clazz)) <> d <> angles(forwslash <> "select")

  def option(d: Doc, value: String, selected: Boolean): Doc =
    angles("option" <+> "value" <> "=" <> dquotes(value) <+> (if (selected) "selected" else empty)) <> d <> angles(forwslash <> "option")

  def p(d: Doc): Doc =
    angles(s"p") <> d <> angles(forwslash <> s"p")

  def h1(implicit d: Doc): Doc =
    h(1)

  def h2(implicit d: Doc): Doc =
    h(2)

  def h3(implicit d: Doc): Doc =
    h(3)

  def h4(implicit d: Doc): Doc =
    h(4)

  def h5(implicit d: Doc): Doc =
    h(5)

  private def h(level: Int)(implicit d: Doc): Doc =
    angles(s"h$level") <> d <> angles(forwslash <> s"h$level")

  def nav(d: Doc, id: Option[String] = None, clazz: Option[String] = None): Doc = {
    angles("nav" <> parseId(id) <> parseClass(clazz)) <> d <>  angles(forwslash <> "nav")
  }

  private def parseId(id: Option[String]): Doc =
    id map (" id" <> "=" <> dquotes(_)) getOrElse empty

  private def parseClass(clazz: Option[String]): Doc =
    clazz map (" class" <> "=" <> dquotes(_)) getOrElse empty
}
