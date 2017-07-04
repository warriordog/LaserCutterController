package net.acomputerdog.lccontroller.gui;

import net.acomputerdog.lccontroller.LaserProperties;
import net.acomputerdog.lccontroller.gui.script.ScriptRunner;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ComponentScriptPath extends JPanel {
    public static final int LINES_PER_TICK = 100;

    // cant put in constructor because IntelliJ builder runs before window constructor
    private GUIMain main;

    private final Color BG_COLOR = new Color(222, 238, 239);
    private final Color GRAPH_COLOR = new Color(204, 206, 206);
    private final Color PATH_COLOR = Color.RED;

    private BufferedImage image;
    private ScriptRunner script;
    private boolean isDrawing = false;
    private int nextLine = 0;
    private long lastGX, lastGY;
    private int lastIX, lastIY;

    // scaling stuff
    //private int imageStartX, imageStartY, imageWidth, imageHeight;
    private double scaleLevel = 0.001d;

    public GUIMain getMain() {
        return main;
    }

    public void setMain(GUIMain main) {
        this.main = main;
    }

    public ScriptRunner getScript() {
        return script;
    }

    public void setScript(ScriptRunner script) {
        this.script = script;

        LaserProperties prop = main.getLaserProperties();
        this.image = new BufferedImage(prop.getBedWidth(), prop.getBedHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        drawGrid(image.getGraphics(), image.getWidth(), image.getHeight());

        //updateScale(image.getWidth(), image.getHeight(), getWidth(), getHeight());
        //drawScript(image.getGraphics(), image.getWidth(), image.getHeight());
        nextLine = 0;
        scaleLevel = 0.001d;
        isDrawing = true;
        main.addLogLine("Starting script draw.");

        //super.repaint();
    }

    public void updateDraw() {
        if (isDrawing) {
            String[] lines = script.getLines();
            Graphics g = image.getGraphics();
            g.setColor(PATH_COLOR);
            for (int i = 0; i < LINES_PER_TICK; i++) {
                if (nextLine >= lines.length) {
                    // we are finished, so we need to redraw
                    isDrawing = false;
                    repaint();
                    main.addLogLine("Script draw complete.");
                    break;
                } else {
                    long lX = lastGX;
                    long lY = lastGY;
                    String line = lines[nextLine];
                    try {
                        if (line.startsWith("G0") || line.startsWith("G1")) {
                            int xIdx = line.indexOf('X');
                            if (xIdx > -1 && line.length() - xIdx > 2) {
                                int spaceIdx = line.indexOf(' ', xIdx);
                                if (spaceIdx > -1) {
                                    lX = Long.parseLong(line.substring(xIdx + 1, spaceIdx));
                                } else {
                                    lX = Long.parseLong(line.substring(xIdx + 1));
                                }
                            }

                            int yIdx = line.indexOf('Y');
                            if (yIdx > -1 && line.length() - yIdx > 2) {
                                int spaceIdx = line.indexOf(' ', yIdx);
                                if (spaceIdx > -1) {
                                    lY = Long.parseLong(line.substring(yIdx + 1, spaceIdx));
                                } else {
                                    lY = Long.parseLong(line.substring(yIdx + 1));
                                }
                            }

                            int iX = (int) Math.round(((double) lX * scaleLevel));
                            int iY = (int) Math.round(((double) lX * scaleLevel));

                            // draw line from lastIX, lastIY -> iX, iY
                            drawThickLine(g, lastIX, lastIY, iX, iY, 3);

                            //System.out.printf("Drawing from (%d, %d) to (%d, %d)\n", lastIX, lastIY, iX, iY);

                            lastIX = iX;
                            lastIY = iY;
                            lastGX = lX;
                            lastGY = lY;
                        }
                    } catch (NumberFormatException e) {
                        main.addLogLine(String.format("Preview failed: malformed gcode on line %d: %s", nextLine, line));
                        isDrawing = false;
                        break;
                    }
                    nextLine++;
                }
            }
        }
    }

    void drawThickLine(Graphics g, int x1, int y1, int x2, int y2, int width) {
        if (width > 0) {
            g.drawLine(x1, y1, x2, y2);

            if (width > 1) {
                int currSide = 1;

                // not a line
                if (x1 == x2 && y1 == y2) {
                    return;
                    // horizontal line
                } else if (y1 == y2) {
                    for (int i = 1; i < width; i++) {
                        int newY = y1 + (i * currSide);
                        g.drawLine(x1, newY, x2, newY);
                        currSide *= -1;
                    }
                    // vertical or diagonal line
                } else if (x1 == x2) {
                    for (int i = 1; i < width; i++) {
                        int newX = x1 + (i * currSide);
                        g.drawLine(newX, y1, newX, y2);
                        currSide *= -1;
                    }
                }
            }
        }
    }

    /*
    private void updateScale(int imWidth, int imHeight, int viewWidth, int viewHeight) {
        //TODO implement scale
        int xSizeDiff = imWidth - viewWidth;
        int ySizeDiff = imHeight - viewHeight;
        int xSizeDiffPerSide = xSizeDiff / 2;
        int ySizeDiffPerSide = ySizeDiff / 2;

        imageStartX = -xSizeDiffPerSide;
        imageStartY = -ySizeDiffPerSide;
        imageWidth = viewWidth + xSizeDiff; // account for both edges
        imageHeight = viewHeight + ySizeDiff; // account for both edges
    }
    */

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawBackground(g, getWidth(), getHeight());

        if (image != null && !isDrawing) {
            int widthDiff = getWidth() - image.getWidth();
            int heightDiff = getHeight() - image.getHeight();
            int widthMag = Math.abs(widthDiff);
            int heightMag = Math.abs(heightDiff);

            //System.out.printf("Component (%d, %d) - image (%d, %d) = diff (%d, %d)\n", getWidth(), getHeight(), image.getWidth(), image.getHeight(), widthDiff, heightDiff);

            float rat;
            if (widthMag > heightMag) {

                rat = (float) getWidth() / (float) image.getWidth();
            } else {
                rat = (float) getHeight() / (float) image.getHeight();
            }

            float outWidthF = rat * (float) image.getWidth();
            float outHeightF = rat * (float) image.getHeight();

            // check for overflow
            if (outWidthF > getWidth()) {
                float downscale = getWidth() / outWidthF;
                outWidthF = outWidthF * downscale;
                outHeightF = outHeightF * downscale;
            } else if (outHeightF > getHeight()) {
                float downscale = getHeight() / outHeightF;
                outWidthF = outWidthF * downscale;
                outHeightF = outHeightF * downscale;
            }

            int outWidth = Math.round(outWidthF);
            int outHeight = Math.round(outHeightF);

            // TODO center

            //System.out.printf("image (%d, %d) * rat (%f) = size (%d, %d)\n", image.getWidth(), image.getHeight(), rat, outWidth, outHeight);

            //System.out.printf("Drawing image from (%d, %d) to (%d, %d)\n", 0, 0, outWidth, outHeight);

            g.drawImage(image, 0, 0, outWidth, outHeight, BG_COLOR, null);
        }
    }

    private void drawBackground(Graphics g, int width, int height) {
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, width, height);
    }

    private void drawScript(Graphics g, int width, int height) {
        drawGrid(g, width, height);
    }

    private void drawGrid(Graphics g, int width, int height) {
        g.setColor(GRAPH_COLOR);

        // draw border
        g.drawRect(0, 0, width - 1, height - 1);
        g.drawRect(1, 1, width - 2, height - 2);

        //TODO apply to actual scale
        // slow, but necessary
        float xSpacing = width / 10f;
        float ySpacing = height / 10f;

        // vertical lines
        for (int x = 0; x < 10; x++) {
            int xLoc = Math.round((float) x * xSpacing);
            g.drawLine(xLoc, 0, xLoc, height);
        }

        // horizontal lines
        for (int y = 0; y < 10; y++) {
            int yLoc = Math.round((float) y * ySpacing);
            g.drawLine(0, yLoc, width, yLoc);
        }
    }

    public boolean isDrawing() {
        return isDrawing;
    }
}
