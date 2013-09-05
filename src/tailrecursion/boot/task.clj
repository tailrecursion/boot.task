(ns tailrecursion.boot.task
  (:require [tailrecursion.boot.core      :refer [make-event]]
            [clojure.pprint               :refer [pprint]]
            [digest                       :refer [md5]]
            [clojure.java.io              :refer [file]]
            [clojure.data                 :refer [diff]]
            [clojure.set                  :refer [difference intersection union]]))

;; Task builders ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pre-task
  [boot & {:keys [process configure] :or {process identity configure identity}}]
  (locking boot
    (swap! boot configure) 
    (fn [continue]
      (fn [event]
        (continue (process event))))))

(defn post-task
  [boot & {:keys [process configure] :or {process identity configure identity}}]
  (locking boot
    (swap! boot configure) 
    (fn [continue]
      (fn [event]
        (process (continue event))))))

;; A task that does nothing ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def identity-task pre-task)

;; Task that just does configuration of boot env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn config-task [boot configure]
  (pre-task boot :configure configure))

;; Print the event ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-debug [continue]
  (fn [event]
    (pprint event)
    (flush)
    (continue event)))

(defn debug [boot]
  #(wrap-debug %))

;; Loop every msec ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-auto [continue & [msec]]
  (fn [event]
    (continue event)
    (Thread/sleep (or msec 200))
    (recur (make-event event))))

(defn auto [boot & [msec]]
  (locking boot
    (if (:auto @boot)
      (identity-task boot)
      (do (swap! boot assoc :auto true) #(wrap-auto % msec)))))

;; Process event if watched directories have modified files in them ;;;;;;;;;;;

(defn- make-watcher [dir]
  (let [prev (atom nil)]
    (fn []
      (let [file?     #(.isFile %)
            only-file #(filter file? %)
            make-info #(vector [% (.lastModified %)] [% (md5 %)])
            file-info #(mapcat make-info %)
            info      (->> dir file file-seq only-file file-info set)
            mods      (difference (union info @prev) (intersection info @prev))
            by        #(->> %2 (filter (comp %1 second)) (map first) set)]
        (reset! prev info)
        {:hash (by string? mods) :time (by number? mods)}))))

(defn wrap-watch [continue type dirs]
  (let [watchers (map make-watcher dirs)]
    (fn [event]
      (let [info (reduce (partial merge-with union) (map #(%) watchers))]
        (when-not (empty? (info type))
          (continue (assoc event :watch info)))))))

(defn watch [boot & [msec]]
  (let [dirs (:directories @boot)]
    (comp (auto boot msec) #(wrap-watch % :time dirs))))
