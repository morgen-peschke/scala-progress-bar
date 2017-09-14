package peschke.console.progressbar

import java.io.PrintStream

/**
 * Manages the state of the progress bar.
 *
 * @param count the current counter value
 * @param total the current total
 * @param width the total width of the displayed progress bar
 * @param isFinished a flag indicating no further changes should be displayed
 */
case class ProgressBarState(count: Long, total: Long, width: Int = 80, isFinished: Boolean = false) {
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

  /**
   * Finishes the progress bar, does not modify the count
   */
  def terminated: ProgressBarState = copy(isFinished = true)

  /**
   * Finishes the bar, and sets the count to the total
   */
  def completed: ProgressBarState = copy(isFinished = true, count = total)

  /**
   * Outputs the bar to the specified [[java.io.PrintStream]].
   *
   * @param output This is a [[java.io.PrintStream]] because that's the type of [[java.lang.System.out]]
   */
  def draw(output: PrintStream): Unit = {
    val isCompleted = count == total
    val percentCompletion = if (isCompleted) 1.0 else count.toDouble / total
    val prefix = {
      val totalStr = s"$total"
      val countStr = s"$count".reverse.padTo(totalStr.length, ' ').reverse
      s"$countStr / $totalStr ["
    }
    val suffix = "] %6.2f%%".format(percentCompletion * 100)
    val middleLength = width - prefix.length - suffix.length
    def endchar = if (isFinished) "|" else ">"
    val bar =
      if (isCompleted) "=" * middleLength
      else (middleLength * percentCompletion).ceil.toInt match {
        case 0 => ""
        case `middleLength` => "=" * (middleLength - 1) + endchar
        case l => ("=" * (l - 1)) + endchar
      }
    val middle = bar.padTo(middleLength, ' ')
    output.print("\r" + prefix + middle + suffix)
    output.flush()
  }
}
