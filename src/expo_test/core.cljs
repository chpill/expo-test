(ns expo-test.core
    (:require-macros [rum.core :refer [defc]])
    (:require [re-natal.support :as support]
              [rum.core :as rum]
              [cljs-exponent.components :refer [text view image touchable-highlight] :as rn]))

(def react-navigation (js/require "react-navigation"))

(def addNavigationHelpers (.-addNavigationHelpers react-navigation))


(def tab-navigator (.-TabNavigator react-navigation))
(def StackNavigator (.-StackNavigator react-navigation))

(def React (js/require "react"))

;; https://github.com/re-native/re-native-navigation
(defn make-stack-navigator
  ([routes] (StackNavigator (clj->js routes)))
  ([routes stack-config]
   (StackNavigator (clj->js routes)
                   (clj->js stack-config))))


(defc home-screen [props-navigation]
  (let [navigate (.-navigate props-navigation)]
    (view
     {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
     #_(image {:source logo-img
             :resizeMode "contain"
             :style {:width 350 :height 75}})
     (text {:style {:fontSize 45 :fontWeight "100" :marginBottom 20 :textAlign "center"}}
           "Home screen")
     (touchable-highlight {:onPress #(navigate "Chat")
                           :style   {:backgroundColor "#999" :padding 10 :borderRadius 5}}
                          (text {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
                                "Open the chat")))))

(defc chat-screen [props-navigation]
  (let [go-back (.-goBack props-navigation)]
    (view
     {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
     (text {:style {:fontSize 45 :fontWeight "100" :marginBottom 20 :textAlign "center"}}
           "Chat screen")
     (touchable-highlight {:onPress #(go-back)
                           :style   {:backgroundColor "#999" :padding 10 :borderRadius 5}}
                          (text {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
                                "go back")))))


(defn wrap-with-class [rum-component {:keys [title]}]
  (let [class (React.createClass
               #js {:render (fn []
                              (this-as plop
                                (println "wrapped class instance props.navigation"
                                         (.-navigation (.-props plop)))
                                (rum-component (.-navigation (.-props plop)))))})
        _ (aset class "navigationOptions" #js {:title title})]
    class))

(def routes
  {:Home {:screen (wrap-with-class home-screen {:title "Home"})}
   :Chat {:screen (wrap-with-class chat-screen {:title "Chat"})}})

(def app-navigator (make-stack-navigator routes))

(def initial-navigation-state
  (app-navigator.router.getStateForAction
   (app-navigator.router.getActionForPathAndParams "Home")))


(def global-mutable-state (atom {:nav initial-navigation-state}))

(defn handle-navigation [])

;; Inspired by https://reactnavigation.org/docs/guides/redux
(defc app
  < rum/reactive
  []
  (println "rendering app")
  (let [nav-state (:nav (rum/react global-mutable-state))]
    (React.createElement
    app-navigator
    ;; props
    #js {:navigation (addNavigationHelpers
                      #js {:dispatch (fn [action]
                                       (println "app-navigator dispatch!" action)
                                       (let [nextState (app-navigator.router.getStateForAction
                                                        action
                                                        (:nav @global-mutable-state))]
                                         (println "state for action?" nextState)
                                         (reset! global-mutable-state
                                                 {:nav nextState}))
                                       ;; Some kind of dispatch here!
                                       )
                           ;; TODO make this less static
                           :state nav-state})})))

(defonce root-component-factory (support/make-root-component-factory))

(defn mount-app [] (support/mount (app)))

(defn init []
  (mount-app)
  (.registerComponent rn/app-registry "main" (fn [] root-component-factory)))
