(ns ^:shared tutorial-client.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app.messages :as msg]
              [io.pedestal.app :as app]))

(defn inc-transform [old-value _]
  ((fnil inc 0) old-value))

(defn swap-transform [_ message]
  (:value message))

(defn total-count [_ nums] (apply + nums))

(defn maximum [old-value nums]
  (apply max (or old-value 0) nums))

(defn average-count [_ {:keys [total nums]}]
  (/ total (count nums)))

(defn merge-counters [_ {:keys [me others]}]
  (assoc others "Me" me))

(defn cumulative-average [debug key x]
  (let [k (last key)
        i (inc (or (::avg-count debug) 0))
        avg (or (::avg-raw debug) 0)
        new-avg (+ avg (/ (- x avg) i))]
    (assoc debug
      ::avg-count i
      ::avg-raw new-avg
      (keyword (str (name k) "-avg")) (int new-avg))))

(defn init-main [path]
  (.log js/console (format "init message: %s" path))
  [[:transform-enable [:main :my-counter] :inc [{msg/topic [:my-counter]}]]])

(defn publish-counter [count]
  [{msg/type :swap msg/topic [:other-counters] :value count}])

(defn my-emitter [paths]
  (let [e (app/default-emitter paths)]
    (fn [& args]
      (.log js/console (format "emitter args: %s" args))
      (apply e args))))

(def example-app
  {:version 2
   :debug true
   :transform [[:inc   [:my-counter]   inc-transform]
               [:swap  [:**]           swap-transform]
               [:debug [:pedestal :**] swap-transform]]
   :derive #{[{[:my-counter] :me [:other-counters] :others} [:counters] merge-counters :map]
             [#{[:counters :*]} [:total-count] total-count :vals]
             [#{[:counters :*]} [:max-count] maximum :vals]
             [{[:counters :*] :nums [:total-count] :total} [:average-count] average-count :map]
             [#{[:pedestal :debug :dataflow-time]} [:pedestal :debug :dataflow-time-max] maximum :vals]
             [#{[:pedestal :debug :dataflow-time]} [:pedestal :debug] cumulative-average :map-seq]}
   ;; When the local counter is incremented, this sends a message back
   ;; to the server, where the result is published as a member of
   ;; other-counters.  How does the local queue know not to consume
   ;; that?  Is it not fed into the local queue, back from the server?
   ;; That has to be it.  Otherwise, an entry for it would show up
   ;; locally in other counters as soon as it was incremented.
   :effect #{[#{[:my-counter]} publish-counter :single-val]}
   ;; init-main is important because rendering/render-config specifies
   ;; that the click-to-increment functionality should be turned
   ;; on/off when a transform-(en/dis)able message is received.
   :emit [{:init init-main}
          [#{[:my-counter]
             [:other-counters :*]
             [:total-count]
             [:max-count]
             [:average-count]} (my-emitter [:main])]
          [#{[:pedestal :debug :dataflow-time]
             [:pedestal :debug :dataflow-time-max]
             [:pedestal :debug :dataflow-time-avg]} (my-emitter [])]
          [#{[:**]} (my-emitter [])]]})

