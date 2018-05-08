package tracehash

import org.scalatest.{FunSuite, Matchers}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.reflect.ClassTag

class Tests extends FunSuite with Matchers with GeneratorDrivenPropertyChecks {
  implicit val arbStackTraceElement: Arbitrary[StackTraceElement] = Arbitrary {
    for {
      cn <- Gen.alphaLowerChar.map(_.toString)
      mn <- Gen.alphaLowerChar.map(_.toString)
      fn <- Gen.alphaLowerChar.map(_.toString)
      ln <- Gen.choose(0, 5)
    } yield new StackTraceElement(cn, mn, fn, ln)
  }

  implicit def arbSlice[A](implicit A: Arbitrary[A], ACT: ClassTag[A]): Arbitrary[Slice[A]] = Arbitrary {
    for {
      arr <- Gen.containerOf[Array, A](A.arbitrary)
      i   <- Gen.choose(0, arr.length)
      l   <- Gen.choose(0, arr.length - i)
    } yield Slice.of(arr).slice(i, l)
  }

  test("slice toList") {
    forAll { s: Array[Int] => Slice.of(s).toList shouldEqual s.toList }
  }

  test("slice reverse toList") {
    forAll { s: Array[Int] => Slice.of(s).reverse.toList shouldEqual s.toList.reverse }
  }

  test("slice empty array") {
    forAll { s: Array[Int] => Slice.of(s).slice(0, 0).toList shouldEqual Nil }
  }

  test("slice reverse length") {
    forAll { s: Slice[Int] => s.reverse.length shouldEqual s.length }
  }

  test("slice reverse 0") {
    forAll { s: Slice[Int] => if (s.length > 0) s.reverse(0) shouldEqual s(s.length - 1) }
  }

  test("slice reverse len-1") {
    forAll { s: Slice[Int] => if (s.length > 0) s.reverse(s.length - 1) shouldEqual s(0) }
  }

  test("sha1") {
    sha1("string") shouldEqual "ecb252044b5ea0f679ee78ec1a12904739e2904d"
  }

  test("bestCover(_, 1)") {
    forAll { s: Array[StackTraceElement] =>
      val c = new Util.MutableCover
      Util.bestCover(s, 1, 1, c)
    }

    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new Util.MutableCover

    {
      Util.bestCover(Array(b, a, a, a, a), 1, 1, r)
      r.coverLength shouldEqual 4
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, a, b, a, a), 1, 1, r)
      r.coverLength shouldEqual 2
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, a, b, a, c), 1, 1, r)
      r.coverLength shouldEqual 0
      r.fragmentLength shouldEqual 0
      r.suffixLength shouldEqual 0
    }
  }

  test("bestCover(_, 2)") {
    forAll { s: Array[StackTraceElement] =>
      val c = new Util.MutableCover
      Util.bestCover(s, 2, 1, c)
    }

    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new Util.MutableCover

    {
      Util.bestCover(Array(b, a, a, a, a), 2, 1, r)
      r.coverLength shouldEqual 4
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, a, b, a, a), 2, 1, r)
      r.coverLength shouldEqual 2
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, a, b, a, c), 2, 1, r)
      r.coverLength shouldEqual 0
      r.fragmentLength shouldEqual 0
      r.suffixLength shouldEqual 0
    }

    {
      Util.bestCover(Array(b, a, b, a, b), 2, 1, r)
      r.coverLength shouldEqual 5
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, b, a, b, a, b), 2, 1, r)
      r.coverLength shouldEqual 5
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }
  }

  test("bestCover(_, 255, 2)") {
    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new Util.MutableCover

    {
      Util.bestCover(Array(b, b, a, b, a, b, a, b), 3, 2, r)
      r.coverLength shouldEqual 7
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }

    {
      Util.bestCover(Array(b, b, a, b, a, b, a, b), 255, 2, r)
      r.coverLength shouldEqual 7
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }
  }

  test("principalStackTrace returns same array slice") {
    forAll { (s: Array[StackTraceElement], b: Boolean) =>
      val r = principalStackTrace(s, b)
      (r.array eq s) shouldEqual true
    }
  }

  test("principalStackTrace returns a reversed slice") {
    forAll { (s: Array[StackTraceElement], b: Boolean) =>
      val r = principalStackTrace(s, b)
      r.reversed shouldEqual true
    }
  }

  test("principalSOStackTrace") {
    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)

    {
      val r = principalSOStackTrace(Array(b, b, a, b, a, b, a, b))
      r.map(_.toList) shouldEqual Some(List(b, a))
    }

    {
      val r = principalSOStackTrace(Array(b, b, a, b, c, a, b, c, a, b))
      r.map(_.toList) shouldEqual Some(List(c, b, a))
    }
  }
}
