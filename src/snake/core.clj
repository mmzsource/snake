(ns snake.core
  (:import
   (java.awt Color Dimension)
   (javax.swing JPanel JFrame Timer JOptionPane)
   (java.awt.event ActionListener KeyListener KeyEvent)))

;; --------------------------------------------------------
;; functional model
;; --------------------------------------------------------

;; constants
(def field-width  50) ; grid units
(def field-height 30) ; grid units
(def point-size   15) ; number of pixels of one grid square
(def turn-millis 100) ; snake moves every turn-millis msecs
(def win-length   10) ; length of snake to win the game
(def directions       ; direction in terms of grid units
  {KeyEvent/VK_LEFT  [-1  0]
   KeyEvent/VK_RIGHT [ 1  0]
   KeyEvent/VK_UP    [ 0 -1]
   KeyEvent/VK_DOWN  [ 0  1]})


(defn create-snake []
  {:body      (list [3 0] [2 0] [1 0] [0 0])
   :direction [1 0]
   :type      :snake
   :color     (Color. 15 160 70)})


(defn create-apple []
  {:location [(rand-int field-width) (rand-int field-height)]
   :type     :apple
   :color    (Color. 210 50 90)})


;; translate a grid point (grid coordinate)
;; to a rectangle on the screen (in pixels)
(defn point->screen-rect [[pt-x pt-y]]
  [(* pt-x point-size) (* pt-y point-size) point-size point-size])


(defn move [{:keys [body direction] :as snake} & grow]
  (assoc snake :body
         (cons
          (let [[head-x head-y] (first body)
                [dir-x dir-y]   direction]
            [(+ head-x dir-x) (+ head-y dir-y)])
          (if grow body (butlast body)))))


(defn turn [snake direction]
  (assoc snake :direction direction))


(defn win? [{body :body}]
  (>= (count body) win-length))


(defn head-overlaps-body? [head body]
  (contains? (set body) head))


(defn head-outside-bounds? [[head-x head-y]]
  (or
   (> head-x field-width)
   (< head-x 0)
   (> head-y field-height)
   (< head-y 0)))


(defn lose? [{[head & body] :body}]
  (or
   (head-overlaps-body?  head body)
   (head-outside-bounds? head)))


(defn eats? [{[head] :body} {apple :location}]
  (= head apple))


;; --------------------------------------------------------
;; Mutable model
;; --------------------------------------------------------


(defn update-positions [snake apple]
  (dosync
   (if (eats? @snake @apple)
     (do
       (ref-set apple (create-apple))
       (alter snake move :grow))
     (alter snake move)))
  nil)


(defn update-direction [snake direction]
  (dosync (alter snake turn direction))
  nil)


(defn reset-game [snake apple]
  (dosync
   (ref-set snake (create-snake))
   (ref-set apple (create-apple)))
  nil)


;; --------------------------------------------------------
;; gui
;; --------------------------------------------------------


(defn fill-point [g pt color]
  (let [[x y width height] (point->screen-rect pt)]
    (.setColor g color)
    (.fillRect g x y width height)))


(defmulti paint (fn [g object] (:type object)))


(defmethod paint :apple [g {:keys [location color]}]
  (fill-point g location color))


(defmethod paint :snake [g {:keys [body color]}]
  (doseq [point body]
    (fill-point g point color)))


(defn game-panel [frame snake apple]
  (proxy [JPanel ActionListener KeyListener] []
    ;; JPanel
    (paintComponent [g]
      (proxy-super paintComponent g)
      (paint g @apple)
      (paint g @snake))
    (getPreferredSize []
      (Dimension.
       (* (inc field-width)  point-size)
       (* (inc field-height) point-size)))
    ;; ActionListener
    (actionPerformed [e]
      (update-positions snake apple)
      (if (lose? @snake)
        (do
          (reset-game snake apple)
          (JOptionPane/showMessageDialog frame "You lose!")))
        (if (win? @snake)
          (do
            (reset-game snake apple)
            (JOptionPane/showMessageDialog frame "You win!")))
      (.repaint this))
    ;; KeyListener
    (keyPressed [e]
      (let [direction (directions (.getKeyCode e))]
        (if direction (update-direction snake direction))))
    (keyReleased [e])
    (keyTyped    [e])))


(defn game []
  (let [snake (ref (create-snake))
        apple (ref (create-apple))
        frame (JFrame. "Snake")
        panel (game-panel frame snake apple)
        timer (Timer. turn-millis panel)]

    (.setFocusable panel true)
    (.addKeyListener panel panel)

    (.add frame panel)
    (.pack frame)
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.setVisible frame true)

    (.start timer)))

(defn -main
  [& args]
  (game))
