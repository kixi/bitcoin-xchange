package commons

class StopWatch {
  val start = System.currentTimeMillis();

  def stop = {
    System.currentTimeMillis() - start
  }
}

object StopWatch {
  def apply = new StopWatch
}