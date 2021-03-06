package is.hail.expr.types

import is.hail.annotations._
import is.hail.asm4s.Code
import is.hail.check.Arbitrary._
import is.hail.check.Gen
import is.hail.expr.ir.EmitMethodBuilder
import is.hail.expr.types.physical.PFloat32
import is.hail.utils._

import scala.reflect.{ClassTag, _}

case object TFloat32Optional extends TFloat32(false)
case object TFloat32Required extends TFloat32(true)

class TFloat32(override val required: Boolean) extends TNumeric {
  lazy val physicalType: PFloat32 = PFloat32(required)

  def _toPretty = "Float32"

  override def pyString(sb: StringBuilder): Unit = {
    sb.append("float32")
  }

  def _typeCheck(a: Any): Boolean = a.isInstanceOf[Float]

  override def str(a: Annotation): String = if (a == null) "NA" else a.asInstanceOf[Float].formatted("%.5e")

  override def genNonmissingValue: Gen[Annotation] = arbitrary[Double].map(_.toFloat)

  override def valuesSimilar(a1: Annotation, a2: Annotation, tolerance: Double, absolute: Boolean): Boolean =
    a1 == a2 || (a1 != null && a2 != null && {
      val f1 = a1.asInstanceOf[Float]
      val f2 = a2.asInstanceOf[Float]

      val withinTol =
        if (absolute)
          math.abs(f1 - f2) <= tolerance
        else
          D_==(f1, f2, tolerance)

      f1 == f2 || withinTol || (f1.isNaN && f2.isNaN)
    })

  override def scalaClassTag: ClassTag[java.lang.Float] = classTag[java.lang.Float]

  val ordering: ExtendedOrdering =
    ExtendedOrdering.extendToNull(implicitly[Ordering[Float]])

  override def byteSize: Long = 4
}

object TFloat32 {
  def apply(required: Boolean = false): TFloat32 = if (required) TFloat32Required else TFloat32Optional

  def unapply(t: TFloat32): Option[Boolean] = Option(t.required)
}
