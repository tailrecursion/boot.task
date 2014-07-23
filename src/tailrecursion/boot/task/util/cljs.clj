;; Copyright (c) Alan Dipert and Micha Niskin. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns tailrecursion.boot.task.util.cljs
  (:require 
   [tailrecursion.boot.core :as boot]
   [clojure.java.io :as io]
   [clojure.string  :as string]))

(defn copy-resource
  [resource-path out-path]
  (with-open [in  (io/input-stream (io/resource resource-path))
              out (io/output-stream (io/file out-path))]
    (io/copy in out)))

(defn do-once [state f] #(when (compare-and-set! state nil true) (f)))

(defn install-inc [state deps srcs dep-dir src-dir]
  (let [filter* (partial filter #(re-find #"\.inc\.js$" (first %)))
        dep-out (io/file dep-dir "c6f4dce0-0384-11e4-9191-0800200c9a66.dep.prelude.js")
        src-out (io/file src-dir "c6f4dce0-0384-11e4-9191-0800200c9a66.prelude.js")
        cat     #(->> % (map (comp slurp second)) (string/join "\n"))
        write   #(doall (spit %2 (cat (filter* %1)) :append %3))
        do-deps (do-once state #(do (write deps dep-out false) ::ok))]
    (do-deps)
    (io/copy dep-out src-out)
    (write srcs src-out true)
    (.getName src-out)))

(defn install-files [re state deps srcs dep-dir src-dir]
  (let [outpath #(str (gensym) "-" (.getName (io/file %)))
        outfile #(doto (io/file %1 %2) io/make-parents)
        filter* (partial filter #(re-find re (first %)))
        copysrc #(io/copy (second %) (outfile src-dir (outpath (first %))))
        copyres #(copy-resource (first %) (outfile dep-dir (outpath (first %))))
        write   #(do (doall (map %2 (filter* %1))) ::ok)
        do-deps (do-once state #(write deps copyres))]
    (do-deps)
    (write srcs copysrc)
    (->> (file-seq dep-dir)
      (concat (file-seq src-dir))
      (filter #(.isFile %))
      (map #(.getPath %)))))

(def install-ext (partial install-files #"\.ext\.js$"))
(def install-lib (partial install-files #"\.lib\.js$"))

(defn compile-cljs [& args]
  (require 'tailrecursion.boot.task.util.cljs.compiler)
  (apply (resolve 'tailrecursion.boot.task.util.cljs.compiler/compile) args))
