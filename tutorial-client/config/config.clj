(ns config
  (:require [net.cgrand.enlive-html :as html]
            [io.pedestal.app-tools.compile :as compile]))

;; The configuration below determines how an applicaiton is built,
;; what is built and what features are available in the application
;; development tool.

(def configs
  ;; One project can host multiple applications. The top-level of the
  ;; config map contains one entry for each appliction.
  {:tutorial-client
   {;; :build contains parameters which are passed to the build
    :build {;; :watch-files contains a list of files to watch for
            ;; changes. Each file had a tag associated with it, in
            ;; this case :html.
            :watch-files (compile/html-files-in "app/templates")
            ;; When an HTML file changes, trigger the compilation of
            ;; any files which use macros to read in templates. This
            ;; will force recompilation of these files and update
            ;; the templates.
            :triggers {:html [#"tutorial_client/rendering.js"]}
            :ignore [#"tutorial_client.simulated.worker.js"
                     #"tutorial_client.worker.js"]}
    ;; General application level configuration
    :application {;; The directory where all generated JavaScript for
                  ;; this application will be written.
                  :generated-javascript "generated-js"
                  ;; The default template to use when creating host
                  ;; pages for each aspect below. Override this in an
                  ;; aspect by adding a :template key.
                  :default-template "application.html"
                  ;; The root directory in which to put build
                  ;; output. Possible values are :public and
                  ;; :tools-public. Override this value in an aspect
                  ;; with :tools-output. :public maps to out/public
                  ;; and and :tools-public maps to tools/out/public.
                  :output-root :public
                  :api-server {:host "localhost" :port 8080 :log-fn nil}}
    ;; Add arbitrary links to the control panel
    :control-panel {:design {:uri "/design.html"
                             :name "Design"
                             ;; The order that this item will appear
                             ;; in the context menu.
                             :order 0}}
    ;; Enable built-in features of the application development
    ;; tool. In the example below we enable the rendering view.
    :built-in {:render {;; The directory where rendering scripts
                        ;; are stored
                        :dir "tutorial-client"
                        ;; The namespace which contains the renderer
                        ;; to use. This namespace must have a
                        ;; `render-config` function which returns a
                        ;; render configuration.
                        :renderer 'tutorial_client.rendering
                        ;; Enable logging of rendering data when in
                        ;; this view.
                        :logging? true
                        :order 2
                        ;; The render menu uses the tooling.html template
                        :menu-template "tooling.html"}}
    ;; Each aspect provides a unique way to view and interact with
    ;; this application.
    :aspects {;; Add an aspect that uses the data renderer
              :data-ui {;; Provide the name of the host page that will
                        ;; be generated to host this application. This
                        ;; page will be generated from the template
                        ;; application.html
                        :uri "/tutorial-client-data-ui.html"
                        :params "renderer=auto"
                        ;; Provide the name that will appear in the
                        ;; control panel for this aspect.
                        :name "Data UI"
                        :order 1
                        :out-file "tutorial-client-data-ui.js"
                        ;; The namespace which contains the `main`
                        ;; function to call to start the application.
                        :main 'tutorial_client.simulated.start
                        ;; Allow render data recording. Use
                        ;; Alt-Shift-R to start and stop recording.
                        :recording? true
                        ;; Turn on logging
                        :logging? true
                        ;; build output goes to tools/out/public
                        :output-root :tools-public
                        ;; The data-ui aspect uses the tooling.html template
                        :template "tooling.html"}
              :ui {:uri "/tutorial-client-dev-ui.html"
                   :name "UI"
                   :order 2
                   :out-file "tutorial-client-dev-ui.js"
                   :main 'tutorial_client.simulated.start
                   :recording? true
                   :logging? true
                   :output-root :tools-public}
              :worker-ui {:uri "/tutorial-client-dev-worker-ui.html"
                          :name "Worker UI"
                          :order 3
                          :out-file "tutorial-client-dev-worker-ui.js"
                          :main 'tutorial_client.simulated.worker_start
                          :output-root :tools-public
                          :template "application.html"
                          :workers {"sim_worker" #{#"tutorial_client/behavior.js"
                                                   #"tutorial_client/worker/.*"
                                                   #"tutorial_client/simulated/worker.js"
                                                   #"tutorial_client/simulated/services.js"}}}
              :development {:uri "/tutorial-client-dev.html"
                            :name "Development"
                            :out-file "tutorial-client-dev.js"
                            :main 'tutorial_client.start
                            :logging? true
                            :order 4
                            :use-api-server? true}
              :worker-development {:uri "/tutorial-client-worker-dev.html"
                                   :use-api-server? true
                                   :name "Worker Development"
                                   :out-file "tutorial-client-worker-dev.js"
                                   :main 'tutorial_client.worker_start
                                   :order 5
                                   :template "application.html"
                                   :workers {"worker" #{#"tutorial_client/behavior.js"
                                                        #"tutorial_client/worker/.*"
                                                        #"tutorial_client/worker.js"
                                                        #"tutorial_client/services.js"}}}
              :fresh {:uri "/fresh.html"
                      :name "Fresh"
                      :out-file "fresh.js"
                      :main 'io.pedestal.app.net.repl_client
                      :order 6
                      :output-root :tools-public
                      :template "tooling.html"}
              :production {:uri "/tutorial-client.html"
                           :use-api-server? true
                           :name "Production"
                           :optimizations :advanced
                           :out-file "tutorial-client.js"
                           :main 'tutorial_client.worker_start
                           :order 7
                           :compiler-options {:externs ["app/externs/game.js"]}
                           :workers {"worker" #{#"tutorial_client/behavior.js"
                                                #"tutorial_client/worker/.*"
                                                #"tutorial_client/worker.js"
                                                #"tutorial_client/services.js"}}}}}})
