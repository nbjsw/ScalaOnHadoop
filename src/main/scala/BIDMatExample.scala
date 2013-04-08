import java.io.IOException
import java.util._
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf._
import org.apache.hadoop.io._
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.util._
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat
import BIDMat.MatFunctions._
import BIDMat.{IMat,FMat}
import scala.reflect.Manifest
import java.io.PrintWriter
import org.apache.commons.cli.Options
import BIDMatWithHDFS._;



object BIDMatExample extends Configured with Tool {

  class Map extends Mapper[LongWritable, Text, LongWritable, MatIO] {
    var one: IntWritable = new IntWritable(1);
    var word: Text = new Text();
  	
    override def map(key: LongWritable, value: Text, context: Mapper[LongWritable, Text, LongWritable, MatIO]#Context) {
      var elemArray : Array[String] = (value.toString() split "\t")
		var doubleArray = elemArray map ((elem : String) => elem.toString.toDouble)
		var fMatArray : Array[FMat] = new Array(doubleArray.length)
		var ct = 0
		while (ct < fMatArray.length)
		{
			fMatArray(ct)=doubleArray(ct)
			ct = ct + 1;
		}
		var fMat :FMat = (fMatArray(0) /: fMatArray.slice(1,fMatArray.length) )(_\_)
		var matIO : MatIO = new MatIO()
		//matI0
		matIO.mat = fMat
		context.write(new LongWritable(0), matIO)
    }
  }

  class Reduce extends Reducer[LongWritable, MatIO, LongWritable, Text] {
    def reduce(key: LongWritable, values: Iterator[MatIO], context: Reducer[LongWritable, MatIO, LongWritable, Text]#Context) {
      var sumFMat : FMat = 0\0\0\0
      while (values.hasNext())
      {
      var matIO = values.next();
      var mat = matIO.mat match
      {
        case fMat : FMat => fMat;
      }
      sumFMat += mat
    }
    var sBuilder = (new StringBuilder /: sumFMat.data) ((soFar, newFloat) => soFar.append(newFloat + "\t"))
    var toWrite = new Text(sBuilder.toString())
    context write (key, toWrite)
    }
  }
  
  def run(args: Array[String]) =
  {
		  var conf = super.getConf()
//		  Configuration.dumpConfiguration(conf,new PrintWriter(System.out)) // for verfying your conf file 
//	      println("Libjars: " + conf.get("tmpjars")); //for making sure your jars have been include
	      var job : Job = new Job(conf,"WordCount")
	      job
		  job setJarByClass(this.getClass())
	      job setOutputKeyClass classOf[LongWritable]
	      job setOutputValueClass classOf[Text]

	      job setMapperClass classOf[Map]
	      job setCombinerClass classOf[Reduce]
	      job setReducerClass classOf[Reduce]
  	      
  	      FileInputFormat.addInputPath(job, new Path(args(1)))
  	      FileOutputFormat.setOutputPath(job, new Path(args(2)))
  	      job waitForCompletion(true) match {
		    case true => 0
		    case false => 1
		  }
   }
  
  def main(args: Array[String]) {
    var  c : Configuration = new Configuration()
    var res : Int = ToolRunner.run(c,this, args)
    System.exit(res);
  }
  
}