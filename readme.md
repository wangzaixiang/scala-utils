* Overview

  Scala-Utils is a set of utilities under namespace ```todo```
  
* dbunit
    sbt dbunit/publishSigned

* b2c(bean2case)
  a util to generate immutable case class for JavaBeans, and java interface(ref to javabean) to scala trait(ref to case class).
  b2c aims to provide a utility when migrate java projects to scala, we generate the scala side code for services, and then re-implementation using scala.
