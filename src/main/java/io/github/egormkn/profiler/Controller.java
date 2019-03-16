package io.github.egormkn.profiler;

import io.github.egormkn.profiler.services.JarLoaderService;
import io.github.egormkn.profiler.services.JarProfilerService;
import io.github.egormkn.profiler.statistics.MethodLogger;
import io.github.egormkn.profiler.util.Trie;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.aspectj.lang.Signature;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class Controller implements Initializable {

    private static final Image packageIcon = loadImage("/icons/package.png");
    private static final Image classIcon = loadImage("/icons/class.png");
    private static final Image progressIcon = loadImage("/icons/progress.png");
    private static final Image doneIcon = loadImage("/icons/done.png");

    @FXML
    private Node root;

    @FXML
    private MenuBar menu;

    @FXML
    private Label placeholder;

    @FXML
    private ProgressIndicator progress;

    @FXML
    private Pane loader;

    @FXML
    private Pane profiler;

    @FXML
    private Label mainClass;

    @FXML
    private TextField args;

    @FXML
    private TreeView<String> packageTree;

    @FXML
    private TreeView<MethodInfo> methodTree;

    private JarLoaderService loaderService = new JarLoaderService();

    private JarProfilerService profilerService = new JarProfilerService();

    private static CheckBoxTreeItem<String> getPackageTree(Trie<String> rootNode) {
        ImageView icon = new ImageView(rootNode.isLeaf() ? classIcon : packageIcon);
        CheckBoxTreeItem<String> root = new CheckBoxTreeItem<>(rootNode.getContent(), icon);
        rootNode.getChildren().forEach((content, childNode) -> {
            CheckBoxTreeItem<String> child = getPackageTree(childNode);
            root.getChildren().add(child);
        });
        return root;
    }

    private static Image loadImage(String url) {
        return new Image(JarLoaderService.class.getResourceAsStream(url));
    }

    private Set<String> getSelectedPackages(TreeItem<String> parent, String prefix) {
        CheckBoxTreeItem<String> checkBoxParent = (CheckBoxTreeItem<String>) parent;
        if (parent != packageTree.getRoot()) {
            if (!prefix.isEmpty()) prefix += '.';
            prefix += checkBoxParent.getValue();
        }
        Set<String> selected = new HashSet<>(4);
        if (checkBoxParent.isSelected() && checkBoxParent.getChildren().isEmpty()) {
            selected.add(prefix.replace(".class", "").replaceAll("\\$", "."));
        } else {
            for (TreeItem<String> child : checkBoxParent.getChildren()) {
                CheckBoxTreeItem<String> checkBoxChild = (CheckBoxTreeItem<String>) child;
                selected.addAll(getSelectedPackages(checkBoxChild, prefix));
            }
        }
        return selected;
    }

    @FXML
    private void open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open JAR File");
        fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("JAR Files", "*.jar"),
                new ExtensionFilter("All Files", "*.*")
        );
        Stage stage = (Stage) root.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            loaderService.setFile(selectedFile);
            loaderService.start();
        }
    }

    @FXML
    private void save() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save data");
        fileChooser.setInitialFileName("report.txt");
        Stage stage = (Stage) root.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            System.out.println("WRITE DATA");
        }
    }

    @FXML
    private void exit() {
        loaderService.cancel();
        profilerService.cancel();
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void about() throws IOException {
        Stage stage = (Stage) root.getScene().getWindow();
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(stage);
        URL layout = Main.class.getResource("/fxml/about.fxml");
        Parent root = FXMLLoader.load(layout);
        Scene scene = new Scene(root, 300, 200);
        dialog.setScene(scene);
        dialog.setTitle("About");
        dialog.show();
    }

    @FXML
    private void run() {
        Set<String> packages = getSelectedPackages(packageTree.getRoot(), "");
        MethodLogger.getInstance().setSelection(packages);
        profilerService.setPackages(FXCollections.observableSet(packages)); // FIXME
        profilerService.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MethodLogger.getInstance().attachController(this);
        packageTree.setCellFactory(CheckBoxTreeCell.forTreeView());
        TreeItem<MethodInfo> methodRoot = new TreeItem<>(MethodInfo.emptyMethodInfo);
        methodRoot.setExpanded(true);
        methodTree.setRoot(methodRoot);
        mainClass.textProperty().bind(profilerService.mainClassProperty());
        profilerService.fileProperty().bind(loaderService.fileProperty());
        profilerService.argsProperty().bind(args.textProperty());
        loaderService.setOnRunning(this::showProgress);
        loaderService.setOnCancelled(this::showPlaceholder);
        loaderService.setOnFailed(this::showPlaceholder);
        loaderService.setOnSucceeded(event -> {
            JarLoaderService.Result result = loaderService.getValue();
            String mainClass = result.getMainClass();
            Trie<String> packages = result.getPackages();

            CheckBoxTreeItem<String> root = getPackageTree(packages);
            root.setExpanded(true);
            packageTree.setRoot(root);

            showLoader(event);

            profilerService.setMainClass(mainClass);
        });
        profilerService.setOnRunning(this::showProfiler);
        profilerService.setOnSucceeded(event -> {
            System.out.println("Done profiling");
        });
    }

    private void showPlaceholder(Event e) {
        placeholder.setVisible(true);
        progress.setVisible(false);
        loader.setVisible(false);
        profiler.setVisible(false);
    }

    private void showProgress(Event e) {
        placeholder.setVisible(false);
        progress.setVisible(true);
        loader.setVisible(false);
        profiler.setVisible(false);
    }

    private void showLoader(Event e) {
        placeholder.setVisible(false);
        progress.setVisible(false);
        loader.setVisible(true);
        profiler.setVisible(false);
    }

    private void showProfiler(Event e) {
        placeholder.setVisible(false);
        progress.setVisible(false);
        loader.setVisible(false);
        profiler.setVisible(true);
    }

    public void startMethod(long thread, int level, Signature signature, long time) {
        // System.out.format("[Thread: %d]%s%s (%d)%n", thread, new String(new char[level]).replace('\0', ' '), signature, time);
        TreeItem<MethodInfo> root = methodTree.getRoot();
        while (level > 0) {
            List<TreeItem<MethodInfo>> children = root.getChildren();
            root = children.get(children.size() - 1);
            level--;
        }
        TreeItem<MethodInfo> child = new TreeItem<>(new MethodInfo(thread, signature, time), new ImageView(progressIcon));
        child.setExpanded(true);
        root.getChildren().add(child);
    }

    public void endMethod(long thread, int level, Signature signature, long time) {
        // System.out.format("[Thread: %d]%s%s (%d)%n", thread, new String(new char[level]).replace('\0', ' '), signature, time);
        TreeItem<MethodInfo> root = methodTree.getRoot();
        while (level >= 0) {
            List<TreeItem<MethodInfo>> children = root.getChildren();
            root = children.get(children.size() - 1);
            level--;
        }
        if (root.getValue().getSignature().equals(signature)) {
            root.setGraphic(new ImageView(doneIcon));
            root.setValue(new MethodInfo(root.getValue(), time));
        } else {
            System.err.format("Expected: %s, got %s", signature.toLongString(), root.getValue());
        }
    }

    private static class MethodInfo {

        public static final MethodInfo emptyMethodInfo = new MethodInfo(-1, null, 0);
        private final long thread;
        private final Signature signature;
        private final long startTime, endTime;

        public MethodInfo(long thread, Signature signature, long startTime) {
            this.thread = thread;
            this.signature = signature;
            this.startTime = startTime;
            this.endTime = 0;
        }

        public MethodInfo(MethodInfo other, long endTime) {
            this.thread = other.thread;
            this.signature = other.signature;
            this.startTime = other.startTime;
            this.endTime = endTime;
        }

        public long getThread() {
            return thread;
        }

        public Signature getSignature() {
            return signature;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        @Override
        public String toString() {
            if (this == MethodInfo.emptyMethodInfo) return "Method call tree:";
            return endTime > 0
                    ? String.format("[Thread %d] %s (%2f ms)", thread, signature.toLongString(), (endTime - startTime) * 1e-6)
                    : String.format("[Thread %d] %s", thread, signature.toLongString());
        }
    }
}
