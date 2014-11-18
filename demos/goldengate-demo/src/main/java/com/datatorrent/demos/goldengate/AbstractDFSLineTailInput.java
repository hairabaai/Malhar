/*
 * Copyright (c) 2014 DataTorrent, Inc. ALL Rights Reserved.
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
package com.datatorrent.demos.goldengate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.datatorrent.api.Context;
import com.datatorrent.api.InputOperator;
import com.datatorrent.api.Operator;

import com.datatorrent.common.util.DTThrowable;

/**
 * This operator tails a file to get latest newly added lines.
 */
public abstract class AbstractDFSLineTailInput implements InputOperator, Operator.ActivationListener<Context.OperatorContext>
{
  private static final Logger logger = LoggerFactory.getLogger(AbstractDFSLineTailInput.class);

  @NotNull
  private String filePath;
  private transient Runnable fileReader;
  private transient Thread fileHelperTh;
  private volatile boolean fileThStop;
  private transient FileSystem fs;
  private transient Path path;
  private transient FSDataInputStream input;
  private transient BufferedReader bufferedReader;
  private ArrayBlockingQueue<String> lines;
  private int lineBufferCapacity = 100;
  private int maxLineEmit = 100;

  public AbstractDFSLineTailInput() {
    fileReader = new FileReader();
    fileHelperTh = new Thread(fileReader);
  }

  @Override
  public void setup(Context.OperatorContext context)
  {
    Configuration conf = new Configuration();

    try {
      fs = FileSystem.get(conf);
      path = new Path(filePath);
    } catch (IOException e) {
      DTThrowable.rethrow(e);
    }
    lines = new ArrayBlockingQueue<String>(lineBufferCapacity);
  }

  @Override
  public void activate(Context.OperatorContext ctx)
  {
    try {
      openFile();
    } catch (IOException e) {
      DTThrowable.rethrow(e);
    }
    fileThStop = false;
    fileHelperTh.start();
  }

  @Override
  public void deactivate()
  {
    fileThStop = true;
    try {
      fileHelperTh.join();
    } catch (InterruptedException e) {
      logger.error("Wait interrupted", e);
      DTThrowable.rethrow(e);
    } finally {
      try {
        bufferedReader.close();
      } catch (IOException e) {
        DTThrowable.rethrow(e);
      }
    }
  }

  @Override
  public void teardown()
  {
    try {
      fs.close();
    } catch (IOException e) {
      DTThrowable.rethrow(e);
    }
  }

  private class FileReader implements Runnable {
    @Override
    public void run()
    {
      while (!fileThStop) {
        try {
          String line = bufferedReader.readLine();
          if (line != null) {
            logger.info("line {}", line);
            lines.add(line);
          } else {
            openFile();
          }
        } catch (IOException e) {
          DTThrowable.rethrow(e);
        }
      }
    }
  }

  private void openFile() throws IOException
  {
    long filepos = 0;
    if (input != null) {
      filepos = input.getPos();
      logger.info("file position {}", filepos);
      bufferedReader.close();
      // Wait a second before reopening file
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
    input = fs.open(path);
    input.seek(filepos);
    bufferedReader = new BufferedReader(new InputStreamReader(input));
  }

  @Override
  public void beginWindow(long l)
  {
    //Do nothing
  }

  @Override
  public void endWindow()
  {
    //Do nothing
  }

  @Override
  public void emitTuples()
  {
    String line = null;
    int count = 0;
    while (((line = lines.poll()) != null) && (count < maxLineEmit)) {
      processLine(line);
      ++count;
    }
    if (count == 0) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        DTThrowable.rethrow(e);
      }
    }
  }

  /**
   * This method gets called every time a line is retreived from the target
   * file.
   * @param line A newly read line from the file.
   */
  protected abstract void processLine(String line);

  /**
   * Gets the path of the file that is being tailed.
   * @return The path of the file that is being tailed.
   */
  public String getFilePath()
  {
    return filePath;
  }

  /**
   * Sets the path of the file that is being tailed.
   * @param filePath The path of the file that is being tailed.
   */
  public void setFilePath(String filePath)
  {
    this.filePath = filePath;
  }

  /**
   * Gets the maximum number of lines emitted in each call to emittuples.
   * @return The maximum number of lines emitted in each call to emittuples.
   */
  public int getMaxLineEmit()
  {
    return maxLineEmit;
  }

  /**
   * Sets the maximum number of lines emitted in each call to emittuples.
   * @param maxLineEmit The maximum number of lines emitted in each call to emittuples.
   */
  public void setMaxLineEmit(int maxLineEmit)
  {
    this.maxLineEmit = maxLineEmit;
  }

  /**
   * Sets the maximum number of lines the worker thread can read and buffer at one time
   * from the target file.
   * @return The maximum number of lines the worker thread can read and buffer at one time
   * from the target file.
   */
  public int getLineBufferCapacity()
  {
    return lineBufferCapacity;
  }

  /**
   * sets the maximum number of lines the worker thread can read and buffer at one time
   * from the target file.
   * @param lineBufferCapacity The maximum number of lines the worker thread can read and
   * buffer at one time from the target file.
   */
  public void setLineBufferCapacity(int lineBufferCapacity)
  {
    this.lineBufferCapacity = lineBufferCapacity;
  }
}
