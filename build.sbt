seq(samskivert.POMUtil.pomToSettings("pom.xml") :_*)

crossPaths := false

autoScalaLibrary := false // scala-library depend comes from POM

// allows SBT to run junit tests
libraryDependencies += "com.novocode" % "junit-interface" % "0.7" % "test->default"
