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
package com.datatorrent.lib.algo;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.datatorrent.lib.testbench.CountAndLastTupleTestSink;

/**
 *
 * Functional tests for {@link com.datatorrent.lib.algo.MatchAnyStringMap}<p>
 *
 */
public class MatchAnyStringMapTest
{
  /**
   * Test node logic emits correct results
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testNodeProcessing() throws Exception
  {
    MatchAnyStringMap<String> oper = new MatchAnyStringMap<String>();
    CountAndLastTupleTestSink matchSink = new CountAndLastTupleTestSink();
    oper.any.setSink(matchSink);
    oper.setKey("a");
    oper.setValue(3.0);
    oper.setTypeEQ();

    oper.beginWindow(0);
    HashMap<String, String> input = new HashMap<String, String>();
    input.put("a", "3");
    input.put("b", "20");
    input.put("c", "1000");
    oper.data.process(input);
    input.clear();
    input.put("a", "2");
    oper.data.process(input);
    oper.endWindow();

    Assert.assertEquals("number emitted tuples", 1, matchSink.count);
    Boolean result = (Boolean) matchSink.tuple;
    Assert.assertEquals("result was false", true, result.booleanValue());
    matchSink.clear();

    oper.beginWindow(0);
    input.clear();
    input.put("a", "2");
    input.put("b", "20");
    input.put("c", "1000");
    oper.data.process(input);
    input.clear();
    input.put("a", "5");
    oper.data.process(input);
    oper.endWindow();
    // There should be no emit as all tuples do not match
    Assert.assertEquals("number emitted tuples", 0, matchSink.count);
    matchSink.clear();
}
}
