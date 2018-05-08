# TraceHash
TraceHash hashes your exceptions into exception signatures that formalize the intuitive notion of "exception sameness": exceptions with the same signature are normally considered "the same" (e.g. when filing bug reports).

## Usage

```scala
tracehash.stackTraceHash(exception)
// will produce something like
// "SOE-b33ffcec6a101750802bcebecae59e6a657145aa"
// or "IOOBE-1b4035e1d5b6023ecd1ef2673278057b5a3bb44c"
```

## Motivation

Say you are fuzzing a Java application, and find an `AssertionError`:

```scala
java.lang.AssertionError: assertion failed: position error: position not set for Ident(<error>) # 5299
        at scala.Predef$.assert(Predef.scala:219)
        at dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:179)
        at dotty.tools.dotc.ast.Positioned.$anonfun$checkPos$4(Positioned.scala:203)
        at dotty.tools.dotc.ast.Positioned.$anonfun$checkPos$4$adapted(Positioned.scala:203)
        at scala.collection.immutable.List.foreach(List.scala:389)
        at dotty.tools.dotc.ast.Positioned.check$1(Positioned.scala:203)
        at dotty.tools.dotc.ast.Positioned.checkPos(Positioned.scala:216)
        ....
```

If the same `AssertionError` happens in another file, the two errors are probably related, and you should only file one issue for both of them. But what exactly do we mean by "same error"? What algorithm should we
use to compare different exception traces?

**Should we compare the entire stacktrace?**
No, folklore and experience tells us that only the last few stacktrace entries are important.

**Should we compare line numbers?**
If someone changes one of the files appearing in the stacktrace without fixing the error, line numbers might change, but the error won't. Therefore, we should not take line numbers into account.

**Should we compare messages?**
Unless we can inspect the code generating messages, we don't know which parts of the message stay constant and which depend on a particular fuzzer input or change non-deterministically.

**Should we compare file names?**
File names are less important than class names, especially in Scala, where a single file can contain multiple classes.

---

Simplified exception trace:
```scala
java.lang.AssertionError
        at scala.Predef$.assert
        at dotty.tools.dotc.ast.Positioned.check$1
        at dotty.tools.dotc.ast.Positioned.$anonfun$checkPos$4
        at dotty.tools.dotc.ast.Positioned.$anonfun$checkPos$4$adapted
        at scala.collection.immutable.List.foreach
```

### Stack overflows

Special care needs to be taken to simplify `StackOverflowException`,
such as:

```scala
java.lang.StackOverflowError
        at dotty.tools.dotc.core.Types$TypeProxy.superType(Types.scala:1460)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:182)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:178)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:182)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:178)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:182)
```

We can see that this stacktrace consists of a repeating fragment of
length 11:
```scala
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:182)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:192)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
        at dotty.tools.dotc.core.TypeApplications$.$anonfun$typeParams$extension$1(TypeApplications.scala:178)
        at dotty.tools.dotc.util.Stats$.track(Stats.scala:35)
        at dotty.tools.dotc.core.TypeApplications$.typeParams$extension(TypeApplications.scala:171)
```

and a prefix of length 1:
```scala
        at dotty.tools.dotc.core.Types$TypeProxy.superType(Types.scala:1460)
```

Clearly, the prefix is not important, only the repeating fragment *is*.

Note that looking at the very end of a `StackOveflowException` stacktrace, we can not tell how the repeating fragment started. For instance, let's imagine that our stacktrace ends in `d b a b c a b c a b c`. We can not tell if the repeating fragment is `a b c` or `b c a` or `c a b`. In order to produce consistent signatures, `TraceHash` sorts all possible options in lexicographic order.
