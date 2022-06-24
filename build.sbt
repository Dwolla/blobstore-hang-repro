lazy val `blobstore-hang-repro` = (project in file("."))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= {
      val fs2Version = "3.2.8"

      Seq(
        "co.fs2" %% "fs2-core" % fs2Version,
        "co.fs2" %% "fs2-io" % fs2Version,
      )
    },
  )
