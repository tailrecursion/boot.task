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
  (let [depjars    (c/deps)
        src-map?   (:source-map opts)
        src-paths  (c/get-env :src-paths)
        inc-out    (c/mktmpdir! ::inc-out)
        ext-out    (c/mktmpdir! ::ext-out)
        lib-out    (c/mktmpdir! ::lib-out)
        flib-out   (c/mktmpdir! ::flib-out)
        output-dir (c/mktmpdir! ::output-dir)
        cljs-stage (c/mkoutdir! ::cljs-stage)
        js-out     (io/file cljs-stage output-path)
        smap       (io/file cljs-stage (str output-path ".map")) 
        smap-path  (str (.getParent (io/file output-path)))
        base-opts  {:warnings      true
                    :externs       []
                    :libs          []
                    :foreign-libs  []
                    :pretty-print  false
                    :optimizations :whitespace
                    :output-dir    (.getPath output-dir)
                    :output-to     (.getPath js-out)}
        ;; see https://github.com/clojure/clojurescript/wiki/Source-maps
        smap-opts  {:source-map-path smap-path
                    :source-map      (.getPath smap)
                    :output-dir      (.getPath cljs-stage)}
        x-opts     (merge base-opts opts (when src-map? smap-opts))]
    (c/consume-src!
      (partial c/by-ext
        (into [".inc.js" ".ext.js"] (if src-map? [] [".clj" ".cljs"]))))
    (cljs/install-deps src-paths depjars inc-out ext-out lib-out flib-out)
    (c/with-pre-wrap
      (println "Compiling ClojureScript...")
      (io/make-parents js-out)
      (cljs/compile-cljs src-paths flib-out lib-out ext-out inc-out x-opts))))

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
