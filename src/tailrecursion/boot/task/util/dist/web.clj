;;;-------------------------------------------------------------------------------------------------
;;; Copyright Alan Dipert, Micha Niskin, & jumblerg. All rights reserved. The use and distribution 
;;; terms for this software are covered by the Eclipse Public License 1.0 (http://www.eclipse.org/
;;; legal/epl-v10.html). By using this software in any fashion, you are agreeing to be bound by the 
;;; terms of this license.  You must not remove this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.boot.task.util.dist.web
  (:require
    [clojure.java.io                  :refer [writer]]
    [tailrecursion.boot.task.util.xml :refer [decelems defelem element]] )
  (:import
    [java.util.jar JarEntry JarOutputStream] ))

;;; elements ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(decelems description display-name param-name param-value servlet-class servlet-name url-pattern)

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defelem init-param [{n :name v :value}]
  (element :init-param
    (param-name  n)
    (param-value v) ))

(defelem servlet [{n :name c :class} elems]
  (element :servlet
    (servlet-name  n)
    (servlet-class c) 
    elems ))

(defelem servlet-mapping [{n :name u :url}]
  (element :servlet-mapping
    (servlet-name n)
    (url-pattern  u) ))

(defelem web-app [{n :name d :description} elems]
  (element :web-app
    :xmlns              "http://java.sun.com/xml/ns/javaee"
    :xmlns:xsi          "http://www.w3.org/2001/XMLSchema-instance"
    :xsi:schemaLocation "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
    :version            "3.0"
    :metadata-complete  "true"
    (display-name n)
    (description  d)
    elems ))

(defn web-xml [name desc class & [params]]
  (web-app :name name :description desc 
    (servlet :name name :class class (for [[n v] params] 
      (init-param :name n :value v) )) 
    (servlet-mapping :name name :url "/*") ))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-web! [& args]
  (println "Creating web.xml...")
  (let [path "WEB-INF/web.xml"
        web-xml (apply web-xml args) ]
    (fn [^JarOutputStream stream]
      (.putNextEntry stream (JarEntry. path))
      (.write stream (.getBytes (pr-str web-xml)))
      (.closeEntry stream) )))
