;;;-------------------------------------------------------------------------------------------------
;;; Copyright (c) Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and 
;;; distribution terms for this software are covered by the Eclipse Public License 1.0. By using 
;;; this software in any fashion, you are agreeing to be bound by the terms of this license.  You 
;;; must not remove this notice, or any other, from this software.
;;; http://www.eclipse.org/legal/epl-v10.html
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util
  (:require
    [clojure.set :refer [difference]] ))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn extract-ids [sym]
  (let [[group artifact] ((juxt namespace name) sym)]
    [(or group artifact) artifact] ))

(defn parse-args [[head & tail :as args]]
  (let [kw1? (comp keyword? first)
        mkkw #(->> (partition 2 %) (take-while kw1?) (map vec))
        drkw #(->> (partition 2 2 [] %) (drop-while kw1?) (mapcat identity))]
    (cond (map?     head) [head tail]
          (keyword? head) [(into {} (mkkw args)) (drkw args)]
          :else           [{} args]) ))

(defmacro defelem [name & forms]
  (let [[_ name [_ & [[bind & body]]]] (macroexpand-1 `(defn ~name ~@forms))]
    `(def ~name (fn [& args#] (let [~bind (parse-args args#)] ~@body))) ))

(defmacro let-assert-keys [binding & body]
  "Let expression that throws an exception when any of the expected bindings is missing."
  (let [[ks m] [(butlast binding) (last binding)]
        req-ks (set (map keyword ks)) ]
   `(if-let [dif-ks# (not-empty (difference ~req-ks (set (keys ~m))))]
      (throw (new AssertionError (apply format "missing key(s): %s" dif-ks#)))
      (let [{:keys ~ks} ~m] ~@body) )))

(defmacro dotoseq [obj seq-exprs & body]
  `(let [o# ~obj] (doseq ~seq-exprs (doto o# ~@body)) o#) )
