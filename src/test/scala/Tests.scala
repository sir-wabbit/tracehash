package tracehash.internal

import org.scalatest.{FunSuite, Matchers}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import tracehash.TraceHash

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

//  test("sha1") {
//    sha1("string") shouldEqual "ecb252044b5ea0f679ee78ec1a12904739e2904d"
//  }

  test("bestCover(_, 1)") {
    forAll { s: Array[StackTraceElement] =>
      val c = new SOCoverSolver.Result
      SOCoverSolver.solve(s, 1, 1, c)
    }

    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new SOCoverSolver.Result

    {
      SOCoverSolver.solve(Array(b, a, a, a, a), 1, 1, r)
      r.coverLength shouldEqual 4
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, a, b, a, a), 1, 1, r)
      r.coverLength shouldEqual 2
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, a, b, a, c), 1, 1, r)
      r.coverLength shouldEqual 0
      r.fragmentLength shouldEqual 0
      r.suffixLength shouldEqual 0
    }
  }

  test("bestCover(_, 2)") {
    forAll { s: Array[StackTraceElement] =>
      val c = new SOCoverSolver.Result
      SOCoverSolver.solve(s, 2, 1, c)
    }

    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new SOCoverSolver.Result

    {
      SOCoverSolver.solve(Array(b, a, a, a, a), 2, 1, r)
      r.coverLength shouldEqual 4
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, a, b, a, a), 2, 1, r)
      r.coverLength shouldEqual 2
      r.fragmentLength shouldEqual 1
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, a, b, a, c), 2, 1, r)
      r.coverLength shouldEqual 0
      r.fragmentLength shouldEqual 0
      r.suffixLength shouldEqual 0
    }

    {
      SOCoverSolver.solve(Array(b, a, b, a, b), 2, 1, r)
      r.coverLength shouldEqual 5
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, b, a, b, a, b), 2, 1, r)
      r.coverLength shouldEqual 5
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }
  }

  test("bestCover(_, 255, 2)") {
    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)
    val r = new SOCoverSolver.Result

    {
      SOCoverSolver.solve(Array(b, b, a, b, a, b, a, b), 3, 2, r)
      r.coverLength shouldEqual 7
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }

    {
      SOCoverSolver.solve(Array(b, b, a, b, a, b, a, b), 255, 2, r)
      r.coverLength shouldEqual 7
      r.fragmentLength shouldEqual 2
      r.suffixLength shouldEqual 1
    }
  }

  test("KeyStackTraceComponent") {
    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)

    {
      val state = new KeyStackTraceComponent.State
      val stack = Array(b, b, a, b, a, b, a, b)
      KeyStackTraceComponent.getSO(stack, 255, 2, state)
      stack.slice(state.index, state.index + state.length) shouldEqual Array(a, b)
    }

    {
      val state = new KeyStackTraceComponent.State
      val stack = Array(b, b, a, b, c, a, b, c, a, b)
      KeyStackTraceComponent.getSO(stack, 255, 2, state)
      stack.slice(state.index, state.index + state.length) shouldEqual Array(a, b, c)
    }
  }

  final case class Mocked(stack: Array[StackTraceElement]) extends Throwable {
    override def getStackTrace: Array[StackTraceElement] = stack
  }

  final case class MockedSO(stack: Array[StackTraceElement]) extends StackOverflowError {
    override def getStackTrace: Array[StackTraceElement] = stack
  }

  test("TraceHash.principal") {
    val a = new StackTraceElement("a", "a", "a", 0)
    val b = new StackTraceElement("b", "b", "b", 1)
    val c = new StackTraceElement("c", "c", "c", 2)

    val params = new TraceHash.Parameters(255, 2, 3, false)

    {
      val stack = Array(b, b, a, b, a, b, a, b)
      TraceHash.principal(params, Mocked(stack)) shouldEqual Array(b, b, a)
      TraceHash.principal(params, MockedSO(stack)) shouldEqual Array(a, b)
    }

    {
      val stack = Array(b, b, a, b, c, a, b, c, a, b)
      TraceHash.principal(params, Mocked(stack)) shouldEqual Array(b, b, a)
      TraceHash.principal(params, MockedSO(stack)) shouldEqual Array(a, b, c)
    }
  }
}
