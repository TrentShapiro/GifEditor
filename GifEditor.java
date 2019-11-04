import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;


public class GifEditor {
    private static String input_filepath =  "/Users/tshapi27/Desktop/RainbowGiffer/face_palm.png";
    private static String output_filepath = "/Users/tshapi27/Desktop/RainbowGiffer/face_palm_java_output_foreground.gif";
    private static Integer color_loops = 1;
    private static String mode = "foreground";

    public static void main(String[] args){

        try {
            BufferedImage image = ImageIO.read(new File(input_filepath));

            BufferedImage[] frames = new BufferedImage[50];

            int eff_size = frames.length / color_loops;

            int[] color_list = getColorIntLists(eff_size);
            int[] eff_color_list = new int[frames.length];

            for(int i=0; i < frames.length; i++){
                eff_color_list[i] = color_list[i%eff_size];
            }

            for(int i = 0; i < frames.length; i++){
                BufferedImage copy = deepCopy(image);
                for (int j = 0; j < copy.getWidth(); j++) {
                    for (int y = 0; y < copy.getWidth(); y++) {
                        if (!isTransparent(image,j,y) & mode == "foreground"){
                            int pixel_red = (copy.getRGB(j,y) >> 16) & 0xFF;
                            int pixel_green = (copy.getRGB(j,y) >> 8) & 0xFF;
                            int pixel_blue = copy.getRGB(j,y) & 0xFF;

                            int color_red = (eff_color_list[i] >> 16) & 0xFF;
                            int color_green = (eff_color_list[i] >> 8) & 0xFF;
                            int color_blue = eff_color_list[i] & 0xFF;

                            int avg_red = (pixel_red^2 + color_red^2) / 2;
                            int avg_green = (pixel_green^2 + color_green^2) / 2;
                            int avg_blue = (pixel_blue^2 + color_blue^2) / 2;

                            int avg_color = (new Color(avg_red, avg_green, avg_blue)).getRGB();
                            copy.setRGB(j, y, avg_color);

                        }else if(isTransparent(image,j,y) & mode == "background"){
                            copy.setRGB(j, y,eff_color_list[i]);
                        }
                    }
                }
                frames[i] = copy;
            }

            int x_mid = (int) Math.floor(frames[0].getWidth() / 2);
            int y_mid = (int) Math.floor(frames[0].getHeight() / 2);

            BufferedImage[] upper_left = new BufferedImage[frames.length];
            BufferedImage[] upper_right = new BufferedImage[frames.length];
            BufferedImage[] lower_left = new BufferedImage[frames.length];
            BufferedImage[] lower_right = new BufferedImage[frames.length];

            double scale = 0.33;

            // Split in quadrants
            for(int i=0; i < frames.length; i++){
                upper_left[i] = getScaledInstance(deepCopy(frames[i]).getSubimage( 0,0, x_mid , y_mid),
                        35,35 , RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);

                upper_right[i] = getScaledInstance(deepCopy(frames[i]).getSubimage( x_mid + 1,0, x_mid-1, y_mid),
                        35,35 , RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);

                lower_left[i] = getScaledInstance(deepCopy(frames[i]).getSubimage(0, y_mid+1, x_mid, y_mid-1),
                        35,35 , RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);

                lower_right[i] = getScaledInstance(deepCopy(frames[i]).getSubimage(x_mid + 1,y_mid + 1, x_mid-1, y_mid-1),
                        35,35 , RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
            }
            // write image
            writeImage(output_filepath, frames);
            writeImage("/Users/tshapi27/Desktop/RainbowGiffer/face_palm_java_output_1.gif" ,upper_left);
            writeImage("/Users/tshapi27/Desktop/RainbowGiffer/face_palm_java_output_2.gif" ,upper_right);
            writeImage("/Users/tshapi27/Desktop/RainbowGiffer/face_palm_java_output_3.gif" ,lower_left);
            writeImage("/Users/tshapi27/Desktop/RainbowGiffer/face_palm_java_output_4.gif" ,lower_right);

        } catch (IOException e) {
            System.out.println("Could not read file.");
        }


    }

    static BufferedImage deepCopy(BufferedImage im) {
        ColorModel cm = im.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = im.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    static int[] getColorIntLists(int array_size){
        int[] colors = new int[array_size];
        float jump = 360.0f / (array_size*1.0f);
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.HSBtoRGB(1.0f*(jump*i), 1.0f, 1.0f);
        }
        return colors;
    }

    static boolean isTransparent(BufferedImage image, int x, int y ) {
        int pixel = image.getRGB(x,y);
        if( (pixel>>24) == 0x00 ) {
            return true;
        }
        return false;
    }

    static void writeImage(String filepath, BufferedImage[] frames ) {
        try {
            // grab the output image type from the first image in the sequence
            BufferedImage firstImage = frames[0];

            // create a new BufferedOutputStream with the last argument
            ImageOutputStream output = new FileImageOutputStream(new File(filepath));

            // create a gif sequence with the type of the first image, 1 second
            // between frames, which loops continuously
            GifSequenceWriter writer =
                    new GifSequenceWriter(output, firstImage.getType(), 125, true);

//            System.out.println(firstImage.getType());

            // write out the first image to our sequence...
            writer.writeToSequence(firstImage);
            for (int i = 1; i < frames.length - 1; i++) {
                BufferedImage nextImage = frames[i];
                writer.writeToSequence(nextImage);
            }

            writer.close();
            output.close();
        } catch (IOException e) {
            System.out.println("Could not write file.");
        }
    }

    private static BufferedImage scaleImage(BufferedImage before, double scale) {
        int w = before.getWidth();
        int h = before.getHeight();

        // Create a new image of the proper size
        int w2 = (int) (w * scale);
        int h2 = (int) (h * scale);
        BufferedImage after = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_ARGB);
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BILINEAR);

        scaleOp.filter(before, after);
        return after;
    }

    public static BufferedImage getScaledInstance(BufferedImage img,
                                           int targetWidth,
                                           int targetHeight,
                                           Object hint,
                                           boolean higherQuality)
    {
        int type = (img.getTransparency() == Transparency.OPAQUE) ?
                BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage ret = (BufferedImage)img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }
}
