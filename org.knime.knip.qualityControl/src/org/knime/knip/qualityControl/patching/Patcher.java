package org.knime.knip.qualityControl.patching;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.Type;

public class Patcher {

        /* Creates a list of patches for the input image.
         * Parameters:  dimensions: Array containing the index of the dimensions in which the image is to be cut into patches
         *              patchesPerDimension: Number of patches per corresponding dimension
         *              img: Image that is to be cut into patches
         * Output:      List of patches of which each contains a part of the input image
         * 
         */
        public static <T extends Type<T>> Object[] patchImg(final Img<T> img, final int[] dimensions, final int[] patchesPerDimension) {
                final int n = dimensions.length;

                // For each dimension that is to be patched, the number of patches needs to be provided
                if (n != patchesPerDimension.length) {
                        System.out.println("Error in org.knime.knip.qualityControl.patching Patcher :"
                                        + "dimensions and patchesPerDimension need to be of the same length");
                        return null;
                }

                // Computation of number of overall patches and determination of the patch size for each dimension
                int numPatches = 1;
                final long[] dimensionSize = new long[img.numDimensions()];
                img.dimensions(dimensionSize);
                final long[] patchSizePerDimension = new long[n];

                for (int i = 0; i < n; i++) {
                        numPatches *= patchesPerDimension[i];
                        patchSizePerDimension[i] = (dimensionSize[i]) / patchesPerDimension[i];
                }

                final Object[] patches = new Object[numPatches];
                final Counter counter = new Counter(patchesPerDimension);
                final int numDimensions = img.numDimensions();

                // compute patches
                for (int p = 0; p < numPatches; p++) {
                        // create patch
                        final long[] min = new long[numDimensions];
                        final long[] max = new long[numDimensions];
                        int index = 0;
                        int currentSpecifiedDimension = dimensions[index];

                        // determine patch interval
                        for (int d = 0; d < numDimensions; d++) {
                                // calculate intervals for specified dimensions
                                if (currentSpecifiedDimension == d) {
                                        min[d] = counter.getDigit(index) * patchSizePerDimension[index];
                                        // the last patch of the dimension is larger if the patch size does
                                        // not evenly fit the dimension size
                                        max[d] = (counter.getDigit(index) == patchesPerDimension[index] - 1) ? dimensionSize[d] : min[d]
                                                        + patchSizePerDimension[index] - 1;

                                        // select next specified dimension
                                        index += (index == dimensions.length - 1) ? 0 : 1;
                                        currentSpecifiedDimension = dimensions[index];
                                } else {
                                        // include unspecified dimensions completely into the patch
                                        min[d] = 0;
                                        max[d] = dimensionSize[d];
                                }
                        }
                        patches[p] = createPatch(img, min, max);

                        // increment counter
                        counter.increment();
                }

                return patches;
        }

        /* Creates a single patch of img that is determined by the interval given by min and max
         * Parameters:  img:    Image from which we want to draw a patch
         *              min:    Array that contains the minimum coordinates that should be in the patch for each dimension
         *              max:    Array that contains the maximum coordinates that should be in the patch for each dimension
         * Output:  A patch that contains the by min and max specified interval of img
         */
        private static <T extends Type<T>> Img<T> createPatch(Img<T> img, long[] min, long[] max) {
                // factory to create new Img<T>
                final ImgFactory<T> factory = img.factory();
                final long[] dimensions = new long[min.length];

                // computation of dimension size for each dimension
                for (int i = 0; i < min.length; i++) {
                        dimensions[i] = max[i] - min[i];
                }

                final Img<T> patch = factory.create(dimensions, img.firstElement());

                // cursor and randomaccess to iterate over the patch and set for each position 
                // the corresponding value for the position in img
                final Cursor<T> cursor = patch.localizingCursor();
                final RandomAccess<T> ra = img.randomAccess();

                while (cursor.hasNext()) {
                        cursor.fwd();
                        final long[] pos = new long[img.numDimensions()];
                        cursor.localize(pos);
                        // positions must be corrected by offset min
                        for (int i = 0; i < pos.length; i++)
                                pos[i] += min[i];
                        ra.setPosition(pos);
                        cursor.get().set(ra.get());
                }

                return patch;
        }

        /* Calculates the patches per dimension relative to the dimension size
         * Parameters:  numPatches: Total number of patches must be a power of 2
         *              dimensions: The size of each dimension (the index indicates which dimension)
         * Output:  int array containing the patches per dimension
         */
        public static <T> int[] calculatePatchesPerDimensionRelSize(final int numPatches, final long[] dimensions) {

                // captures the patches per dimension
                final int[] patchesPerDimension = new int[dimensions.length];
                long sumSizes = 0;
                for (long d : dimensions)
                        sumSizes += d;
                // compute ratio of sizes of the dimensions
                final double[] sizeRatio = new double[dimensions.length];
                for (int d = 0; d < sizeRatio.length; d++)
                        sizeRatio[d] = ((double) dimensions[d]) / sumSizes;

                // log2 of numPatches
                final int pow = log2(numPatches);
                // used to keep track how many patches are already assigned
                int powCounter = pow;

                // assign patches to dimensions
                for (int p = 0; p < patchesPerDimension.length; p++) {
                        if (powCounter == 0) {
                                patchesPerDimension[p] = 1;
                        } else {
                                patchesPerDimension[p] = (int) Math.pow(2, (int) (sizeRatio[p] * pow));
                                powCounter -= (int) (sizeRatio[p] * pow);
                        }
                }

                // assign left over patches to largest dimension
                if (powCounter > 0) {
                        patchesPerDimension[argmax(sizeRatio)] *= (int) Math.pow(2, powCounter);
                }

                return patchesPerDimension;
        }

        /* Calculates the most even distribution of patches over the dimensions
         * for example 4*4 is better than 8*2 (assuming that the dimensions are of approximately the same size)
         * Parameters:  numPatches: Total number of patches must be a power of 2
         *              dimensions: The size of each dimension (the index indicates which dimension)
         * Output:  int array containing the patches per dimension
         */
        public static int[] calculatePatchesPerDimension(final int numPatches, final long[] dimensions) {
                int pow = log2(numPatches);

                int avgPatches = (int) Math.round(((double) pow) / dimensions.length);

                int[] patchesPerDimension = new int[dimensions.length];

                for (int p = 0; p < dimensions.length; p++) {
                        if (pow == 0) {
                                patchesPerDimension[p] = 1;
                        } else if (pow - avgPatches >= 0) {
                                patchesPerDimension[p] = (int) Math.pow(2, avgPatches);
                                pow -= avgPatches;
                        } else {
                                patchesPerDimension[p] = (int) Math.pow(2, pow);
                                pow = 0;
                        }
                }

                if (pow > 0)
                        patchesPerDimension[0] *= (int) Math.pow(2, pow);

                return patchesPerDimension;
        }

        /* Computes the log2 for input ONLY ACCURATE FOR POWERS OF 2
         * Parameters:  input:  The int for which the log2 is to be computed
         * Output:  The log2 of input
         */
        private static int log2(final int input) {
                int num = input;
                int pow = 0;
                for (; num > 0; pow++)
                        num >>= 1;
                pow--;
                return pow;
        }

        /* Returns the index of the largest element of array
         * Parameters:  array:  input double array
         * Output:  Index of largest element in array
         */
        private static int argmax(final double[] array) {
                int index = 0;

                for (int i = 0; i < array.length; i++)
                        index = (array[i] > array[index]) ? i : index;

                return index;
        }

        public static void main(String[] args) {
                int numPatches = 256;
                long[] dimensions = {100, 100, 100};
                int[] patchesPerDimension = calculatePatchesPerDimension(numPatches, dimensions);

                System.out.print("patchesPerDimension: ");
                for (int dim : patchesPerDimension)
                        System.out.print(dim + " ");
                System.out.println();

        }

}
