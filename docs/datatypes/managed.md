---
id: datatypes_managed
title:  "Managed"
---

`Managed` is a data structure that encapsulates the acquisition and the release of a resource.

A `Managed[E, A]` is a managed resource of type `A`, which may be used by invoking the `use` method of the resource. The resource will be automatically acquired before the resource is used, and automatically released after the resource is used.

Resources do not survive the scope of `use`, meaning that if you attempt to capture the resource, leak it from `use`, and then use it after the resource has been consumed, the resource will not be valid anymore and may fail with some checked error, as per the type of the functions provided by the resource.

```scala mdoc:silent
import scalaz.zio._
def doSomething(queue: Queue[Int]): UIO[Unit] = IO.unit

val managedResource = Managed.make(Queue.unbounded[Int])(_.shutdown)
val usedResource: UIO[Unit] = managedResource.use { queue => doSomething(queue) }
```

In this example, the queue will be created when `use` is called, and `shutdown` will be called when `doSomething` completes.

## Creating a Managed

As shown in the previous example, a `Managed` can be created by passing an `acquire` function and a `release` function.

It can also be created from an effect. In this case the release function will do nothing.
```scala mdoc:silent
import scalaz.zio._
def acquire: IO[String, Int] = IO.succeedLazy(???)

val managedFromEffect: Managed[String, Int] = Managed.fromEffect(acquire)
```

You can create a `Managed` from a pure value as well.
```scala mdoc:silent
import scalaz.zio._
val managedFromValue: Managed[Nothing, Int] = Managed.succeed(3)
```

## Managed with ZIO environment

`Managed[E, A]` is actually an alias for `ZManaged[Any, E, A]`. If you'd like your `acquire`, `release` or `use` functions to require an environment R, just use `ZManaged` instead of `Managed`.

```scala mdoc:silent
import scalaz.zio._
import scalaz.zio.console._

val zManagedResource: ZManaged[Console, Nothing, Unit] = ZManaged.make(console.putStrLn("acquiring"))(_ => console.putStrLn("releasing"))
val zUsedResource: ZIO[Console, Nothing, Unit] = zManagedResource.use { _ => console.putStrLn("running") }
```

## Combining Managed

It is possible to combine multiple `Managed` using `flatMap` to obtain a single `Managed` that will acquire and release all the resources.

```scala mdoc:silent
import scalaz.zio._
```

```scala mdoc:invisible
import java.io.{ File, IOException }

def openFile(s: String): IO[IOException, File] = IO.succeedLazy(???)
def closeFile(f: File): UIO[Unit] = IO.succeedLazy(???)
def doSomething(queue: Queue[Int], file: File): UIO[Unit] = IO.succeedLazy(???)
```

```scala mdoc:silent
val managedQueue: Managed[Nothing, Queue[Int]] = Managed.make(Queue.unbounded[Int])(_.shutdown)
val managedFile: Managed[IOException, File] = Managed.make(openFile("data.json"))(closeFile)

val combined: Managed[IOException, (Queue[Int], File)] = for {
    queue <- managedQueue
    file  <- managedFile
} yield (queue, file)

val usedCombinedRes: IO[IOException, Unit] = combined.use { case (queue, file) => doSomething(queue, file) }

```

## Reservation

Unlike `Managed`, `Reservation` does not bind `release` to `acquire` allowing interruptible resource acquisition. 

```scala mdoc:invisible
import java.io.{ File, IOException, InterruptedIOException }
import scala.util.Try

def openFile(name: String, p: Promise[IOException, File]): IO[IOException, File] = UIO.succeedLazy(new File(name))
def closeFile(p: Promise[IOException, File]): UIO[Unit] = UIO.unit
def readFile(file: File): IO[IOException, String] = IO.succeedLazy("Don't forget to clean up!")
```

```scala mdoc:silent
import scalaz.zio._
```

Whilst having more control, a concurrency primitive such as `Promise` may be needed to help properly manage resource state and clean up.

```scala mdoc:silent
val data: IO[IOException, String] = for {
  p <- Promise.make[IOException,File]
  res = Reservation(openFile("x", p), closeFile(p))
  result <- (for {
      fiber  <- res.acquire.fork
      ex     <- fiber.interrupt
      data   <- ex.toEither.fold(e => IO.fail(new InterruptedIOException("Stop!")), file => readFile(file))
    } yield data).ensuring(res.release)
} yield result
```
