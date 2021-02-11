// Databricks notebook source
// STARTER CODE - DO NOT EDIT THIS CELL
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import spark.implicits._
import org.apache.spark.sql.expressions.Window

// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
val customSchema = StructType(Array(StructField("lpep_pickup_datetime", StringType, true), StructField("lpep_dropoff_datetime", StringType, true), StructField("PULocationID", IntegerType, true), StructField("DOLocationID", IntegerType, true), StructField("passenger_count", IntegerType, true), StructField("trip_distance", FloatType, true), StructField("fare_amount", FloatType, true), StructField("payment_type", IntegerType, true)))

// COMMAND ----------

// STARTER CODE - YOU CAN LOAD ANY FILE WITH A SIMILAR SYNTAX.
val df = spark.read
   .format("com.databricks.spark.csv")
   .option("header", "true") // Use first line of all files as header
   .option("nullValue", "null")
   .schema(customSchema)
   .load("/FileStore/tables/nyc_tripdata.csv") // the csv file which you want to work with
   .withColumn("pickup_datetime", from_unixtime(unix_timestamp(col("lpep_pickup_datetime"), "MM/dd/yyyy HH:mm")))
   .withColumn("dropoff_datetime", from_unixtime(unix_timestamp(col("lpep_dropoff_datetime"), "MM/dd/yyyy HH:mm")))
   .drop($"lpep_pickup_datetime")
   .drop($"lpep_dropoff_datetime")


// COMMAND ----------

// LOAD THE "taxi_zone_lookup.csv" FILE SIMILARLY AS ABOVE. CAST ANY COLUMN TO APPROPRIATE DATA TYPE IF NECESSARY.

// ENTER THE CODE BELOW
val df_lookup = spark.read
   .format("com.databricks.spark.csv")
   .option("header", "true") // Use first line of all files as header
   .option("nullValue", "null")
   .load("/FileStore/tables/taxi_zone_lookup.csv") // the csv file which you want to work with
   .withColumn("LocationID",col("LocationID").cast(IntegerType))



// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
// Some commands that you can use to see your dataframes and results of the operations. You can comment the df.show(5) and uncomment display(df) to see the data differently. You will find these two functions useful in reporting your results.
// display(df)
df.show(5) // view the first 5 rows of the dataframe


// COMMAND ----------

// STARTER CODE - DO NOT EDIT THIS CELL
// Filter the data to only keep the rows where "PULocationID" and the "DOLocationID" are different and the "trip_distance" is strictly greater than 2.0 (>2.0).

// VERY VERY IMPORTANT: ALL THE SUBSEQUENT OPERATIONS MUST BE PERFORMED ON THIS FILTERED DATA

val df_filter = df.filter($"PULocationID" =!= $"DOLocationID" && $"trip_distance" > 2.0)
df_filter.show(5)


// COMMAND ----------

// PART 1a: The top-5 most popular drop locations - "DOLocationID", sorted in descending order - if there is a tie, then one with lower "DOLocationID" gets listed first
// Output Schema: DOLocationID int, number_of_dropoffs int 

// Hint: Checkout the groupBy(), orderBy() and count() functions.

// ENTER THE CODE BELOW
var df_1a = df_filter.groupBy($"DOLocationID").agg(count($"DOLocationID") as "number_of_dropoffs").orderBy($"number_of_dropoffs".desc, $"DOLocationID".asc).limit(5)

df_1a.show()


// COMMAND ----------

// PART 1b: The top-5 most popular pickup locations - "PULocationID", sorted in descending order - if there is a tie, then one with lower "PULocationID" gets listed first 
// Output Schema: PULocationID int, number_of_pickups int

// Hint: Code is very similar to part 1a above.

// ENTER THE CODE BELOW
var df_1b = df_filter.groupBy($"PULocationID").agg(count($"PULocationID") as "number_of_pickups").orderBy($"number_of_pickups".desc, $"PULocationID".asc).limit(5)

df_1b.show()


// COMMAND ----------

// PART 2: List the top-3 locations with the maximum overall activity, i.e. sum of all pickups and all dropoffs at that LocationID. In case of a tie, the lower LocationID gets listed first.
// Output Schema: LocationID int, number_activities int

// Hint: In order to get the result, you may need to perform a join operation between the two dataframes that you created in earlier parts (to come up with the sum of the number of pickups and dropoffs on each location). 

// ENTER THE CODE BELOW

var df_2a = df_filter.groupBy($"DOLocationID").agg(count($"DOLocationID") as "number_of_dropoffs").orderBy($"number_of_dropoffs".desc, $"DOLocationID".asc)


var df_2b = df_filter.groupBy($"PULocationID").agg(count($"PULocationID") as "number_of_pickups").orderBy($"number_of_pickups".desc, $"PULocationID".asc)



val df_2_temp = df_2a.join(df_2b, $"DOLocationID" === $"PULocationID")                
                .withColumn("LocationID", col("DOLocationID"))
                .withColumn("number_activities" , col("number_of_dropoffs")+ col("number_of_pickups"))
                .orderBy($"number_activities".desc,$"LocationID".asc)

var df_2 = df_2_temp.select($"LocationID",$"number_activities").limit(3)

df_2.show()

// COMMAND ----------

// PART 3: List all the boroughs in the order of having the highest to lowest number of activities (i.e. sum of all pickups and all dropoffs at that LocationID), along with the total number of activity counts for each borough in NYC during that entire period of time.
// Output Schema: Borough string, total_number_activities int

// Hint: You can use the dataframe obtained from the previous part, and will need to do the join with the 'taxi_zone_lookup' dataframe. Also, checkout the "agg" function applied to a grouped dataframe.

// ENTER THE CODE BELOW

val df_3 = df_2_temp.join(df_lookup, df_lookup("LocationID") === df_2_temp("LocationID"))
                    .groupBy($"Borough")
                    .agg(sum($"number_activities") as "total_number_activities")
                    .orderBy($"total_number_activities".desc)

df_3.show()

// COMMAND ----------

// PART 4: List the top 2 days of week with the largest number of (daily) average pickups, along with the values of average number of pickups on each of the two days. The day of week should be a string with its full name, for example, "Monday" - not a number 1 or "Mon" instead.
// Output Schema: day_of_week string, avg_count float

// Hint: You may need to group by the "date" (without time stamp - time in the day) first. Checkout "to_date" function.

// ENTER THE CODE BELOW
val df_4_temp = df_filter.select($"pickup_datetime")
                  .withColumn("date",to_date(col("pickup_datetime")))
                  .groupBy("date").agg(count($"date") as "count")
                  .withColumn("day_of_week",date_format(col("date"), "EEEE"))
                  

val df_4 = df_4_temp.groupBy("day_of_week").agg(avg($"count") as "avg_count").orderBy($"avg_count".desc).limit(2)
              
                  
                   

df_4.show()


// COMMAND ----------

// PART 5: For each particular hour of a day (0 to 23, 0 being midnight) - in their order from 0 to 23, find the zone in Brooklyn borough with the LARGEST number of pickups. 
// Output Schema: hour_of_day int, zone string, max_count int

// Hint: You may need to use "Window" over hour of day, along with "group by" to find the MAXIMUM count of pickups

// ENTER THE CODE BELOW

// create the dataframe for each hour of the day, each zone and its pickup count
val df_5_temp = df_filter.join(df_lookup, $"PULocationID" === $"LocationID").select($"PULocationID",$"LocationID",$"pickup_datetime",$"Borough",$"Zone")
                         .withColumn("hour",hour(col("pickup_datetime")))
                         .filter($"Borough" === "Brooklyn")
                         .groupBy("hour","zone").agg(count($"hour") as "count")
                         .orderBy($"hour".asc)

//create the dataframe for the max count for each hour of the date. Note need to rename the "hour" column to avoid ambigious for the system
val df_5_temp2 = df_5_temp.groupBy("hour").agg(max($"count") as "max_count")
                          .withColumn("hour_of_day",col("hour"))
                          .drop($"hour")

//join the temp and temp2 dataframe to add in the zone column
val df_5 = df_5_temp2.join(df_5_temp,$"hour" === $"hour_of_day" && $"count" === $"max_count")
                     .select($"hour_of_day",$"zone",$"max_count" )
                     .orderBy($"hour_of_day")
                          
val df_6 = df_5_temp2.join(df_5_temp,$"hour" === $"hour_of_day" && $"count" === $"max_count")
                     .select($"zone")
                     .orderBy($"hour_of_day")
                          
df_5.show(24)


// COMMAND ----------

// PART 6 - Find which 3 different days of the January, in Manhattan, saw the largest percentage increment in pickups compared to previous day, in the order from largest increment % to smallest increment %. 
// Print the day of month along with the percent CHANGE (can be negative), rounded to 2 decimal places, in number of pickups compared to previous day.
// Output Schema: day int, percent_change float


// Hint: You might need to use lag function, over a window ordered by day of month.

// ENTER THE CODE BELOW


val df_6_temp3 = df_filter.select($"pickup_datetime",$"PULocationID")
                         .withColumn("month",month(col("pickup_datetime")))
                         .withColumn("day",dayofmonth(col("pickup_datetime")))
                        .filter($"month" === "1")
                        .join(df_lookup, $"PULocationID" === $"LocationID")
                        .filter($"Borough" === "Manhattan")
                        .groupBy("day").agg(count("day") as "count")


//order by day of month to get previous count.
//For first row we will get NULL
//use lag to get previous row value for count, 1 is the offset
val window = Window.orderBy($"day")

val prior_day = lag(col("count"),1).over(window)

val df_6 = df_6_temp3.withColumn("prior day count", prior_day)
                     .withColumn("percent_change", round((col("count")-col("prior day count"))/col("prior day count") *100,2) )
                     .orderBy($"percent_change".desc)
                     .select($"day",$"percent_change")
                     .limit(3)
                                           
df_6.show()
