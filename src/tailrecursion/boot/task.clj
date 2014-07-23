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

(defn- srcfiles []
  (->> (c/get-env :src-paths)
    (mapcat (comp file-seq io/file))
    (filter #(.isFile %))
    (sort-by #(.getName %))
    (map (juxt c/relative-path identity))))

(defn- depfiles []
  (->> (c/deps)
    (map second)
    (mapcat (partial sort-by #(.getName (io/file (first %)))))))

(c/deftask cljs
  "Compile ClojureScript source files."
  [& {:keys [output-path opts] :or {output-path "main.js"}}]
  (let [deps         (depfiles)
        src-map?     (:source-map opts)
        src-paths    (c/get-env :src-paths)
        output-dir   (c/mktmpdir! ::output-dir)
        cljs-stage   (c/mkoutdir! ::cljs-stage)
        dep-lib-out  (c/mktmpdir! ::dep-lib-out)
        dep-inc-out  (c/mktmpdir! ::dep-inc-out)
        dep-ext-out  (c/mktmpdir! ::dep-ext-out)
        install-inc? (atom nil)
        install-ext? (atom nil)
        install-lib? (atom nil)
        js-out       (io/file cljs-stage output-path)
        smap         (io/file (.getParentFile js-out) (str (.getName js-out) ".map")) 
        base-opts    {:warnings      true
                      :externs       []
                      :libs          []
                      :foreign-libs  []
                      :preamble      []
                      :pretty-print  false
                      :optimizations :whitespace
                      :output-dir    (.getPath output-dir)
                      :output-to     (.getPath js-out)}
        ;; see https://github.com/clojure/clojurescript/wiki/Source-maps
        smap-opts    {:source-map-path ""
                      :source-map      (.getPath smap)
                      :output-dir      (.getPath (.getParentFile js-out))}
        x-opts       (merge base-opts opts (when src-map? smap-opts))]
    (c/consume-src!
      (partial c/by-ext
        (into [".inc.js" ".ext.js"] (if src-map? [] [".clj" ".cljs"]))))
    (c/with-pre-wrap
      (println "Compiling ClojureScript...")
      (let [src-inc-out  (c/mksrcdir! ::src-inc-out)
            src-ext-out  (c/mktmpdir! ::src-ext-out)
            src-lib-out  (c/mktmpdir! ::src-lib-out)
            prelude      (cljs/install-inc install-inc? deps (srcfiles) dep-inc-out src-inc-out)
            externs      (cljs/install-ext install-ext? deps (srcfiles) dep-ext-out src-ext-out)
            libs         (cljs/install-lib install-lib? deps (srcfiles) dep-lib-out src-lib-out)]
        (io/make-parents js-out)
        (cljs/compile-cljs src-paths libs externs prelude x-opts)))))

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
