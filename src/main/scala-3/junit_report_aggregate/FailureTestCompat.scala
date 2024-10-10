package junit_report_aggregate

trait FailureTestCompat { self: JunitReportAggregatePlugin.FailureTest =>
  private[junit_report_aggregate] def toTupleOption = Option(Tuple.fromProductTyped(self))
}
