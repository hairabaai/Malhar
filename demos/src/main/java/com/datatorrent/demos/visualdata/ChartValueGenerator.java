/*
 * Copyright (c) 2013 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.demos.visualdata;

import com.datatorrent.api.BaseOperator;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.InputOperator;

/**
 * Chart value generator.
 */
public class ChartValueGenerator extends BaseOperator implements InputOperator {
    private int value = 50;
    private int randomIncrement = 5;

    public final transient DefaultOutputPort<Integer> output = new DefaultOutputPort<Integer>();

    public ChartValueGenerator() {
    }

    @Override
    public void beginWindow(long windowId) {
    }

    @Override
    public void endWindow() {
        value = nextValue(value);
        output.emit(Integer.valueOf(value));
    }

    @Override
    public void emitTuples() {
    }

    private int nextValue(int oldValue) {
        int nextValue = oldValue + (int) (Math.random() * randomIncrement - randomIncrement / 2);
        nextValue = nextValue < 0 ? 0 : nextValue > 100 ? 0 : nextValue;
        return nextValue;
    }

    public void setRandomIncrement(int increment) {
        randomIncrement = increment;
    }
}
