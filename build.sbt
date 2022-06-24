lazy val `blobstore-hang-repro` = (project in file("."))
  .settings(
    scalaVersion := "2.13.8",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    libraryDependencies ++= {
      val awsSdkV = "2.17.213"
      val nettyTcNativeV = "2.0.53.Final"

      Seq(
        "org.typelevel" %% "cats-effect" % "3.3.12",
        "co.fs2" %% "fs2-core" % "3.2.8",
        "com.github.fs2-blobstore" %% "s3" % "0.9.6",
        "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
        "software.amazon.awssdk" % "s3" % awsSdkV,
        "software.amazon.awssdk" % "utils" % awsSdkV,
        "ch.qos.logback" % "logback-classic" % "1.2.11" % Runtime,
        "io.netty" % "netty-tcnative" % nettyTcNativeV % Runtime,
        "io.netty" % "netty-tcnative-boringssl-static" % nettyTcNativeV % Runtime,
      )
    },
  )
