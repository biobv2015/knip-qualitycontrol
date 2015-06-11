package org.knime.knip.qc.globalfeature;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class GradientMagnitudeHistogram {

        public static <T extends RealType<T>> double[] calculateGradientMagnitudeHistogram(final Img<T> img, final int min, final int max) {
                final int maxNum = (int) img.firstElement().getMaxValue();
                final int minNum = (int) img.firstElement().getMinValue();
                final int rangeNum = maxNum - minNum;
                System.out.println("rangeNum: " + rangeNum);
                final int range = max - min + 1;

                Cursor<T> center = img.localizingCursor();
                ExtendedRandomAccessibleInterval<T, Img<T>> outOfBounds = Views.extendMirrorSingle(img);

                RandomAccess<T> front = outOfBounds.randomAccess();
                RandomAccess<T> back = outOfBounds.randomAccess();

                final double[] histogram = new double[range];

                while (center.hasNext()) {
                        center.fwd();
                        long[] pos = new long[img.numDimensions()];
                        center.localize(pos);
                        double gradientMagnitude = 0;
                        for (int d = 0; d < img.numDimensions(); d++) {
                                front.setPosition(pos);
                                front.fwd(d);
                                back.setPosition(pos);
                                back.bck(d);
                                double f = front.get().getRealDouble();
                                double b = back.get().getRealDouble();
                                double g = (front.get().getRealDouble() - back.get().getRealDouble()) * 0.5;
                                gradientMagnitude += g * g;
                        }

                        gradientMagnitude = Math.sqrt(gradientMagnitude);

                        if ((int) gradientMagnitude >= min && (int) gradientMagnitude <= max) {
                                histogram[(int) gradientMagnitude - min]++;
                        }

                }

                final long[] dimensions = new long[img.numDimensions()];
                img.dimensions(dimensions);
                long numPixels = 1;
                for (long d : dimensions)
                        numPixels *= d;

                for (int i = 0; i < histogram.length; i++) {
                        histogram[i] = histogram[i] / numPixels;
                }

                return histogram;
        }
}
