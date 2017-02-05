(ns maldive.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))

(def dslidea
  {:message {:target "/message"
             :method "POST"
             :header "Just a bogus datatype"
             :description "This is a bogus datatype for planning put Maldive™ extravaganza on the map."
             :fields [{:name "title"
                       :label "Title of the document"
                       :placeholder "Write here"
                       :type :text}
                      {:name "additional-title"
                       :label "Additional title"
                       :type :text}
                      {:name "description"
                       :label "Description"
                       :placeholder "Description describes"
                       :type :textarea}
                      {:name "long-description"
                       :label "Long description"
                       :placeholder "Long description describes things longer (see :rows)"
                       :type :textarea
                       :rows 10}]}})

(html/deftemplate form-template "form.html" [{:keys [header description target method]} snippets]
  [:h1] (html/content header)
  [:form] (html/do-> (html/set-attr :target target)
                     (html/set-attr :method method))
  [:form :p.description] (html/content description)
  [:form :.fields] (html/do-> (html/append (html/html-snippet (anti-forgery-field)))
                              (html/append snippets)))

(def snippets
  {:text (html/defsnippet text-input "form-segments.html"
           [:.text]
           [{:keys [name label placeholder]}]
           [:label] (html/content label)
           [:input] (html/do-> (html/set-attr :name name)
                               (html/set-attr :placeholder placeholder)))
   :textarea (html/defsnippet text-area "form-segments.html"
               [:.textarea]
               [{:keys [name label placeholder rows]}]
               [:label] (html/content label)
               [:textarea] (html/do-> (html/set-attr :name name)
                                      (html/set-attr :placeholder placeholder)
                                      (html/set-attr :rows (or rows 4))))})

(defn create-fields
  [{:keys [fields]}]
  (for [{:keys [type] :as field} fields] ((get snippets type) field)))

(defn create-form
  [type]
  (let [description (get dslidea type)]
    (form-template description (create-fields description))))

(defroutes app-routes
  (route/resources "/static")
  (GET "/message" [] (create-form :message))
  (POST "/message" request
        (do (println "Received POST to /message. Should persist but now just pass through.")
            (println request)
            (ring.util.response/redirect "/message")))
  (route/not-found "Not Found"))

(def app
  (wrap-defaults app-routes site-defaults))
