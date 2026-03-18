from pyspark.ml import PipelineModel
from pyspark.ml.functions import vector_to_array
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json, to_json, struct, lower, regexp_replace
from pyspark.sql.types import StructType, StructField, StringType, LongType
import os

# ===============================
# SPARK SESSION
# ===============================
spark = (
    SparkSession.builder
    .appName("Kafka-Spark-Streaming-Sentiment")
    .config("spark.shuffle.service.enabled", "false")
    .config("spark.dynamicAllocation.enabled", "false")
    .getOrCreate()
)

spark.sparkContext.setLogLevel("WARN")

# ===============================
# MODEL PATH (SHARED VOLUME)
# ===============================
model_path = "/opt/spark/work-dir/data/spark_sentiment_model"

if not os.path.exists(model_path + "/metadata"):
    raise RuntimeError(
        f"Model not found at {model_path}. Run training first."
    )

model = PipelineModel.load(model_path)
print("✓ Model loaded")

# ===============================
# KAFKA SOURCE
# ===============================
kafka_server = "broker:9092"

kafka_df = (
    spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", kafka_server)
    .option("subscribe", "tweets.raw")
    .option("startingOffsets", "latest")
    .load()
)

# ===============================
# SCHEMA
# ===============================
schema = StructType([
    StructField("tweetId", StringType()),
    StructField("text", StringType()),
    StructField("timestamp", LongType())
])

parsed_df = kafka_df.selectExpr("CAST(value AS STRING)") \
    .select(from_json(col("value"), schema).alias("data")) \
    .select("data.*")

# ===============================
# CLEAN TEXT (SAME AS TRAINING)
# ===============================
parsed_df = parsed_df.withColumnRenamed("text", "cleaned_text")

parsed_df = (parsed_df
    .withColumn("cleaned_text", lower(col("cleaned_text")))
    .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"http\S+", ""))
    .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"@\w+", ""))
    .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"[^a-zA-Z\s]", ""))
    .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"\s+", " "))
)

# ===============================
# PREDICTIONS
# ===============================
predictions = model.transform(parsed_df)

predictions = predictions.withColumn(
    "prob_array",
    vector_to_array(col("probability"))
)

output_df = predictions.select(
    col("tweetId"),
    col("predicted_label").alias("sentiment"),
    col("prob_array")[col("prediction").cast("int")].alias("score")
)

# ===============================
# SEND TO KAFKA
# ===============================
kafka_output = output_df.select(
    to_json(struct("tweetId", "sentiment", "score")).alias("value")
)

query = (
    kafka_output.writeStream
    .format("kafka")
    .option("kafka.bootstrap.servers", kafka_server)
    .option("topic", "tweets.processed")
    .option("checkpointLocation", "/tmp/spark_checkpoint")
    .outputMode("append")
    .start()
)

print("✓ Streaming started")
query.awaitTermination()