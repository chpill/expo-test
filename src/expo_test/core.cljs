(ns expo-test.core
    (:require-macros [rum.core :refer [defc]])
    (:require [re-natal.support :as support]
              [rum.core :as rum]
              [cljs-exponent.components :refer [text view image touchable-highlight] :as rn]))

(def react-navigation (js/require "react-navigation"))

(def addNavigationHelpers (.-addNavigationHelpers react-navigation))

(def navigators
  {:stack (.-StackNavigator react-navigation)
   :tab (.-TabNavigator react-navigation)
   :drawer (.-DrawerNavigator react-navigation)})

;; https://github.com/re-native/re-native-navigation
(defn navigator
  ([name routes] ((get navigators name) (clj->js routes)))
  ([name routes navigator-config]
   ((get navigators name) (clj->js routes)
    (clj->js navigator-config))))

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

(defc drawer-screen [props-navigation]
  (let [navigate (.-navigate props-navigation)]
    (view
    {:style {:flexDirection "column" :margin 40 :alignItems "center"}}
    (text {:style {:fontSize 45 :fontWeight "100" :marginBottom 20 :textAlign "center"}}
          "PLOP?")
    (touchable-highlight {:onPress #(navigate "DrawerOpen")
                          :style   {:backgroundColor "#999" :padding 10 :borderRadius 5}}
                         (text {:style {:color "white" :textAlign "center" :fontWeight "bold"}}
                               "Open drawer")))))



(defn wrap-with-class [rum-component {:keys [title]}]
  (let [class (js/React.createClass
               #js {:render (fn []
                              (this-as plop
                                (println "wrapped class instance props.navigation"
                                         (.-navigation (.-props plop)))
                                (rum-component (.-navigation (.-props plop)))))})
        _ (aset class "navigationOptions" #js {:title title})]
    class))

(def drawer-sub-routes
  (navigator :drawer
             {:plop          {:screen (wrap-with-class drawer-screen {:title "plop?"})}
              :notifications {:screen (wrap-with-class drawer-screen {:title "nofigications"})}}))

(def routes
  {:Home {:screen (wrap-with-class home-screen {:title "Home"})}
   :Chat {:screen (navigator :tab
                             {:chat-1 {:screen (wrap-with-class chat-screen {:title "Chat-1"})}
                              :chat-2 {:screen (wrap-with-class chat-screen {:title "Chat-2"})}
                              :chat-3 {:screen (wrap-with-class chat-screen {:title "Chat-3"})}
                              :drawer {:screen drawer-sub-routes}}
                             {:tabBarPosition :bottom})}})

(def app-navigator (navigator :stack
                              routes
                              ;; :float | :screen | :none
                              {:headerMode :float
                               ;;:transitionConfig {}
                               }))

(def initial-navigation-state
  (app-navigator.router.getStateForAction
   (app-navigator.router.getActionForPathAndParams "Home")))


(def global-mutable-state (atom {:nav initial-navigation-state}))

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
