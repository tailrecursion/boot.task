(defproject tailrecursion/boot.task "2.1.2-SNAPSHOT"
  :description  "Useful tasks for the boot Clojure build tool."
  :url          "http://github.com/tailrecursion/boot.task"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.maven/maven-model "3.0.4"]
                 [tailrecursion/warp "0.1.0" :exclusions [org.clojure/clojure]]
                 [tailrecursion/boot.core "2.2.2-SNAPSHOT"]]
  :profiles {:dev {:dependencies [[tailrecursion/boot "1.0.3-SNAPSHOT"]]}})
