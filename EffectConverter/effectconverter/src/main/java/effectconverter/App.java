package effectconverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;

/**
 * Hello world!
 *
 */
public class App 
{
    private static final int NUM_LEDS = 147;
    private static final String inputDir = "C:\\Users\\Alex\\Downloads\\patterns170918";
    private static final String outputDir = "C:\\Users\\Alex\\Documents\\Projects\\LED Hoop\\effects_v2";

    public static void main(String[] args) throws Exception {
        decodeFiles(inputDir, outputDir);
        //decodeImage(new File("src/chaser.bmp"));
        //streamImage(new File("src/chaser.bmp"));
    }

    private static void decodeFiles(String input, String output) throws IOException {
        File dir = new File(input);

        System.out.println("Searching directory: " + input);

        Collection<File> files = FileUtils.listFiles(dir, new RegexFileFilter("^([^\\s]+(\\.(?i)(bmp))$)"), DirectoryFileFilter.DIRECTORY);

        System.out.println("Found " + files.size() + " effects");

        int count = 1;
        for(File file : files) {
            Path outputPath = Paths.get(outputDir, String.format("PAT%04d.PAT", count));
            Files.createFile(outputPath);
            File outputFile = new File(outputPath.toString());
            BufferedImage bufferedImage = decodeImage(file, outputFile);
            saveImage(bufferedImage, Paths.get(outputDir, "resized/", String.format("PAT%04d.BMP", count)));
            count++;
        }
    }

    private static BufferedImage decodeImage(File input, File output) throws IOException {
        System.out.println("Decoding image: " + input);
        BufferedImage img = ImageIO.read(input);

        int height = img.getHeight();
        int width = img.getWidth();

        BufferedImage resized;

        if(width < (NUM_LEDS / 2)) {
            resized = tileImage(img, NUM_LEDS, height);
            System.out.println("Tiling image: " + input + " | Width: " + width + " | Height: " + height);
        } else {
            double scaleFactor = (double) NUM_LEDS / (double)width;
            height = scaleFactor >= 1 ? (int)((double)height * scaleFactor) : height;
            resized = scaleImage(img, NUM_LEDS, height);
            System.out.println("Scaling image: " + input + " | Width: " + width + " | Height: " + height);
        }

        writeBytesToFile(new FileOutputStream(output, false), resized);

        System.out.println("Finished decoding: " + input + " | Output: " + output);

        return resized;
    }

    private static void writeBytesToFile(FileOutputStream out, BufferedImage image) throws IOException {
        int red, green, blue;

        out.write(new byte[] {(byte)image.getHeight()}, 0, 1); // number of frames

        for (int h = 0; h < image.getHeight(); h++) {
            for (int w = 0; w < NUM_LEDS; w++) {
                Color color = new Color(image.getRGB(w, h));
                red = color.getRed() & 0xFF;
                green = color.getGreen() & 0xFF;
                blue = color.getBlue() & 0xFF;
                out.write(new byte[] {(byte)red, (byte)green, (byte)blue}, 0, 3);
            }
        }

        out.flush();
    }

    private static BufferedImage scaleImage(BufferedImage img, int newW, int newH) {
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_DEFAULT);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }

    private static BufferedImage tileImage(BufferedImage img, int newW, int newH) {
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = dimg.createGraphics();
        for(int i = 0; i < newW; i+=10) {
            g2d.drawImage(img, i, 0, null);
        }
        g2d.dispose();

        return dimg;
    }

    private static File getFile(Path path) throws IOException {
        File file = new File(path.toString());
        if(!file.exists()) {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }
        return file;
    }

    private static void saveImage(BufferedImage image, Path path) throws IOException {
        File file = getFile(path);
        ImageIO.write(image, "BMP", file);
    }
}
