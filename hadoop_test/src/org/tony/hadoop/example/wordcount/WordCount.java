/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tony.hadoop.example.wordcount;



import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

/***
 * create class WordCount.class
 * @author Tony
 *
 */
public class WordCount {

  //Map过程需要继承org.apache.hadoop.mapreduce包中Mapper类，并重写其 map方法。
  //通过在map方法中添加两句把key值和value值输出到控制台的代码，
  //可以发现map方法中value值存储的是文本文件中的一行（以回车符为行结束标记），
  //而key值为该行的首字母相对于文本文件的首地址的偏移量。
  //然后StringTokenizer类将每一行拆分成为一个个的单词，并将<word,1>作为map方法的结果输出，其余的工作都交有MapReduce框架处理。
  public static class TokenizerMapper 
       extends Mapper<Object, Text, Text, IntWritable>{
    
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      }
    }
  }
  
  //Reduce过程需要继承org.apache.hadoop.mapreduce包中Reducer类，并重写其reduce方法。
  //Map过程输出<key,values>中key为单个单词，而values是对应单词的计数值所组成的列表，
  //Map的输出就是Reduce的输入，所以reduce方法只要遍历values并求和，即可得到某个单词的总次数。
  public static class IntSumReducer 
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
  }

@SuppressWarnings("deprecation")
public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length < 2) {
      System.err.println("Usage: wordcount <in> [<in>...] <out>");
      System.exit(2);
    }
    //Jobconf类来对MapReduce Job进行初始化，然后调用setJobName()方法命名这个Job。
    //对Job进行合理的命名有助于更快地找到Job，以便在JobTracker和Tasktracker的页面中对其进行监视。
    Job job = new Job(conf, "word count");
    //设置Job处理的Map（拆分）、Combiner（中间结果合并）以及Reduce（合并）的相关处理类。
    //这里用Reduce类来进行Map产生的中间结果合并，避免给网络数据传输产生压力。
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    //设置Job输出结果<key,value>的中key和value数据类型，因为结果是<单词,个数>，
    //所以key设置为"Text"类型，相当于Java中String类型。Value设置为"IntWritable"，
    //相当于Java中的int类型。
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    for (int i = 0; i < otherArgs.length - 1; ++i) {
      //调用setInputPath()和setOutputPath()设置输入输出路径。
      FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
    }
    //调用setInputPath()和setOutputPath()设置输入输出路径。
    FileOutputFormat.setOutputPath(job,
      new Path(otherArgs[otherArgs.length - 1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
