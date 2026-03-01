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
import os


def main():

    spark = (
        SparkSession.builder
        .appName("SentimentTrainingSparkML")
        .getOrCreate()
    )

    spark.sparkContext.setLogLevel("WARN")

    # ===============================
    # PATHS (Docker shared volume)
    # ===============================

    base_path = "/opt/spark/work-dir/data"
    train_path = f"{base_path}/twitter/twitter_training.csv"
    validation_path = f"{base_path}/twitter/twitter_validation.csv"
    model_output_path = f"{base_path}/models/spark_sentiment_model"

    print(f"Loading training data from: {train_path}")
    print(f"Loading validation data from: {validation_path}")

    # ===============================
    # LOAD TRAINING DATA
    # ===============================

    df = spark.read.csv(
        train_path,
        header=False,
        inferSchema=True
    ).toDF("id", "entity", "sentiment", "text")

    df = (
        df.withColumnRenamed("sentiment", "target")
          .withColumnRenamed("text", "cleaned_text")
          .dropna(subset=["cleaned_text", "target"])
    )

    df = df.withColumn(
        "target",
        when(col("target") == "Irrelevant", "Neutral").otherwise(col("target"))
    )

    # ===============================
    # CLEAN TEXT
    # ===============================

    df = df.withColumn("cleaned_text", lower(col("cleaned_text")))
    df = df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"http\S+", ""))
    df = df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"@\w+", ""))
    df = df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"[^a-zA-Z\s]", ""))
    df = df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"\s+", " "))

    # ===============================
    # LOAD VALIDATION DATA
    # ===============================

    val_df = spark.read.csv(
        validation_path,
        header=False,
        inferSchema=True
    ).toDF("id", "entity", "sentiment", "text")

    val_df = (
        val_df.withColumnRenamed("sentiment", "target")
              .withColumnRenamed("text", "cleaned_text")
              .dropna(subset=["cleaned_text", "target"])
    )

    val_df = val_df.withColumn(
        "target",
        when(col("target") == "Irrelevant", "Neutral").otherwise(col("target"))
    )

    val_df = val_df.withColumn("cleaned_text", lower(col("cleaned_text")))
    val_df = val_df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"http\S+", ""))
    val_df = val_df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"@\w+", ""))
    val_df = val_df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"[^a-zA-Z\s]", ""))
    val_df = val_df.withColumn("cleaned_text", regexp_replace(
        col("cleaned_text"), r"\s+", " "))

    # ===============================
    # LABEL INDEXING
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
    # FEATURE PIPELINE
    # ===============================

    tokenizer = Tokenizer(
        inputCol="cleaned_text",
        outputCol="tokens"
    )

    remover = StopWordsRemover(
        inputCol="tokens",
        outputCol="filtered_tokens"
    )

    vectorizer = CountVectorizer(
        inputCol="filtered_tokens",
        outputCol="raw_features",
        vocabSize=5000,
        minDF=5
    )

    idf = IDF(
        inputCol="raw_features",
        outputCol="features"
    )

    lr = LogisticRegression(
        featuresCol="features",
        labelCol="label_indexed",
        maxIter=20,
        regParam=0.01
    )

    pipeline = Pipeline(stages=[
        tokenizer,
        remover,
        vectorizer,
        idf,
        lr,
        label_converter
    ])

    # ===============================
    # TRAIN
    # ===============================

    print("Training model...")
    model = pipeline.fit(df)
    print("Model training complete.")

    # ===============================
    # EVALUATE
    # ===============================

    predictions = model.transform(val_df)

    evaluator = MulticlassClassificationEvaluator(
        labelCol="label_indexed",
        predictionCol="prediction",
        metricName="accuracy"
    )

    accuracy = evaluator.evaluate(predictions)
    f1 = evaluator.setMetricName("f1").evaluate(predictions)

    print(f"Validation Accuracy: {accuracy:.4f}")
    print(f"Validation F1 Score: {f1:.4f}")

    # ===============================
    # SAVE MODEL (CORRECT WAY)
    # ===============================

    print(f"Saving model to {model_output_path}")
    model.write().overwrite().save(model_output_path)

    print("Model saved successfully.")

    spark.stop()


if __name__ == "__main__":
    main()
