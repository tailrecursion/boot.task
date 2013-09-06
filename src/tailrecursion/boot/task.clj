(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require [tailrecursion.boot.task.file :as f]
            [tailrecursion.boot.core      :refer [make-event]]
            [clojure.stacktrace           :refer [print-cause-trace]]
            [clojure.string               :refer [split join blank?]]
            [clojure.pprint               :refer [pprint]]
            [digest                       :refer [md5]]
            [clojure.java.io              :refer [file]]
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

(defn- print-time [msg ok fail f]
  (let [now!    #(System/currentTimeMillis)
        time*   #(let [start (now!)] (%) (double (/ (- (now!) start) 1000)))
        trace   #(with-out-str (print-cause-trace %))
        printf* (fn [& s] (apply printf s) (flush))]
    (printf* msg)
    (try (printf* ok (time* f))
      (catch Throwable e (printf* fail (trace e))))))

(defn wrap-watch [continue type dirs]
  (let [watchers (map make-watcher dirs)]
    (fn [event]
      (let [info (reduce (partial merge-with union) (map #(%) watchers))]
        (when-let [mods (seq (info type))] 
          (let [path  (.getPath (first mods))
                xtr   (when-let [c (next mods)] (format " and %d others" (count c)))
                msg   (format "Building %s%s..." path (str xtr))
                ok    "done. (%.3f sec)\n"
                fail  "dang!\n%s\n"]
            (print-time msg ok fail #(continue (assoc event :watch info)))))))))

(defn watch [boot & [msec]]
  (let [dirs (:directories @boot)]
    (comp (auto boot msec) #(wrap-watch % :time dirs))))

;; Sync (copy) files between directories ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-sync [continue type dst srcs]
  (let [dst (file dst), srcs (map file srcs)]
    (fn [event]
      (apply f/sync type dst srcs)
      (continue event))))

(defn sync [boot dst srcs]
  #(wrap-sync % dst srcs))
