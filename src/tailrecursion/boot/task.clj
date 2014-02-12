;; Copyriuht (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require 
   [clojure.java.io                    :as io]
   [tailrecursion.boot.core            :as c]
   [tailrecursion.boot.file            :as f]
   [tailrecursion.boot.task.util.pom   :as p]
   [tailrecursion.boot.task.util.jar   :as j]
   [tailrecursion.boot.task.util.cljs  :as cljs]))

;; Compile ClojureScript ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(c/deftask cljs
  "Compile ClojureScript source files."
  [& {:keys [output-path opts] :or {output-path "main.js"}}]
  (c/consume-src! (partial c/by-ext [".clj" ".cljs" ".inc.js" ".ext.js"]))
  (let [depjars    (c/deps)
        base-opts  {:warnings      true
                    :externs       []
                    :libs          []
                    :foreign-libs  []
                    :pretty-print  false
                    :optimizations :whitespace}
        src-paths  (c/get-env :src-paths)
        inc-out    (c/mktmpdir! ::inc-out)
        ext-out    (c/mktmpdir! ::ext-out)
        lib-out    (c/mktmpdir! ::lib-out)
        flib-out   (c/mktmpdir! ::flib-out)
        output-dir (c/mktmpdir! ::output-dir)
        cljs-out   (c/mktmpdir! ::cljs-out)
        cljs-stage (c/mkoutdir! ::cljs-stage)
        x-opts     (->> {:output-dir (f/path output-dir)} (merge base-opts opts))]
    (c/consume-src! (partial c/by-ext [".clj" ".cljs"]))
    (cljs/install-deps src-paths depjars inc-out ext-out lib-out flib-out)
    (c/with-pre-wrap
      (when-let [srcs (c/newer? (c/by-ext [".cljs"] (c/src-files)) cljs-out)]
        (c/mktmpdir! ::cljs-out)
        (println "Compiling ClojureScript...")
        (let [src-paths (c/get-env :src-paths)
              js-out    (doto (io/file cljs-out output-path) io/make-parents)
              opts      (assoc x-opts :output-to (.getPath js-out))]
          (cljs/compile-cljs src-paths flib-out lib-out ext-out inc-out opts)))
      (c/sync! cljs-stage cljs-out))))

;; Build jar files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(c/deftask jar
  "Build a jar file."
  [& {:keys [main manifest]}]
  (assert
    (and (c/get-env :project) (c/get-env :version))
    "Both :project and :version must be defined.")
  (c/with-pre-wrap
    (let [{:keys [project version repositories dependencies src-paths]} (c/get-env)
          src-paths  (map io/file src-paths)
          output-dir (c/mkoutdir! ::jar-out-dir)
          tmp-dir    (c/mktmpdir! ::jar-tmp-dir)
          pom-xml    (p/make-pom project version repositories dependencies src-paths)]
      (j/create-jar! project version src-paths output-dir tmp-dir :main main :manifest manifest :pom pom-xml))))
