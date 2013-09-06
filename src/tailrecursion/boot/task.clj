(ns tailrecursion.boot.task
  (:refer-clojure :exclude [sync])
  (:require 
    [tailrecursion.boot.task.util.file  :as f]
    [tailrecursion.boot.task.util.cljs  :refer [compile-cljs]]
    [tailrecursion.boot.task.util.pom   :refer [make-pom]]
    [tailrecursion.boot.task.util.jar   :refer [create-jar!]]
    [tailrecursion.boot.core            :refer [make-event]]
    [tailrecursion.boot.deps            :refer [deps]]
    [tailrecursion.boot.tmpregistry     :refer [mk! mkdir!]]
    [clojure.stacktrace                 :refer [print-cause-trace]]
    [clojure.pprint                     :refer [pprint]]
    [clojure.java.io                    :refer [file delete-file make-parents]]
    [clojure.set                        :refer [difference intersection union]]))

(defmacro print-time [msg ok fail expr]
  `(let [start# (System/currentTimeMillis)]
     (try
       (printf ~ok (do ~expr (double (/ (- (System/currentTimeMillis) start#) 1000))))
       (catch Throwable e#
         (printf ~fail (with-out-str (print-cause-trace e#)))))))

;; Task builders ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pass-thru-wrap [f]
  (fn [continue & args]
    (fn [event]
      (apply f args)
      (continue event))))

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

(def ^:task identity-task pre-task)

;; Task that just does configuration of boot env ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:task config-task [boot configure]
  (pre-task boot :configure configure))

;; Print the event ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn ^:task debug [boot]
  (fn [continue]
    (fn [event]
      (pprint event)
      (flush)
      (continue event))))

;; Loop every msec ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:task auto [boot & [msec]]
  (if-not (locking boot
            (when-not (:auto @boot)
              (swap! boot assoc :auto true)))
    (identity-task boot)
    (fn [continue]
      (fn [event]
        (continue event)
        (Thread/sleep (or msec 200)) 
        (recur (make-event event))))))

;; Process event if watched directories have modified files in them ;;;;;;;;;;;

(defn ^:task watch [boot & {:keys [type msec]}]
  (let [dirs      (:directories @boot)
        watchers  (map f/make-watcher dirs)]
    (comp
      (auto boot msec)
      (fn [continue]
        (fn [event]
          (let [info (reduce (partial merge-with union) (map #(%) watchers))]
            (when-let [mods (seq (info (or type :time)))] 
              (let [path  (f/path (first mods))
                    xtr   (when-let [c (next mods)]
                            (format " and %d others" (count c)))
                    msg   (format "Building %s%s..." path (str xtr))
                    ok    "done. (%.3f sec)\n"
                    fail  "dang!\n%s\n"]
                (print-time msg ok fail (continue (assoc event :watch info)))))))))))

;; Sync (copy) files between directories ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:task sync [boot dst srcs]
  #((pass-thru-wrap f/sync) % (file dst) (map file srcs)))

;; Compile ClojureScript ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:task cljs [boot output-to & [opts]]
  (let [base-opts   {:output-dir    nil
                     :optimizations :whitespace
                     :warnings      true
                     :externs       []
                     :libs          []
                     :foreign-libs  []
                     :pretty-print  true}
        src-paths   (:directories @boot)
        tmp         (get-in @boot [:system :tmpregistry])
        depjars     (deps boot)
        output-dir  (mkdir! tmp ::output-dir)
        flib-out    (mkdir! tmp ::flib-out)
        lib-out     (mkdir! tmp ::lib-out)
        ext-out     (mkdir! tmp ::ext-out)
        inc-out     (mkdir! tmp ::inc-out)
        x-opts      (->> {:output-to  (f/path output-to)
                          :output-dir output-dir}
                      (merge base-opts opts))]
    #((pass-thru-wrap compile-cljs) % src-paths depjars flib-out lib-out ext-out inc-out x-opts)))

;; Build jar files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ^:task jar [boot output-dir & {:keys [main manifest]}]
  (.mkdirs (io/file output-dir))
  (let [{:keys [project version repositories dependencies directories]} @boot
        tmp       (get-in @boot [:system :tmpregistry])
        tmp-dir   (mkdir! tmp ::jar-tmp-dir)
        pom-xml   (make-pom project version repositories dependencies directories)]
    #((pass-thru-wrap create-jar!) % project version directories output-dir tmp-dir :main main :manifest manifest :pom pom-xml)))
