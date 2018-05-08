package tracehash

object `package` {
  def sha1(str: String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.update(str.getBytes("UTF-8"))
    md.digest()
      .map(x => Integer.toString((x & 0xff) + 0x100, 16).drop(1))
      .mkString("")
  }

  def stackTraceHash(ex: Throwable): String = {
    val stackTrace = principalStackTrace(
      ex.getStackTrace,
      ex.isInstanceOf[StackOverflowError])

    val stackTraceStr = stackTrace.toList
      .map { el =>
        val className  = Option(el.getClassName).getOrElse("{null}")
        val methodName = Option(el.getMethodName).getOrElse("{null}")
        className + "/" + methodName
      }
      .mkString("|")

    val prefix  = ex.getClass.getName.filter(_.isUpper).mkString("")
    val descStr = ex.getClass.getCanonicalName + " : " + stackTraceStr

    prefix + "-" + sha1(descStr)
  }

  private[this] val SOE_MAX_FRAGMENT_SIZE = 255
  private[this] val SOE_MIN_FRAGMENT_COUNT = 2
  private[this] val NON_SOE_PRINCIPAL_SIZE = 5

  def principalStackTrace(stack: Array[StackTraceElement], stackOverflow: Boolean
                         ): Slice[StackTraceElement] =
    if (stackOverflow) principalSOStackTrace(stack) match {
      case None    => principalNonSOStackTrace(stack)
      case Some(x) => x
    } else principalNonSOStackTrace(stack)

  def principalNonSOStackTrace(stack: Array[StackTraceElement]): Slice[StackTraceElement] =
    Slice.of(stack).slice(0, math.min(NON_SOE_PRINCIPAL_SIZE, stack.length)).reverse

  def principalSOStackTrace(stack: Array[StackTraceElement]): Option[Slice[StackTraceElement]] = {
    val cover = new Util.MutableCover
    Util.bestCover(stack, SOE_MAX_FRAGMENT_SIZE, SOE_MIN_FRAGMENT_COUNT, cover)
    if (cover.coverLength >= cover.fragmentLength * 2) {
      val i = Util.sortFragments(stack, cover.fragmentLength)
      Some(Slice.of(stack).reverse.slice(i, cover.fragmentLength))
    } else None
  }
}
