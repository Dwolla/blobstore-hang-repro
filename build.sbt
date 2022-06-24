lazy val `blobstore-hang-repro` = (project in file("."))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= {
      val fs2Version = "3.2.8"

      Seq(
        "org.typelevel" %% "cats-effect" % "3.3.12",
        "co.fs2" %% "fs2-core" % fs2Version,
        "co.fs2" %% "fs2-io" % fs2Version,
        "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
        "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime,
      )
    },
  )
