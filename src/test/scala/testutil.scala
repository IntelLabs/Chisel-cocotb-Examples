// Adapted from https://github.com/ucb-bar/chiseltest


package testutil

import chiseltest._

trait TestParams {
  val annons = 
    sys.env.get("SIM") match {
      case None => Seq(TreadleBackendAnnotation,WriteVcdAnnotation)
      case Some("verilator") => Seq(VerilatorBackendAnnotation,WriteVcdAnnotation)
      case Some("treadle") => Seq(TreadleBackendAnnotation,WriteVcdAnnotation)
      case Some("vcs") => Seq(VcsBackendAnnotation,WriteVcdAnnotation)
      case Some("icarus") => Seq(IcarusBackendAnnotation,WriteVcdAnnotation)
      case Some(s) =>
        println(s"Unknown value for environment variable SIM: ${s}. Using treadle instead.")
        Seq(TreadleBackendAnnotation,WriteVcdAnnotation)
    }
}
