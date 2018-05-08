package tracehash

private[tracehash] final class Slice[A]
(val array: Array[A],
 val start: Int,
 val length: Int,
 val reversed: Boolean
) {
  private[this] def index(i: Int): Int =
    if (!reversed) start + i
    else start + length - 1 - i

  def apply(i: Int): A = {
    require(0 <= i && i < length, "invalid index")
    array(index(i))
  }

  def slice(newStart: Int, newLength: Int): Slice[A] = {
    require(0 <= newStart && newStart <= length, "invalid newStart")
    require(0 <= newLength && newStart + newLength <= length, "invalid newLength")
    val from = index(newStart)
    val to = index(newStart + newLength - 1)
    new Slice(array, math.min(from, to), newLength, reversed)
  }

  def reverse: Slice[A] =
    new Slice(array, start, length, !reversed)

  def toList: List[A] = {
    val builder = List.newBuilder[A]
    var i: Int = 0
    while (i < length) {
      builder += apply(i)
      i += 1
    }
    builder.result()
  }

  override def toString: String = {
    val builder = new StringBuilder()
    builder.append("Slice(")
    var i: Int = 0
    while (i < length) {
      builder.append(apply(i))
      if (i < length - 1) builder.append(", ")
      i += 1
    }
    builder.append(")")
    builder.result()
  }
}
object Slice {
  def of[A](array: Array[A]): Slice[A] =
    new Slice(array, 0, array.length, false)
}