(ns tutorial-client.start
  (:require [io.pedestal.app.protocols :as p]
            [io.pedestal.app :as app]
            [io.pedestal.app.render.push :as push-render]
            [io.pedestal.app.render :as render]
            [io.pedestal.app.messages :as msg]
            [tutorial-client.behavior :as behavior]
            [tutorial-client.rendering :as rendering]
            [tutorial-client.post-processing :as post]
            [tutorial-client.services :as services]))

(defn create-app [render-config]
  ;; Adds the post-processors to the dataflow map
  (let [app (app/build (post/add-post-processors behavior/example-app))
        ;; Rendering.  This is a function called by consume-app-model
        ;; with the most recent application-model deltas. See
        ;; render/consume-app-model-queue.  See also
        ;; push-render/renderer, which calls each handler matching a
        ;; delta message with the delta
        render-fn (push-render/renderer "content" render-config render/log-fn)
        ;; A queue which consumes application-model deltas, based on render-config
        app-model (render/consume-app-model app render-fn)]
    (app/begin app)
    {:app app :app-model app-model}))

(defn ^:export main []
  ;; Just returns a map
  (let [app (create-app (rendering/render-config))
        ;; Interface to the SSE connection.  This is the syntax for
        ;; instantiating a defrecord using positional arguments.  See
        ;; http://dev.clojure.org/display/design/defrecord+improvements#defrecordimprovements-Factoryfunctiontakingpositionalvaluesdefrecordanddeftype
        services (services/->Services (:app app))]
    ;; Start up the SSE connection.
    (app/consume-effects (:app app) services/services-fn)
    (p/start services)
    app))

