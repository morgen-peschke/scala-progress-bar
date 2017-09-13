package peschke.console.progressbar

import java.io.PrintStream

/**
 * Manages the state of the progress bar.
 *
 * @param count the current counter value
 * @param total the current total
 * @param width the total width of the displayed progress bar
 * @param isTerminated a flag indicating no further changes should be displayed
 */
case class ProgressBarState(count: Long, total: Long, width: Int = 80, isTerminated: Boolean = false) {
  /**
   * Increments the total.
   *
   * This is bounded, and will not go lower than the value of `count`.
   */
  def incrementTotal(delta: Long): ProgressBarState = copy(total = (total + delta).max(count))

  /**
   * Increments the current count.
   *
   * This is bounded, and will not go lower than 0 or higher than the value of `total`.
   */
  def incrementCount(delta: Long): ProgressBarState = {
    val newCount = (count + delta).max(0L).min(total)
    copy(count = newCount)
  }

  def terminated: ProgressBarState = copy(isTerminated = true)

  /**
   * Outputs the bar to the specified [[java.io.PrintStream]].
   *
   * @param output This is a [[java.io.PrintStream]] because that's the type of [[java.lang.System.out]]
   */
  def draw(output: PrintStream): Unit = {
    val isCompleted = isTerminated || count == total
    val percentCompletion = if (isCompleted) 1.0 else count.toDouble / total
    val prefix = {
      val totalStr = s"$total"
      val countStr = s"$count".reverse.padTo(totalStr.length, ' ').reverse
      s"$countStr / $totalStr ["
    }
    val suffix = "] %6.2f%%".format(percentCompletion * 100)
    val middleLength = width - prefix.length - suffix.length
    val bar =
      if (isCompleted) "=" * middleLength
      else (middleLength * percentCompletion).ceil.toInt match {
        case 0 => ""
        case `middleLength` => "=" * (middleLength - 1) + ">"
        case l => ("=" * (l - 1)) + ">"
      }
    val middle = bar.padTo(middleLength, ' ')
    output.print("\r" + prefix + middle + suffix)
    output.flush()
  }
}
