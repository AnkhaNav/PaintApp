package com.example.paintapp;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.util.Stack;
import java.awt.image.BufferedImage;
import java.io.File;

public class Controller {

    @FXML
    private Canvas drawingCanvas;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private Slider brushSizeSlider;

    @FXML
    private Label brushSizeLabel;

    @FXML
    private Slider pixelSizeSlider;

    @FXML
    private Label pixelSizeLabel;

    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();
    private GraphicsContext gc;
    private double scaleFactor = 1.0;
    private final double SCALE_STEP = 0.1;
    private final double MIN_SCALE = 0.5;
    private final double MAX_SCALE = 3.0;


    @FXML
    public void initialize() {
        // Nastavení grafického kontextu plátna
        gc = drawingCanvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        colorPicker.setValue(Color.BLACK);

        // Události myši
        drawingCanvas.setOnMousePressed(this::startDrawing);
        drawingCanvas.setOnMouseDragged(this::draw);

        // Přidání posluchače pro změnu hodnoty slideru
        pixelSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            pixelSizeLabel.setText("Pixel Size: " + newValue.intValue());
        });
        brushSizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            brushSizeLabel.setText("Brush Size: " + newValue.intValue());
        });
        updateCanvasScale();
    }

    private void updateCanvasScale() {
        drawingCanvas.setScaleX(scaleFactor);
        drawingCanvas.setScaleY(scaleFactor);
    }

    @FXML
    private void zoomIn() {
        if (scaleFactor < MAX_SCALE) {
            scaleFactor += SCALE_STEP;
            updateCanvasScale();
        }
    }

    @FXML
    private void zoomOut() {
        if (scaleFactor > MIN_SCALE) {
            scaleFactor -= SCALE_STEP;
            updateCanvasScale();
        }
    }


    private void saveState() {
        Image snapshot = drawingCanvas.snapshot(null, null);
        undoStack.push(snapshot);
        // Omezíme velikost zásobníku na 5 položek
        if (undoStack.size() > 5) {
            undoStack.remove(0); // Odstraníme nejstarší stav
        }
        redoStack.clear(); // Vymazání redo zásobníku po nové akci
    }

    @FXML
    private void undo() {
        if (!undoStack.isEmpty()) {
            Image lastState = undoStack.pop();
            redoStack.push(drawingCanvas.snapshot(null, null)); // Uložení aktuálního stavu do redo
            gc.drawImage(lastState, 0, 0);
        }
    }

    @FXML
    private void redo() {
        if (!redoStack.isEmpty()) {
            Image redoState = redoStack.pop();
            undoStack.push(drawingCanvas.snapshot(null, null)); // Uložení aktuálního stavu do undo
            gc.drawImage(redoState, 0, 0);
        }
    }

    private void startDrawing(MouseEvent e) {
        saveState(); // Uložení aktuálního stavu
        gc.setStroke(colorPicker.getValue());
        gc.setLineWidth(brushSizeSlider.getValue());
        gc.beginPath();
        gc.moveTo(e.getX(), e.getY());
        gc.stroke();
    }

    private void draw(MouseEvent e) {
        double brushSize = brushSizeSlider.getValue();
        gc.setFill(colorPicker.getValue());
        gc.fillOval(e.getX() - brushSize / 2, e.getY() - brushSize / 2, brushSize, brushSize);
    }

    @FXML
    private void clearCanvas() {
        saveState(); // Uložení aktuálního stavu
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
    }
    private Image originalImage;

    @FXML
    private void loadImage() {
        saveState();
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            try {
                Image image = new Image(file.toURI().toString());
                double canvasWidth = drawingCanvas.getWidth();
                double canvasHeight = drawingCanvas.getHeight();

                // Výpočet nových rozměrů při zachování poměru stran
                double aspectRatio = image.getWidth() / image.getHeight();
                double newWidth, newHeight;

                if (aspectRatio >= 1) { // Obrázek je širší než vyšší
                    newWidth = canvasWidth;
                    newHeight = canvasWidth / aspectRatio;
                } else { // Obrázek je vyšší než širší
                    newHeight = canvasHeight;
                    newWidth = canvasHeight * aspectRatio;
                }

                // Vymazání plátna a překreslení obrázku
                gc.setFill(Color.WHITE);
                gc.fillRect(0, 0, canvasWidth, canvasHeight);
                gc.drawImage(image, (canvasWidth - newWidth) / 2, (canvasHeight - newHeight) / 2, newWidth, newHeight);

                // Uložení obrázku pro pozdější použití
                originalImage = image;
            } catch (Exception e) {
                // Pokud dojde k chybě, informujeme uživatele
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Load Error");
                alert.setHeaderText("Failed to load image");
                alert.setContentText("An error occurred while trying to load the image:\n" + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private void saveImageToFile(WritableImage image, File file, String format) throws Exception {
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                javafx.scene.paint.Color fxColor = reader.getColor(x, y);
                java.awt.Color awtColor = new java.awt.Color(
                        (float) fxColor.getRed(),
                        (float) fxColor.getGreen(),
                        (float) fxColor.getBlue(),
                        (float) fxColor.getOpacity()
                );
                bufferedImage.setRGB(x, y, awtColor.getRGB());
            }
        }
        try {
            ImageIO.write(bufferedImage, format, file);
        } catch (Exception e) {
            // Pokud nastane chyba při zápisu, zobrazíme chybu
            throw new Exception("Failed to save image: " + e.getMessage());
        }
    }

    @FXML
    private void exportCanvas() {
        WritableImage image = new WritableImage((int) drawingCanvas.getWidth(), (int) drawingCanvas.getHeight());
        drawingCanvas.snapshot(null, image); // Vytvoření snapshotu canvasu

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        fileChooser.setInitialFileName("drawing.png"); // Výchozí název souboru

        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                String fileName = file.getName().toLowerCase();
                String format = fileName.endsWith(".jpg") ? "jpg" : "png"; // Určí formát souboru
                saveImageToFile(image, file, format);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Error");
                alert.setHeaderText("Could not export canvas");
                alert.setContentText("An error occurred while trying to save the image:\n" + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void restoreOriginalImage() {
        saveState();
        if (originalImage != null) {
            double canvasWidth = drawingCanvas.getWidth();
            double canvasHeight = drawingCanvas.getHeight();
            double aspectRatio = originalImage.getWidth() / originalImage.getHeight();
            double newWidth, newHeight;

            if (aspectRatio >= 1) { // Obrázek je širší než vyšší
                newWidth = canvasWidth;
                newHeight = canvasWidth / aspectRatio;
            } else { // Obrázek je vyšší než širší
                newHeight = canvasHeight;
                newWidth = canvasHeight * aspectRatio;
            }

            // Vymazání plátna a vykreslení původního obrázku se zachováním poměru stran
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, canvasWidth, canvasHeight);
            gc.drawImage(originalImage, (canvasWidth - newWidth) / 2, (canvasHeight - newHeight) / 2, newWidth, newHeight);

        }
    }

    @FXML
    private void applyNegativeFilter() {
        saveState();
        Image image = drawingCanvas.snapshot(null, null);
        PixelReader reader = image.getPixelReader();
        WritableImage writableImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        PixelWriter writer = writableImage.getPixelWriter();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = reader.getColor(x, y);
                // Příklad inverze barvy
                Color invertedColor = color.invert();
                writer.setColor(x, y, invertedColor);
            }
        }

        drawingCanvas.getGraphicsContext2D().drawImage(writableImage, 0, 0);
    }

    @FXML
    private void applyPixelation() {
        saveState();
        int pixelSize = (int) pixelSizeSlider.getValue(); // Získání hodnoty ze slideru

        WritableImage image = drawingCanvas.snapshot(null, null);
        PixelReader reader = image.getPixelReader();
        WritableImage writableImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        PixelWriter writer = writableImage.getPixelWriter();

        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        for (int y = 0; y < height; y += pixelSize) {
            for (int x = 0; x < width; x += pixelSize) {
                // Výpočet průměrné barvy pro aktuální buňku
                Color averageColor = calculateAverageColor(reader, x, y, pixelSize, width, height);

                // Vyplnění buňky průměrnou barvou
                for (int dy = 0; dy < pixelSize && y + dy < height; dy++) {
                    for (int dx = 0; dx < pixelSize && x + dx < width; dx++) {
                        writer.setColor(x + dx, y + dy, averageColor);
                    }
                }
            }
        }

        // Aplikace pixelovaného obrazu na canvas
        drawingCanvas.getGraphicsContext2D().drawImage(writableImage, 0, 0);
    }

    private Color calculateAverageColor(PixelReader reader, int startX, int startY, int pixelSize, int maxWidth, int maxHeight) {
        double red = 0, green = 0, blue = 0, opacity = 0;
        int count = 0;

        for (int y = startY; y < startY + pixelSize && y < maxHeight; y++) {
            for (int x = startX; x < startX + pixelSize && x < maxWidth; x++) {
                Color color = reader.getColor(x, y);
                red += color.getRed();
                green += color.getGreen();
                blue += color.getBlue();
                opacity += color.getOpacity();
                count++;
            }
        }

        return new Color(red / count, green / count, blue / count, opacity / count);
    }

    @FXML
    private void handleExit() {
        // Akce pro zavření aplikace
        Platform.exit();
    }

    @FXML
    private void handleAbout() {
        // Akce pro zobrazení informací o aplikaci
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Drawing App");
        alert.setContentText("This is a simple drawing application built with JavaFX.");
        alert.showAndWait();
    }
}