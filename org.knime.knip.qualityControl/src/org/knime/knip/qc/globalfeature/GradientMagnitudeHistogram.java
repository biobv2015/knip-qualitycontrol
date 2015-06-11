package org.knime.knip.qc.globalfeature;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class GradientMagnitudeHistogram {

        public static <T extends IntegerType<T>> double[] calculateGradientMagnitudeHistogram(final Img<T> img, final int min, final int max) {
                final int maxNum = (int) img.firstElement().getMaxValue();
                final int minNum = (int) img.firstElement().getMinValue();
                final int rangeNum = maxNum - minNum;
                final int range = max - min;

                Cursor<T> center = img.localizingCursor();
                ExtendedRandomAccessibleInterval<T, Img<T>> outOfBorder = Views.extendMirrorSingle(img);

                RandomAccess<T> front = outOfBorder.randomAccess();
                RandomAccess<T> back = outOfBorder.randomAccess();

                final double[] histogram = new double[range];

                while (center.hasNext()) {
                        center.fwd();
                        long[] pos = new long[img.numDimensions()];
                        center.localize(pos);
                        double gradientMagnitude = 0;
                        for (int d = 0; d < img.numDimensions(); d++) {
                                long[] posFront = pos;
                                posFront[d]++;
                                long[] posBack = pos;
                                posBack[d]--;
                                front.setPosition(posFront);
                                back.setPosition(posBack);
                                double g = (front.get().getRealDouble() - back.get().getRealDouble()) * 0.5;
                                gradientMagnitude += g * g;
                        }

                        gradientMagnitude = Math.sqrt(gradientMagnitude);

                        if ((int) gradientMagnitude >= min && (int) gradientMagnitude <= max) {
                                histogram[(int) gradientMagnitude - min]++;
                        }

                }

                for (int i = 0; i < histogram.length; i++) {
                        histogram[i] = histogram[i] / rangeNum;
                }

                return histogram;
        }
}
