(defproject tailrecursion/boot.task "0.1.2"
  :description "Useful tasks for the boot Clojure build tool."
  :url "http://github.com/tailrecursion/boot.task"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.maven/maven-model "3.0.4"
                  :exclusions [org.codehaus.plexus/plexus-utils]]
                 [reply "0.2.0"]])
