(ns ote.integration.export.geojson
  "Integration service that serves GeoJSON documents for published
  transport services."
  (:require [ote.components.http :as http]
            [ote.components.service :refer [define-service-component]]
            [compojure.core :refer [GET]]
            [specql.core :as specql]
            [ote.db.transport-operator :as t-operator]
            [ote.db.transport-service :as t-service]
            [jeesql.core :refer [defqueries]]
            [cheshire.core :as cheshire]
            [ote.db.modification :as modification]
            [clojure.set :as set]
            [ote.integration.export.transform :as transform]
            [ote.netex.netex_util :as netex-util]
            [ote.util.feature :as feature]
            [ote.time]                                      ; Require time which extends PGInterval JSON generation
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [specql.op :as op]))

(defqueries "ote/integration/export/geojson.sql")

(declare export-geojson)

(defn- feature-collection [geometry properties]
  {:type "FeatureCollection"
   :features [{:type "Feature"
               :properties properties
               :geometry geometry}]})

(def ^{:doc "Transport operator columns to set as properties in GeoJSON export"}
  transport-operator-properties-columns
  #{::t-operator/name ::t-operator/business-id ::t-operator/homepage ::t-operator/visiting-address
    ::t-operator/phone ::t-operator/gsm ::t-operator/email})

(def ^{:doc "Transport service columns to set as properties in GeoJSON export"}
  transport-service-properties-columns
  (set/difference (conj (specql/columns ::t-service/transport-service)
                        ;; Fetch linked external interfaces
                        [::t-service/external-interfaces #{::t-service/id
                                                           ::t-service/format
                                                           ::t-service/license
                                                           ::t-service/data-content
                                                           ::t-service/external-interface}])
                  modification/modification-field-keys
                  #{::t-service/notice-external-interfaces?
                    ::t-service/company-csv-filename
                    ::t-service/company-source
                    ::t-service/ckan-resource-id
                    ::t-service/ckan-dataset-id}))

(defn- styled-operation-area [areas]
  {:type "GeometryCollection"
   :geometries (mapv
                 (fn [{:keys [geojson primary?]}]
                   (assoc (cheshire/decode geojson keyword)
                     :style {:fill (if primary? "green" "orange")}))
                 areas)})

(defn- append-nap-generated-netex-file-links
  "Returns `service` collection where NAP NeTEx file download link is appended to interfaces collection."
  [service db config]
  (if (feature/feature-enabled? config :netex-conversion-automated)
    (when service
      (-> (vector service)
          (netex-util/append-ote-netex-urls config
                                            db
                                            ::t-service/external-interfaces)
          first))
    service))

(defn export-geojson [db config transport-operator-id transport-service-id]
  (let [areas (seq (fetch-operation-area-for-service db {:transport-service-id transport-service-id}))
        operator (when areas
                   (-> (first
                         (specql/fetch db
                                       ::t-operator/transport-operator
                                       transport-operator-properties-columns
                                       {::t-operator/id transport-operator-id}))
                       ;; Personal data should not be exported due to privacy requirement
                       (dissoc ::t-operator/gsm ::t-operator/visiting-address ::t-operator/email ::t-operator/phone)))
        service (when operator
                  (-> (first
                        (specql/fetch db
                                      ::t-service/transport-service
                                      transport-service-properties-columns
                                      {::t-service/transport-operator-id transport-operator-id
                                       ::t-service/id transport-service-id
                                       ::t-service/published op/not-null?}))
                      (append-nap-generated-netex-file-links db config)
                      ;; Company contact details removed because of privacy requirement
                      (dissoc ::t-service/contact-address
                              ::t-service/contact-email
                              ::t-service/contact-phone
                              ::t-service/id)))]
    (if (and areas operator service)
      (-> areas
          styled-operation-area
          (feature-collection (transform/transform-deep
                                {:transport-operator operator
                                 :transport-service service}))
          (cheshire/encode {:key-fn name})
          http/geojson-response)
      {:status 404
       :body "GeoJSON for service not found."})))

(defn- keys-of [keys-spec]
  (let [spec (into {} (map vec) (partition 2 (rest keys-spec)))]
    (concat (:req spec) (:opt spec))))

(defn spec->json-schema [spec]
  (let [first-elt (when (seq? spec)
                    (first spec))
        kw (cond
             (keyword? spec) spec
             (= 'and first-elt) (second spec))]

    (cond

      (= ::s/unknown spec)
      {}

      ;; Array of things
      (= first-elt 'coll-of)
      {:type "array"
       :items (spec->json-schema (second spec))}

      ;; Nilable value (nil or matches spec)
      (= first-elt 'nilable)
      {:anyOf [{:type "null"}
               (spec->json-schema (second spec))]}

      ;; A nested object
      (= first-elt 'keys)
      {:type "object"
       :properties
       (into {}
             (for [k (keys-of spec)]
               [(name k) (spec->json-schema k)]))}

      ;; Set of allowed values
      (set? spec)
      {:enum (mapv name spec)}

      ;; Some primitive data type
      kw
      (case kw
        (:specql.data-types/varchar
          :specql.data-types/bpchar
          :specql.data-types/text
          :specql.data-types/date
          ::t-service/maximum-stay)
        {:type "string"}

        :specql.data-types/bool
        {:type "boolean"}

        (:specql.data-types/numeric :specql.data-types/int4)
        {:type "number"}

        :specql.data-types/geometry
        {:type "object"}

        :specql.data-types/time
        {:type "object"
         :properties {"hours" {:type "number"}
                      "minutes" {:type "number"}
                      "seconds" {:type "number"}}}
        :specql.data-types/interval
        {:type "object"
         :properties {"years" {:type "number"}
                      "months" {:type "number"}
                      "days" {:type "number"}
                      "hours" {:type "number"}
                      "minutes" {:type "number"}
                      "seconds" {:type "number"}}}

        ;; Default: Unrecognized, describe it and recurse
        (spec->json-schema (s/describe spec)))

      :else
      (do
        (log/debug "I don't know what this spec is: " (pr-str spec))
        {}))))

(def transport-service-schema
  {:type "object"
   :properties
   (into {}
         (for [c transport-service-properties-columns]
           (if (vector? c)
             [(name (first c))
              {:anyOf [{:type "null"}
                       {:type "array"
                        :item {:type "object"
                               :properties
                               (into {}
                                     (for [c (second c)]
                                       [(name c) (spec->json-schema (s/describe c))]))}}]}]
             [(name c) (spec->json-schema (s/describe c))])))})

(def transport-operator-schema
  {:type "object"
   :properties
   (into {}
         (for [c transport-operator-properties-columns]
           [(name c) (spec->json-schema (s/describe c))]))})

(defn export-geojson-schema []
  {:$schema "http://json-schema.org/draft-07/schema#"
   :type "object"
   :properties
   {"type" {:type "string"}
    "features"
    {:type "array"
     :items {:type "object"
             :properties
             {"properties"
              {:type "object"
               :properties {"transport-operator" transport-operator-schema
                            "transport-service" transport-service-schema}}
              "geometry" {:type "object"}}}}}})

(define-service-component GeoJSONExport {:fields [config]}
  ^:unauthenticated
  (GET "/export/geojson/:transport-operator-id{[0-9]+}/:transport-service-id{[0-9]+}"
       [transport-operator-id transport-service-id]
    (export-geojson db
                    config
                    (Long/parseLong transport-operator-id)
                    (Long/parseLong transport-service-id)))

  ^:unauthenticated
  (GET "/export/geojson/transport-service.schema.json"
       []
    (http/json-response (#'export-geojson-schema))))
