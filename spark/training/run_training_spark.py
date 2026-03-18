from pyspark.sql import SparkSession
from pyspark.ml import Pipeline
from pyspark.ml.feature import (
    Tokenizer,
    StopWordsRemover,
    CountVectorizer,
    IDF,
    StringIndexer,
    IndexToString
)
from pyspark.ml.classification import LogisticRegression
from pyspark.ml.evaluation import MulticlassClassificationEvaluator
from pyspark.sql.functions import col, lower, regexp_replace, when

def main():

    spark = (
        SparkSession.builder
        .appName("SentimentTrainingSparkML")
        .getOrCreate()
    )

    spark.sparkContext.setLogLevel("WARN")

    # ✅ ALWAYS use shared volume
    base_path = "/opt/spark/work-dir/data"
    train_path = f"{base_path}/twitter/twitter_training.csv"
    validation_path = f"{base_path}/twitter/twitter_validation.csv"
    model_output_path = f"{base_path}/spark_sentiment_model"

    print(f"Training data: {train_path}")
    print(f"Validation data: {validation_path}")
    print(f"Model output: {model_output_path}")

    # ===============================
    # LOAD DATA
    # ===============================
    df = spark.read.csv(train_path, header=False, inferSchema=True) \
        .toDF("id", "entity", "sentiment", "text")

    df = df.withColumnRenamed("sentiment", "target") \
           .withColumnRenamed("text", "cleaned_text") \
           .dropna(subset=["cleaned_text", "target"])

    df = df.withColumn(
        "target",
        when(col("target") == "Irrelevant", "Neutral").otherwise(col("target"))
    )

    # ===============================
    # CLEAN TEXT
    # ===============================
    def clean(df):
        return (df
            .withColumn("cleaned_text", lower(col("cleaned_text")))
            .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"http\S+", ""))
            .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"@\w+", ""))
            .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"[^a-zA-Z\s]", ""))
            .withColumn("cleaned_text", regexp_replace(col("cleaned_text"), r"\s+", " "))
        )

    df = clean(df)

    val_df = spark.read.csv(validation_path, header=False, inferSchema=True) \
        .toDF("id", "entity", "sentiment", "text")

    val_df = val_df.withColumnRenamed("sentiment", "target") \
                   .withColumnRenamed("text", "cleaned_text") \
                   .dropna(subset=["cleaned_text", "target"])

    val_df = val_df.withColumn(
        "target",
        when(col("target") == "Irrelevant", "Neutral").otherwise(col("target"))
    )

    val_df = clean(val_df)

    # ===============================
    # LABELS
    # ===============================
    label_indexer = StringIndexer(
        inputCol="target",
        outputCol="label_indexed",
        handleInvalid="skip"
    )

    label_model = label_indexer.fit(df)
    df = label_model.transform(df)
    val_df = label_model.transform(val_df)

    label_converter = IndexToString(
        inputCol="prediction",
        outputCol="predicted_label",
        labels=label_model.labels
    )

    # ===============================
    # PIPELINE
    # ===============================
    pipeline = Pipeline(stages=[
        Tokenizer(inputCol="cleaned_text", outputCol="tokens"),
        StopWordsRemover(inputCol="tokens", outputCol="filtered_tokens"),
        CountVectorizer(inputCol="filtered_tokens", outputCol="raw_features"),
        IDF(inputCol="raw_features", outputCol="features"),
        LogisticRegression(featuresCol="features", labelCol="label_indexed"),
        label_converter
    ])

    # ===============================
    # TRAIN
    # ===============================
    print("Training model...")
    model = pipeline.fit(df)
    print("Training complete")

    # ===============================
    # EVALUATE
    # ===============================
    predictions = model.transform(val_df)

    evaluator = MulticlassClassificationEvaluator(
        labelCol="label_indexed",
        predictionCol="prediction"
    )

    print(f"Accuracy: {evaluator.evaluate(predictions, {evaluator.metricName: 'accuracy'}):.4f}")
    print(f"F1 Score: {evaluator.evaluate(predictions, {evaluator.metricName: 'f1'}):.4f}")

    # ===============================
    # SAVE (CRITICAL FIX)
    # ===============================
    print("Saving model...")
    model.write().overwrite().save(model_output_path)
    print("Model saved successfully!")

    spark.stop()


if __name__ == "__main__":
    main()