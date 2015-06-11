/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.qc.nodes.globalfeature;

import java.io.File;

import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.BufferedDataTableHolder;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.qc.globalfeature.GradientMagnitudeHistogram;

/**
 * Crop BitMasks or parts of images according to a Labeling
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @param <L>
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class GlobalFeatureNodeModel<L extends Comparable<L>, T extends RealType<T>> extends NodeModel implements BufferedDataTableHolder {

        static final String[] BACKGROUND_OPTIONS = new String[] {"Min Value of Result", "Max Value of Result", "Zero", "Source"};

        /**
         * Helper
         *
         * @return SettingsModel to store img column
         */
        static SettingsModelString createImgColumnSelectionModel() {
                return new SettingsModelString("GFImgColumnSelectionModel", "");
        }

        static SettingsModelDoubleRange createRangeSelectionModel() {
                return new SettingsModelDoubleRange("GFRangeSelectionModel", 0, 64);
        }

        /* SettingsModels */
        private SettingsModelString m_imgColumnNameModel = createImgColumnSelectionModel();
        private SettingsModelDoubleRange m_rangeModel = createRangeSelectionModel();

        /* Resulting BufferedDataTable */
        private BufferedDataTable m_data;

        /**
         * Constructor SegementCropperNodeModel
         */
        public GlobalFeatureNodeModel() {
                super(1, 1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
                // TODO check inspec for img value column

                return createOutSpec();
        }

        private DataTableSpec[] createOutSpec() {
                final int range = (int) m_rangeModel.getMaxRange() - (int) m_rangeModel.getMinRange();
                DataColumnSpec[] columns = new DataColumnSpec[range + 1];
                columns[0] = new DataColumnSpecCreator("Source Image", ImgPlusCell.TYPE).createSpec();
                for (int i = 1; i < columns.length; i++) {
                        columns[i] = new DataColumnSpecCreator("h" + i, DoubleCell.TYPE).createSpec();
                }

                return new DataTableSpec[] {new DataTableSpec(columns)};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings({"unchecked"})
        protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec) throws Exception {

                final BufferedDataContainer container = exec.createDataContainer(createOutSpec()[0]);
                final ImgPlusCellFactory imgCellFac = new ImgPlusCellFactory(exec);

                int imgCellIdx = inData[0].getSpec().findColumnIndex(m_imgColumnNameModel.getStringValue());

                if (imgCellIdx == -1) {
                        throw new IllegalArgumentException("No Image Column found with name: " + m_imgColumnNameModel.getStringValue());
                }

                final int min = (int) m_rangeModel.getMinRange();
                final int max = (int) m_rangeModel.getMaxRange();
                final int range = max - min;

                for (final DataRow row : inData[0]) {
                        final ImgPlusValue<T> imgPlusValue = (ImgPlusValue<T>) row.getCell(imgCellIdx);
                        DataCell[] cells = new DataCell[range + 1];

                        // get Img from ImgPlus
                        Img<T> img = imgPlusValue.getImgPlus().getImg();

                        // calculate parameters for patching

                        double[] histogram = GradientMagnitudeHistogram.calculateGradientMagnitudeHistogram(img, min, max);

                        cells[0] = imgCellFac.createCell(imgPlusValue.getImgPlus());

                        for (int h = 0; h < histogram.length; h++) {
                                cells[h + 1] = new DoubleCell(histogram[h]);
                        }

                        container.addRowToTable(new DefaultRow(row.getKey().toString(), cells));

                }

                container.close();
                m_data = container.getTable();
                return new BufferedDataTable[] {m_data};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BufferedDataTable[] getInternalTables() {
                return new BufferedDataTable[] {m_data};
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                m_imgColumnNameModel.loadSettingsFrom(settings);
                m_rangeModel.loadSettingsFrom(settings);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void reset() {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) {
                //
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void saveSettingsTo(final NodeSettingsWO settings) {
                m_imgColumnNameModel.saveSettingsTo(settings);
                m_rangeModel.saveSettingsTo(settings);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setInternalTables(final BufferedDataTable[] tables) {
                m_data = tables[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
                m_imgColumnNameModel.validateSettings(settings);
                m_rangeModel.validateSettings(settings);
        }
}