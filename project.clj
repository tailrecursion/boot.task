(defproject tailrecursion/boot.task "2.1.0"
  :description  "Useful tasks for the boot Clojure build tool."
  :url          "http://github.com/tailrecursion/boot.task"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.maven/maven-model "3.0.4"
                  :exclusions [org.codehaus.plexus/plexus-utils]]
                 [tailrecursion/warp "0.1.0" :exclusions [org.clojure/clojure]]
                 [tailrecursion/boot.core "2.2.1"]]
  :profiles {:dev {:dependencies [[tailrecursion/boot "1.0.1"]
                                  [tailrecursion/boot.core "2.0.0"]]}})
