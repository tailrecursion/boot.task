;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require
   [clojure.java.io                       :as io]
   [tailrecursion.boot.core               :as c]
   [tailrecursion.boot.file               :as f]
   [tailrecursion.boot.task.util          :as u]
   [tailrecursion.boot.task.util.cljs     :as j]
   [tailrecursion.boot.task.util.dist     :refer [spit-dist!]]
   [tailrecursion.boot.task.util.dist.dep :refer [add-dep!]]
   [tailrecursion.boot.task.util.dist.pom :refer [pom-xml add-pom! spit-pom!]]
   [tailrecursion.boot.task.util.dist.src :refer [add-src!]]
   [tailrecursion.boot.task.util.dist.web :refer [add-web!]] ))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn filename [aid vers ext] (str (if aid (str aid "-" vers) "project") "." ext))

;;; tasks ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
        x-opts     (merge base-opts opts (when src-map? smap-opts)) ]
    (c/consume-src!
      (partial c/by-ext
        (into [".inc.js" ".ext.js"] (if src-map? [] [".clj" ".cljs"]))))
    (j/install-deps src-paths depjars inc-out ext-out lib-out flib-out)
    (c/with-pre-wrap
      (println "Compiling ClojureScript...")
      (io/make-parents js-out)
      (j/compile-cljs src-paths flib-out lib-out ext-out inc-out x-opts) )))

(c/deftask jar
  "Build a jar file."
  [& {:keys [main manifest]}]
  (u/let-assert-keys [dst-path src-paths project version (c/get-env)]
    (let [[gid aid] (u/extract-ids project)
          {d :description u :url {ln :name lu :url} :license deps :dependencies reps :repositories} (c/get-env)
          main     (if main main (c/get-env :main))
          pom-xml  (pom-xml gid aid version d u ln lu deps reps) 
          jar-file (io/file dst-path (filename aid version "jar")) ]
      (c/with-pre-wrap
        (spit-dist! jar-file main manifest
          (add-pom! gid aid version pom-xml)
          (add-src! src-paths) )))))

(c/deftask war
  "Build a war file."
  [& {:keys [main manifest]}]
  (u/let-assert-keys [dst-path src-paths project version (c/get-env)]
    (let [aid  (second (u/extract-ids project))
          main (if main main (c/get-env :main))
          file (io/file dst-path (filename aid version "war")) ]
      (c/with-pre-wrap
        (spit-dist! file main manifest
          (add-web! aid (c/get-env :description) "/*" "test-class" )
          (add-src! src-paths "WEB-INF/classes") )))))

(c/deftask uberwar
  "Build an uberwar file."
  [& {:keys [main manifest]}]
  (u/let-assert-keys [dst-path src-paths project version (c/get-env)]
    (let [aid  (second (u/extract-ids project))
          main (if main main (c/get-env :main))
          file (io/file dst-path (filename aid version "war")) ]
      (c/with-pre-wrap
        (spit-dist! file main manifest
          (add-web! aid (c/get-env :description) "/*" "test-class" )
          (add-src! src-paths "WEB-INF/classes")
          (add-dep! (c/get-env :repositories) (c/get-env :dependencies)) )))))

(c/deftask install
  "Build and install the jar and pom files into the local repository."
  [& {:keys [main manifest]}]
  (require 'cemerick.pomegranate.aether)
  (u/let-assert-keys [dst-path src-paths project version (c/get-env)]
    (let [[gid aid] (u/extract-ids project)
          {d :description u :url {ln :name lu :url} :license deps :dependencies reps :repositories} (c/get-env)
          main     (if main main (c/get-env :main))
          tmp-dir  (c/mktmpdir! ::tmp-dir)
          jar-file (io/file tmp-dir (filename aid version "jar")) 
          pom-xml  (pom-xml gid aid version d u ln lu deps reps) 
          pom-file (io/file tmp-dir (filename aid version "pom"))
          install  (resolve 'cemerick.pomegranate.aether/install)]
      (c/with-pre-wrap
        (spit-dist! jar-file main manifest
          (add-pom! gid aid version pom-xml)
          (add-src! src-paths) )
        (spit-pom! pom-file pom-xml)
        (install :coordinates [project version] :jar-file jar-file :pom-file pom-file) ))))
