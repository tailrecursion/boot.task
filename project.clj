(defproject tailrecursion/boot.task "2.2.4"
  :description  "Useful tasks for the boot Clojure build tool."
  :url          "http://github.com/tailrecursion/boot.task"
  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.apache.maven/maven-model "3.0.4"]
                 [tailrecursion/warp "0.1.0" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[tailrecursion/boot "1.0.3-SNAPSHOT"]]}})
