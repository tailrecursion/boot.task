;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.deps)

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn print-tree [tree & [depth]]
  (let [depth (or depth 0)]
    (doseq [[coord branch] tree]
      (println (apply str (repeat (* depth 4) \space)) coord)
      (when branch (print-tree branch (inc depth))) )))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn list-deps [reps deps]
  (println "Listing dependencies...")
  (require 'cemerick.pomegranate.aether)
  (let [dep-tree  (resolve 'cemerick.pomegranate.aether/dependency-hierarchy) 
        res-deps  (resolve 'cemerick.pomegranate.aether/resolve-dependencies) 
        dep-graph (res-deps :repositories (zipmap reps reps) :coordinates deps :transfer-listener :stdout) ]
    (print-tree (dep-tree deps dep-graph)) ))
