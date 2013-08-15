(ns tutorial-client.rendering
  (:require [domina :as dom]
            [io.pedestal.app.render.push :as render]
            ;; Note that this is shadowed by the templates var below.
            ;; Relies on the different namespaces for functions and
            ;; vars.
            [io.pedestal.app.render.push.templates :as templates]
            [io.pedestal.app.render.push.handlers.automatic :as d]
            [io.pedestal.app.render.push.handlers :as h])
  (:require-macros [tutorial-client.html-templates :as html-templates]))

(def templates (html-templates/tutorial-client-templates))

(defn render-template [template-name initial-value-fn]
  (fn [renderer [_ path :as delta] input-queue]
    (let [parent (render/get-parent-id renderer path)
          id (render/new-id! renderer path)
          html (templates/add-template renderer path (template-name templates))]
      (comment ;; Debugging attempts
        (.log js/console (format "html: %s" html))
        ;; It appears that at this point, the renderer does not even
        ;; know about other-counters.  The string does not appear in the
        ;; following console output.
        (.log js/console (format "renderer: %s" (render/get-data renderer [])))
        ;; It's in the template, though.  It shows up here.
        (.log js/console (format "template: %s" (template-name templates))))
      ;; This is just a shim around the javascript which updates the
      ;; dom.|
      (dom/append! (dom/by-id parent) (html (assoc (initial-value-fn delta) :id id))))))


(defn render-value [renderer [_ path _ new-value] input-queue]
  (let [key (last path)]
    (templates/update-t renderer [:main] {key (str new-value)})))

(defn render-other-counters-element [renderer [_ path] _]
  (.log js/console (format "other-counters-element: %s" (render/new-id! renderer path "other-counters")))  
  (render/new-id! renderer path "other-counters"))

(defn render-other-counter-value [renderer [_ path _ new-value] input-queue]
  (let [key (last path)]
    (templates/update-t renderer path {:count (str new-value)})))

(defn log-add-send-on-click [dom-content]
  (let [h (h/add-send-on-click dom-content)]
    (fn [r msgs q]
      (.log js/console (format "click-message: %s" msgs))
      (h r msgs q))))

(defn render-config []
  ;; Why can't I send a more extensive map to render-template so that
  ;; other-counters has a default rendering, too?.  In
  ;; tutorial-client.html, the template, other-counters has no
  ;; 'field="content:..."' attribute associated with it.  Those are
  ;; inside its div.  That has to be the reason.
  [[:node-create [:main] (render-template :tutorial-client-page
                                          (constantly {:my-counter "0"}))]
   [:node-destroy [:main] h/default-destroy]
   ;; This causes [:main :my-counter] to be sent when 
   [:transform-enable [:main :my-counter] (log-add-send-on-click "inc-button")]
   [:transform-disable [:main :my-counter] (h/remove-send-on-click "inc-button")]
   [:value [:main :*] render-value]
   [:value [:pedestal :debug :*] render-value]

   ;; Because publish-counter sends a ":swap" message, with topic
   ;; [:other-counters `counter-id] (I read this out of the js/console
   ;; log), :other-counters needs to be created.
   [:node-create [:main :other-counters] render-other-counters-element]
   ;; The path to the new counter needs to be created because it's
   ;; part of the message topic, too.  
   [:node-create [:main :other-counters :*]
    ;; Note that this is the reference to the other template,
    ;; other-counter.  I think it's the only one in the code.
    (render-template :other-counter
                     ;; The counter id is the last part of the sse
                     ;; message topic.
                     (fn [[_ path]] {:counter-id (last path)}))]
   [:node-destroy [:main :other-counters :*] h/default-destroy]
   [:value [:main :other-counters :*] render-other-counter-value]])
