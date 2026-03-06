# !wget -q https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
# !dpkg -i cloudflared-linux-amd64.deb


import re
import time
import subprocess
import psutil
import torch
from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline

app = FastAPI()

print("Loading Models onto GPU...")
# Load onto GPU (device=0)
intent_classifier = pipeline(
    "zero-shot-classification", model="valhalla/distilbart-mnli-12-1", device=0)
ner_pipeline = pipeline("ner", model="dslim/bert-base-NER",
                        aggregation_strategy="simple", device=0)
print("Models Ready!")


class TweetRequest(BaseModel):
    text: str


@app.post("/extract")
def extract_nlp(req: TweetRequest):
    # Intent
    intents = ["complaint", "praise", "question", "spam", "purchase_intent"]
    intent_result = intent_classifier(req.text, candidate_labels=intents)
    top_intent = intent_result['labels'][0]

    # Entities
    entities = ner_pipeline(req.text)
    brands = list(
        set([e['word'] for e in entities if e['entity_group'] in ['ORG', 'MISC']]))

    return {
        "intent": top_intent,
        "entities": brands
    }


@app.get("/health")
def health_check():
    """
    Health check endpoint that returns system and GPU metrics for monitoring.
    """
    # 1. System Memory (RAM)
    vm = psutil.virtual_memory()
    ram_gb = {
        "total": round(vm.total / (1024 ** 3), 2),
        "used": round(vm.used / (1024 ** 3), 2),
        "free": round(vm.available / (1024 ** 3), 2),
        "percent": vm.percent
    }

    # 2. CPU Usage
    cpu_percent = psutil.cpu_percent(interval=0.1)

    # 3. GPU Metrics (If available)
    gpu_stats = {"available": False}
    if torch.cuda.is_available():
        # Get the currently active GPU device
        device = torch.cuda.current_device()

        # Calculate memory in GB
        total_vram = torch.cuda.get_device_properties(
            device).total_memory / (1024 ** 3)
        allocated_vram = torch.cuda.memory_allocated(device) / (1024 ** 3)
        reserved_vram = torch.cuda.memory_reserved(device) / (1024 ** 3)

        gpu_stats = {
            "available": True,
            "device_name": torch.cuda.get_device_name(device),
            "vram_gb": {
                "total": round(total_vram, 2),
                # Memory actually holding the model weights
                "allocated": round(allocated_vram, 2),
                # Memory reserved by PyTorch caching allocator
                "reserved": round(reserved_vram, 2),
                "free": round(total_vram - reserved_vram, 2)
            }
        }

    return {
        "status": "healthy",
        "models_loaded": True,
        "system_metrics": {
            "cpu_usage_percent": cpu_percent,
            "ram_gb": ram_gb,
            "gpu": gpu_stats
        }
    }


# 1. Start FastAPI in the background
subprocess.Popen(["uvicorn", "main:app", "--host",
                 "0.0.0.0", "--port", "8000"])
print("Starting FastAPI... waiting 5 seconds for models to load.")
time.sleep(5)

# 2. Start Cloudflare Tunnel and pipe all logs to a text file
print("Starting Cloudflare Tunnel...")
get_ipython().system(
    'nohup cloudflared tunnel --url http://localhost:8000 > tunnel.log 2>&1 &')

# Wait for Cloudflare to assign the URL
time.sleep(8)

# 3. Read the log file and extract the URL
try:
    with open("tunnel.log", "r") as f:
        log_content = f.read()

        # Look for the specific trycloudflare.com URL pattern
        match = re.search(
            r"https://[a-zA-Z0-9-]+\.trycloudflare\.com", log_content)

        if match:
            url = match.group(0)
            print("\n" + "="*60)
            print(f" YOUR PUBLIC API URL: {url}")
            print(f" SWAGGER UI (TEST IT HERE): {url}/docs")
            print("="*60 + "\n")
        else:
            print(
                "Could not find the URL. Cloudflare might be blocking the connection. Here are the logs:")
            print(log_content)

except FileNotFoundError:
    print("Log file was not created. Cloudflared failed to start.")
