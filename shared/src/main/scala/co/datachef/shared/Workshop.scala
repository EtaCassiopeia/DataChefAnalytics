package co.datachef.shared

import zio._

object ZIOTypes {

  type ??? = Nothing
  /**
   * EXERCISE
   *
   * Provide definitions for the ZIO type aliases below.
   */

  //ZIO[-R, +E ,+A] ~ R => Either[E, A]
  /*Functional Effect: Lazy description of an action to interact with the outside world, by running
  an effect we can translate it to a running effect
   */
  type Task[+A] = ???
  type UIO[+A] = ???
  type RIO[-R, +A] = ???
  type IO[+E, +A] = ???
  type URIO[-R, +A] = ???
}

object HelloWorld extends App {

  import zio.console._

  /**
   * EXERCISE
   *
   * Implement a simple "Hello World!" program using the effect returned by `putStrLn`.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???
}

object PrintSequence extends App {

  import zio.console._

  /**
   * EXERCISE
   *
   * Using `*>` (`zipRight`), compose a sequence of `putStrLn` effects to
   * produce an effect that prints three lines of text to the console.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???
}

object ErrorRecovery extends App {
  val StdInputFailed = 1

  import zio.console._

  val failed: ZIO[Console, String, Unit] =
    putStrLn("About to fail...") *>
      ZIO.fail("Uh oh!") *>
      putStrLn("This will NEVER be printed!")

  /**
   * EXERCISE
   *
   * Using `ZIO#orElse` or `ZIO#fold`, have the `run` function compose the
   * preceding `failed` effect into the effect that `run` returns.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???
}

object Looping extends App {

  import zio.console._

  /**
   * EXERCISE
   *
   * Implement a `repeat` combinator using `flatMap` and recursion.
   */
  def repeat[R, E, A](n: Int)(effect: ZIO[R, E, A]): ZIO[R, E, A] = ???

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    repeat(100)(putStrLn("All work and no play makes Jack a dull boy")) as 0
}


object PromptName extends App {
  val StdInputFailed = 1

  import zio.console._

  /**
   * EXERCISE
   *
   * Using `ZIO#flatMap`, implement a simple program that asks the user for
   * their name (using `getStrLn`), and then prints it out to the user (using `putStrLn`).
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???

}

object NumberGuesser extends App {

  import zio.console._
  import zio.random._

  def analyzeAnswer(random: Int, guess: String) =
    if (random.toString == guess.trim) putStrLn("You guessed correctly!")
    else putStrLn(s"You did not guess correctly. The answer was $random")

  /**
   * EXERCISE
   *
   * Choose a random number (using `nextInt`), and then ask the user to guess
   * the number, feeding their response to `analyzeAnswer`, above.
   */
  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???
}

object AlarmApp extends App {

  import java.io.IOException
  import java.util.concurrent.TimeUnit

  import zio.console._
  import zio.duration._

  /**
   * EXERCISE
   *
   * Create an effect that will get a `Duration` from the user, by prompting
   * the user to enter a decimal number of seconds.
   * narrow the error type as necessary.
   */

  def toDouble(s: String): Either[NumberFormatException, Double] =
    try Right(s.toDouble) catch {
      case e: NumberFormatException => Left(e)
    }

  lazy val getAlarmDuration: ZIO[Console, IOException, Duration] = ???


  /**
   * EXERCISE
   *
   * Create a program that asks the user for a number of seconds to sleep,
   * sleeps the specified number of seconds using ZIO.sleep(d), and then
   * prints out a wakeup alarm message, like "Time to wakeup!!!".
   */

  def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = ???
}

