(ns maldive.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [net.cgrand.enlive-html :as html]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [maldive.generic-in-memory-store :as store]))

(def dslidea
  {:generic-entity
   {:type :generic-entity
    :name "Generic entity"
    :target "/generic-entity"
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
              :rows 10}]}
   :memo {:type :memo
          :name "Memo"
          :target "/memo"
          :method "POST"
          :header "Memo"
          :description "Memo for testing put the DSL"
          :fields [{:name "title" ; Otsikkokenttä pitää pystyä määrittämään tyypille (voisi olla myös funktio?)
                    :label "Topic of the memo"
                    :type :text}
                   {:name "content"
                    :label "Memo"
                    :type :textarea
                    :rows 20}]}})

(html/deftemplate listing "listing.html" [type name-selector listing]
  [:.forms [:li html/first-of-type]] (html/clone-for [[type {:keys [name]}] (into [] dslidea)]
                                                     [:li :a] (html/do-> (html/content name)
                                                                         ; DAFUG?
                                                                         (html/set-attr :href (str "?type="  (subs (str type) 1)))))
  [:p.lead] (html/content (if type (str "Documents of type: " (:name (get dslidea (keyword type)))) "Select type"))
  [:a.add-new] (html/set-attr :href (-> dslidea (get (keyword type)) (get :target)))
  [:table [:tr html/last-of-type]] (html/clone-for [doc listing]
                                                   [:td [:a]] (html/do-> (html/content (get doc name-selector))
                                                                         (html/set-attr :href (str (-> dslidea (get (keyword type)) (get :target)) "/" (get doc "id"))))))

(html/deftemplate form-template "form.html" [id {:keys [header description target method]} snippets]
  [:h1] (html/content header)
  [:form] (html/do-> (html/set-attr :action target)
                     (html/set-attr :method method))
  [:form :p.description] (html/content description)
  [:.id] (html/set-attr :value id)
  [:form :.fields] (html/do-> (html/append (html/html-snippet (anti-forgery-field)))
                              (html/append snippets)))

(def snippets
  {:text (html/defsnippet text-input "form-segments.html"
           [:.text]
           [{:keys [name label placeholder]} value]
           [:label] (html/content label)
           [:input] (html/do-> (html/set-attr :name name)
                               (html/set-attr :value value)
                               (html/set-attr :placeholder placeholder)))
   :textarea (html/defsnippet text-area "form-segments.html"
               [:.textarea]
               [{:keys [name label placeholder rows]} value]
               [:label] (html/content label)
               [:textarea] (html/do-> (html/set-attr :name name)
                                      (html/content value)
                                      (html/set-attr :placeholder placeholder)
                                      (html/set-attr :rows (or rows 4))))})

(defn create-fields
  [{:keys [fields]} obj]
  (for [{:keys [type name] :as field} fields :let [val (get obj name)]] ((get snippets type) field (or val ""))))

(defn create-form
  [type backing-struct]
  (let [description (get dslidea type)]
    (form-template (get backing-struct "id") description (create-fields description backing-struct))))

(defroutes app-routes
  (route/resources "/static")
  (GET "/" {qp :query-params} (if-let [t (get qp "type")]
                                (listing t "title" (store/docs-by-type (keyword t)))
                                (listing nil nil nil)))
  (GET "/generic-entity" [] (create-form :generic-entity nil))
  (GET "/generic-entity/:id" [id] (create-form :generic-entity (store/get-entity :generic-entity (read-string id))))
  (POST "/generic-entity" {params :form-params}
        (let [p (dissoc params "__anti-forgery-token")]
            (do (println "Received POST to /message.")
                (println p)
                (store/store :generic-entity p)
                (ring.util.response/redirect "/?type=generic-entity"))))
  (GET "/memo" [] (create-form :memo nil))
  (GET "/memo/:id" [id] (create-form :memo (store/get-entity :memo (read-string id))))
  (POST "/memo" {params :form-params}
        (let [p (dissoc params "__anti-forgery-token")]
            (do (println "Received POST to /memo.")
                (println p)
                (store/store :memo p)
                (ring.util.response/redirect "/?type=memo"))))  
  (route/not-found "Not Found"))

(defn wrap-type-mappings
  [handler]
  (fn [request]
    (if (= (:request-method request) :post)
      (handler (-> request
                   (update-in [:form-params "id"] (fn [id] (if (empty? id) nil (read-string id))))))
      (handler request))))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      (wrap-type-mappings)
      (wrap-params)))
