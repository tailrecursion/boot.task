;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist.src
  (:require
    [tailrecursion.boot.task.util :as u]
    [clojure.java.io              :as io] )
  (:import
    [java.util.jar JarOutputStream] ))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-src! [src-paths & [tgt-path]]
  (println "Adding source files...")
  (fn [^JarOutputStream stream]
    (u/dotoseq stream [s src-paths :let [src-path (io/file s)]]
      (u/add! (io/file tgt-path) src-path src-path) )))
