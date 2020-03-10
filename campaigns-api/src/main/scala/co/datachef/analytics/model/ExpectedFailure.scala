package co.datachef.analytics.model

sealed abstract class ExpectedFailure extends Exception
case class DSFailure(throwable: Throwable) extends ExpectedFailure
case class NotFoundFailure(message: String) extends ExpectedFailure
