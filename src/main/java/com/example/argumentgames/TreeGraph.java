package com.example.argumentgames;

import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;

public class TreeGraph {
    private final double moveAwayStep = 10;
    private double dragOriginY = 0, dragOriginX = 0, tCircleRadius = 30;
    private final RadioButton setSelectModeButton, setMoveModeButton, setPanModeButton;
    private final Button buildTreeButton;
    private final ArrayList<TreeCircle> tCircles = new ArrayList<>();
    private final ArrayList<TreeArrow> tArrows = new ArrayList<>();
    private TreeCircle selected = null;
    private final Pane treePane;
    private final ToggleGroup tg = new ToggleGroup();

    private TreeArgument root;

    private EventHandler<MouseEvent> graphWideEvent;
    enum InteractMode {
        SELECT_MODE,
        MOVE_MODE,
        PAN_MODE,
        SPECIAL_MODE
    }
    private TreeGraph.InteractMode interactMode = TreeGraph.InteractMode.SELECT_MODE;

    public TreeGraph(Pane graphPane, RadioButton setSelectModeButton, RadioButton setMoveModeButton,
                     RadioButton setPanModeButton, Button buildTreeButton) {
        // Save variables
        this.treePane = graphPane;
        this.setSelectModeButton = setSelectModeButton;
        this.setMoveModeButton = setMoveModeButton;
        this.setPanModeButton = setPanModeButton;
        this.buildTreeButton = buildTreeButton;
        //
        // Setup interact mode buttons
        setUpInteractModeButton(setMoveModeButton, TreeGraph.InteractMode.MOVE_MODE);
        setUpInteractModeButton(setSelectModeButton, TreeGraph.InteractMode.SELECT_MODE);
        setUpInteractModeButton(setPanModeButton, TreeGraph.InteractMode.PAN_MODE);
        this.setSelectModeButton.fire();
        // Setup other buttons
        this.buildTreeButton.setOnAction(e-> {
            clear();
            root = new TreeArgument("a", null);
            TreeArgument b1 = new TreeArgument("b1", root);
            TreeArgument b2 = new TreeArgument("b2", root);
            TreeArgument b3 = new TreeArgument("b3", root);
            TreeArgument c1 = new TreeArgument("c1", b1);
            TreeArgument c2 = new TreeArgument("c2", b2);
            TreeArgument c3 = new TreeArgument("c3", b2);
            TreeArgument c4 = new TreeArgument("c4", b2);
            buildTree(root);
        });
        setUpClip();
    }

    public TreeArgument getRoot() { return root; }

    // Clears the graph of all tCircles and tArrows
    private void clear() {
        treePane.getChildren().clear();
        if (root!=null) root.clearVisualTCircles();
        tCircles.clear();
        tArrows.clear();
        selected = null;
    }

    // Creates a boundary for the area
    // Only the nodes inside of the area will be visible - anything outside will be cut
    private void setUpClip() {
        final Rectangle outputClip = new Rectangle();
        outputClip.setArcWidth(5);
        outputClip.setArcHeight(5);
        outputClip.setWidth(treePane.getWidth());
        outputClip.setHeight(treePane.getHeight());
        treePane.setClip(outputClip);
        treePane.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });
    }

    // Performs basic setup for buttons that change the interact mode
    // Changes their appearance, sets action, and assigns toggle group
    private void setUpInteractModeButton(RadioButton b, TreeGraph.InteractMode i) {
        b.getStyleClass().remove("radio-button");
        b.setToggleGroup(this.tg);
        b.setOnAction(e -> {
            setInteractMode(i);
        });
    }

    // Changes the interact mode for the tree
    private void setInteractMode(TreeGraph.InteractMode m) {
        switch (m) {
            // SELECT MODE
            // Allows user to click on Nodes to select them
            // Allows user to highlight nodes
            case SELECT_MODE -> {
                treePane.setOnMousePressed(null); treePane.setOnMouseDragged(null);
                interactMode = m;
                setDisableButtons(false);
                // Implement selection
                for (TreeCircle n : tCircles) {
                    n.setMouseTransparent(false);
                    n.setOnMouseDragged(null);
                    n.setOnMousePressed(null);
                    n.setOnMouseClicked(e -> {
                        if (selected == n) {
                            selected.deselect();
                            selected = null;
                        } else {
                            if (selected != null) selected.deselect();
                            selected = n;
                            selected.select();
                        }
                    });
                }
            }
            // MOVE MODE
            // Allows user to drag the nodes around and move them
            case MOVE_MODE -> {
                treePane.setOnMousePressed(null); treePane.setOnMouseDragged(null);
                interactMode = m;
                if (selected != null) {
                    selected.deselect();
                    selected = null;
                }
                // Turn off other events, then make draggable
                for (TreeCircle n : tCircles) {
                    n.setMouseTransparent(false);
                    n.setOnMouseClicked(null);
                    makeDraggable(n);
                }
            }
            // PAN MODE
            // Allows user to drag the mouse to "look around", and scroll to zoom in and out
            case PAN_MODE -> {
                // Make nodes and arrows transparent - only clicks on the Pane matter
                interactMode = m;
                if (selected != null) {
                    selected.deselect();
                    selected = null;
                }
                for (TreeCircle c: tCircles) { c.setMouseTransparent(true); }
                treePane.setOnMousePressed(e -> {
                    if (interactMode == TreeGraph.InteractMode.PAN_MODE) {
                        dragOriginX = e.getSceneX();
                        dragOriginY = e.getSceneY();
                    }
                });
                treePane.setOnMouseDragged(e -> {
                    if (interactMode == TreeGraph.InteractMode.PAN_MODE) {
                        double xDrag = e.getSceneX() - dragOriginX;
                        double yDrag = e.getSceneY() - dragOriginY;
                        dragOriginX = e.getSceneX();
                        dragOriginY = e.getSceneY();
                        for (TreeCircle c: tCircles) { c.translateXY(xDrag, yDrag); }
                    }
                });
            }
            // SPECIAL MODE
            // Turns off all other events and interactions
            // Used in special cases with unique events
            case SPECIAL_MODE -> {
                treePane.setOnMousePressed(null); treePane.setOnMouseDragged(null);
                interactMode = m;
                if (selected != null) {
                    selected.deselect();
                    selected = null;
                }
                setDisableButtons(true);
                // Disable all GCircle/edge click events
                for (TreeCircle n : tCircles) {
                    n.setMouseTransparent(false);
                    n.setOnMouseClicked(null);
                    n.setOnMouseDragged(null);
                    n.setOnMousePressed(null);
                }
                for (TreeArrow a : tArrows) {
                    a.setMouseTransparent(false);
                    a.setOnMouseClicked(null);
                    a.setOnMouseDragged(null);
                    a.setOnMousePressed(null);
                }
            }
        }
    }

    // Adds the events that allow the provided Circle to be dragged around by the user
    private void makeDraggable(TreeCircle node) {
        node.setOnMousePressed(e -> {
            if (interactMode == TreeGraph.InteractMode.MOVE_MODE) {
                node.toFront();
                dragOriginX = e.getSceneX() - node.getLayoutX();
                dragOriginY = e.getSceneY() - node.getLayoutY();
            }
        });
        node.setOnMouseDragged(e -> {
            if (interactMode == TreeGraph.InteractMode.MOVE_MODE) {
                double newX = e.getSceneX() - dragOriginX;
                node.getCenterXProperty().set(newX);
                node.setLayoutX(newX);
                double newY = e.getSceneY() - dragOriginY;
                node.getCenterYProperty().set(newY);
                node.setLayoutY(newY);
                node.rotateArrows();
            }
        });
    }

    // Sets all control buttons to disabled (or enabled, as the parameter)
    private void setDisableButtons(boolean b) {
        setSelectModeButton.setDisable(b);
        setMoveModeButton.setDisable(b);
        setPanModeButton.setDisable(b);
    }

    // Takes the specified bound, assigns totalPositions spots inside it, equal distance from each other
    // Returns the value of the specified position
    // Ex. For bounds [50, 100] and 1 totalPositions, position 1, it will return 75
    private double getEvenDivision(double leftBound, double rightBound, int position, int totalPositions) {
        double length = rightBound - leftBound;
        double distanceBetween = length / (totalPositions + 1);
        return leftBound + (position * distanceBetween);
    }

    // Places the children of the root
    // Places them within the area between xLeft and xRight, at yLevel
    private void buildBranch(TreeArgument root, double yLevel, double xLeft, double xRight) {
        ArrayList<TreeArgument> children = root.getChildren();
        if (children.isEmpty()) return;
        double distance = (xRight - xLeft) / root.getWidth();
        if (children.size() == root.getWidth()) {
            // Special case - all width slots are taken, so treat each node as width 1
            double currX = xLeft;
            for (TreeArgument arg: children) {
                // Create the node
                // NEW CIRCLE
                addTCircle(arg, currX + (distance/2),
                        yLevel, root.getVisualTCircle());
                // If it's a branch, build the subtree
                if (!arg.isLeaf()) buildBranch(arg, yLevel + (tCircleRadius * 3), currX, currX + distance);
                // Increase the currLeftX
                currX += distance;
            }
        } else {
            /* For each child:
                Step 1: Take the first argument. If it's a leaf, save it in a collection and move on
                Step 2: If it's not a leaf, take its width and place it in its "box"
                Step 3: Save the locations of free spaces - leaves will slot into them later
             */
            double currRightX = xLeft, currLeftX = xLeft;
            ArrayList<TreeArgument> leaves = new ArrayList<>();
            ArrayList<Double> emptySpots = new ArrayList<Double>();
            for (TreeArgument arg: children) {
                if (arg.isLeaf()) {leaves.add(arg); }
                else {
                    int width = arg.getWidth();
                    // Create the node
                    currRightX += distance * width;
                    // TotalPositions is the width rounded down to an odd number
                    int totalPositions = width; if (width%2 == 0) totalPositions -= 1;
                    // Position is the middle number among all positions
                    int position = (int) Math.ceil(width/2.0);
                    // NEW CIRCLE
                    addTCircle(arg, getEvenDivision(currLeftX, currRightX, position, totalPositions),
                            yLevel, root.getVisualTCircle());
                    // Build the subtree of this branch
                    buildBranch(arg, yLevel + (tCircleRadius * 3), currLeftX, currRightX);
                    // Save the empty slots
                    for (int i = 1; i <= totalPositions; i++) {
                        if (i != position) emptySpots.add( getEvenDivision(currLeftX, currRightX, i, totalPositions) );
                    }
                    currLeftX += distance*width;
                }
            }
            // We have placed all branches - time for slotting leaves into the saved empty slots
            for (TreeArgument leaf: leaves) {
                double xSlot = emptySpots.remove(0);
                // NEW CIRCLE
                addTCircle(leaf, xSlot,
                        yLevel, root.getVisualTCircle());
            }
        }
    }

    private void addTCircle(TreeArgument arg, double x, double y, TreeCircle parent) {
        TreeCircle newCircle = new TreeCircle(arg, tCircleRadius);
        treePane.getChildren().add(newCircle); tCircles.add(newCircle);
        newCircle.moveToXY(x, y);
        //
        // Create the arrow pointing from self to parent
        if (parent != null) {
            TreeArrow arrow = new TreeArrow(newCircle, parent);
            treePane.getChildren().addAll(arrow, arrow.getArrowTip()); tArrows.add(arrow);
            arrow.toBack(); arrow.getArrowTip().toBack();
        }
        newCircle.toFront();
    }

    public ArrayList<TreeCircle> gettCircles() {return tCircles;}

    public void buildTree(TreeArgument newRoot) {
        clear();
        root = newRoot;
        if (root == null) return;
        root.updateWidth();
        double leftBound = 0, rightBound = Math.max( treePane.getWidth(), (3*tCircleRadius * root.getWidth()) ),
                yLevel = tCircleRadius;
        // Create the root
        TreeCircle rootCircle = new TreeCircle(root, tCircleRadius);
        treePane.getChildren().add(rootCircle); tCircles.add(rootCircle);
        rootCircle.moveToXY(getEvenDivision(leftBound, rightBound, 1, 1), yLevel);
        // Build the branch from the root
        buildBranch(root, yLevel + (tCircleRadius*3), leftBound, rightBound);
    }

    // Changes the appearance of all nodes and arrows to make them invisible
    // Also makes them unclickable
    // Used in Games to signify which nodes are not yet in play
    public void disableAll() {
        for (TreeCircle c : tCircles) {
            c.setMouseTransparent(true);
            c.setVisible(false);
        }
        for (TreeArrow a : tArrows) {
            a.setDisplayVisible(false);
        }
    }

    public void visualEnableAll() {
        for (TreeCircle c : tCircles) {
            c.baseVisual();
            c.makeGameUnselectable();
            c.setMouseTransparent(false);
            c.setVisible(true);
        }
        for (TreeArrow a : tArrows) {
            a.setDisplayVisible(true);
        }
    }

    // Disables the buttons that could change the current Tree
    // Returns the Select mode button - the reference to it is used by the Game Controller
    public void enterGameMode(GameController game) {
        setMoveModeButton.fire();
        setSelectModeButton.setOnAction(e -> {
            treePane.setOnMousePressed(null); treePane.setOnMouseDragged(null);
            interactMode = TreeGraph.InteractMode.SELECT_MODE;
            // Get which circles should be enabled
            for (TreeArgument arg: root.getAllArguments()) {
                TreeCircle c = arg.getVisualTCircle();
                c.setMouseTransparent(false);
                c.setOnMouseDragged(null);
                c.setOnMousePressed(null);
                c.setOnMouseClicked(e2 -> {
                    //
                    //
                    if (c.isGameSelectEnabled()) {
                        c.gameSelected();
                        game.selectArgumentToCounter(c.getName(), arg);
                    }
                    //
                    //
                });
            }
        });
        setSelectModeButton.fire();
    }

    public void exitGameMode() {
        // Restore Select functionality
        setUpInteractModeButton(setSelectModeButton, TreeGraph.InteractMode.SELECT_MODE);
        setSelectModeButton.fire();
    }
}
