package ir.mahdiparastesh.mobinaexplorer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/* var data: ByteBuffer
        c.resources.assets.openFd("model_raw_single.tflite").apply {
            data = FileInputStream(fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            close()
        }
        val input = arrayOf<Array<Array<ByteArray>>>(
            TFUtils.prepareImage(Mobina(c).first, 0)
        )
        val output = Array(input.size) { ByteArray(1) }
        Interpreter(data).use {
            it.run(input, output)
            it.close()
        }
        b.bytes.text = Gson().toJson(output)*/

public class TFUtils {
    static byte[][][] prepareImage(Bitmap bitmap, int rotationDegrees) {
        int modelImageSize = 800;

        Bitmap paddedBitmap = padToSquare(bitmap);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                paddedBitmap, modelImageSize, modelImageSize, true);

        /*Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(rotationDegrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(
                scaledBitmap, 0, 0, modelImageSize, modelImageSize, rotationMatrix, false);*/

        byte[][][] normalizedRgb = new byte[modelImageSize][modelImageSize][3];
        for (int y = 0; y < modelImageSize; y++) {
            for (int x = 0; x < modelImageSize; x++) {
                Color c = Color.valueOf(scaledBitmap.getPixel(x, y));
                normalizedRgb[y][x][0] = (byte) ((c.red() * 255f) - 128f);
                normalizedRgb[y][x][1] = (byte) ((c.green() * 255f) - 128f);
                normalizedRgb[y][x][2] = (byte) ((c.blue() * 255f) - 128f);
            }
        }

        return normalizedRgb;
    }

    private static Bitmap padToSquare(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int paddingX = width < height ? (height - width) / 2 : 0;
        int paddingY = height < width ? (width - height) / 2 : 0;
        Bitmap paddedBitmap = Bitmap.createBitmap(
                width + 2 * paddingX, height + 2 * paddingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(paddedBitmap);
        canvas.drawARGB(0xFF, 0xFF, 0xFF, 0xFF);
        canvas.drawBitmap(source, paddingX, paddingY, null);
        return paddedBitmap;
    }
}
