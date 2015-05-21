package org.knime.knip.qualityControl.patching;

import java.util.ArrayList;
import java.util.HashMap;

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
         * Output:      Object array that contains patches of which each contains a part of the input image
         * 
         */
        public static <T extends Type<T>> Object[] patchImg(final int[] dimensions, final int[] patchesPerDimension, final Img<T> img) {
                final int n = dimensions.length;

                // For each dimension that is to be patched, the number of patches needs to be provided
                if (n != patchesPerDimension.length) {
                        System.out.println("Error in org.knime.knip.qualityControl.patching Patcher :"
                                        + "dimensions and patchesPerDimension need to be of the same length");
                        return null;
                }

                // Computation of number of overall patches and determination of the patch size for each dimension
                int numPatches = 1;
                final long[] dimensionSize = new long[n];
                img.dimensions(dimensionSize);
                final long[] patchSizePerDimension = new long[n];

                for (int i = 0; i < n; i++) {
                        numPatches *= patchesPerDimension[i];
                        patchSizePerDimension[i] = (dimensionSize[i]) / patchesPerDimension[i];
                        if (patchSizePerDimension[i] == 0) {
                                System.out.println("Error in org.knime.knip.qualityControl.patching Patcher :"
                                                + "The number of patches per dimension must be smaller or equal to the size of the dimension.");
                        }
                }

                // HashMap is necessary to later check whether dimension should be patched or not
                final HashMap<Integer, Integer> dimensionsMap = createHashMapFromArray(dimensions);
                final ArrayList<Img<T>> patches = new ArrayList<Img<T>>(numPatches);

                // creation of the patches
                for (int i = 0; i < numPatches; i++) {
                        final int numDimensions = img.numDimensions();
                        final long[] min = new long[numDimensions];
                        final long[] max = new long[numDimensions];

                        // computation of patch boundaries per dimension 
                        //(full dimension if the dimension is not specified to be patched)
                        for (int j = 0; j < numDimensions; j++) {
                                if (dimensionsMap.containsKey(j)) {
                                        final int index = dimensionsMap.get(j);
                                        // since it is possible that an image cannot be evenly divided into patches,
                                        // the last patch of the dimension might be larger
                                        min[j] = (i % patchesPerDimension[index]) * patchSizePerDimension[index];
                                        max[j] = (((i + 1) % patchesPerDimension[index]) == 0) ? dimensionSize[j] : min[j]
                                                        + patchSizePerDimension[index] - 1;
                                } else {
                                        min[j] = 0;
                                        max[j] = dimensionSize[j];
                                }
                        }

                        patches.add(createPatch(img, min, max));

                }

                return patches.toArray();
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

        /* Creates a HashMap from an array (helper function for patchImg)
         * Input:   array: int array containing values (that will be the keys later)
         * Output:  HashMap for which the values of the arrays are the keys and the indices are the values
         */
        private static HashMap<Integer, Integer> createHashMapFromArray(int[] array) {
                HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(array.length);
                for (int i = 0; i < array.length; i++) {
                        map.put(array[i], i);
                }
                return map;
        }

}
