/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.phoenix.pherf.result;

import org.apache.phoenix.pherf.PherfConstants;
import org.apache.phoenix.pherf.result.file.ResultFileDetails;
import org.apache.phoenix.pherf.result.impl.CSVFileResultHandler;
import org.apache.phoenix.pherf.result.impl.ImageResultHandler;
import org.apache.phoenix.pherf.result.impl.XMLResultHandler;
import org.apache.phoenix.util.InstanceResolver;

import java.util.ArrayList;
import java.util.List;

public class ResultManager {
    private final List<ResultHandler> resultHandlers;
    private final ResultUtil util;
    private static final List<ResultHandler> defaultHandlers;

    static {
        defaultHandlers = new ArrayList<>();
        XMLResultHandler xmlResultHandler = new XMLResultHandler();
        xmlResultHandler.setResultFileDetails(ResultFileDetails.XML);
        defaultHandlers.add(xmlResultHandler);

        ImageResultHandler imageResultHandler = new ImageResultHandler();
        imageResultHandler.setResultFileDetails(ResultFileDetails.IMAGE);
        defaultHandlers.add(imageResultHandler);

        ResultHandler handlerAgg = new CSVFileResultHandler();
        handlerAgg.setResultFileDetails(ResultFileDetails.CSV_AGGREGATE_PERFORMANCE);
        defaultHandlers.add(handlerAgg);

        ResultHandler handlerDet = new CSVFileResultHandler();
        handlerDet.setResultFileDetails(ResultFileDetails.CSV_DETAILED_PERFORMANCE);
        defaultHandlers.add(handlerDet);
    }

    public ResultManager(String fileNameSeed) {
        this(fileNameSeed, InstanceResolver.get(ResultHandler.class, defaultHandlers));
    }

    public ResultManager(String fileNameSeed, List<ResultHandler> resultHandlers) {
        this.resultHandlers = resultHandlers;
        util = new ResultUtil();

        for (ResultHandler resultHandler : resultHandlers) {
            if (resultHandler.getResultFileName() == null) {
                resultHandler.setResultFileName(fileNameSeed);
            }
        }
    }

    /**
     * Write out the result to each writer in the pool
     *
     * @param result {@link DataModelResult}
     * @throws Exception
     */
    public synchronized void write(DataModelResult result) throws Exception {
        try {
            util.ensureBaseResultDirExists();
            final DataModelResult dataModelResultCopy = new DataModelResult(result);
            for (ResultHandler handler : resultHandlers) {
                util.write(handler, dataModelResultCopy);
            }
        } finally {
            for (ResultHandler handler : resultHandlers) {
                try {
                    if (handler != null) {
                        handler.flush();
                        handler.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Write a combined set of results for each result in the list.
     *
     * @param dataModelResults List<{@link DataModelResult > </>}
     * @throws Exception
     */
    public synchronized void write(List<DataModelResult> dataModelResults) throws Exception {
        util.ensureBaseResultDirExists();

        CSVFileResultHandler detailsCSVWriter = null;
        try {
            detailsCSVWriter = new CSVFileResultHandler();
            detailsCSVWriter.setResultFileDetails(ResultFileDetails.CSV_DETAILED_PERFORMANCE);
            detailsCSVWriter.setResultFileName(PherfConstants.COMBINED_FILE_NAME);
            for (DataModelResult dataModelResult : dataModelResults) {
                util.write(detailsCSVWriter, dataModelResult);
            }
        } finally {
            if (detailsCSVWriter != null) {
                detailsCSVWriter.flush();
                detailsCSVWriter.close();
            }
        }
    }

    public List<ResultHandler> getResultHandlers() {
        return resultHandlers;
    }
}
