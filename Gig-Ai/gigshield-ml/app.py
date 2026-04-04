from flask import Flask, request, jsonify
import joblib
import numpy as np
import os

app = Flask(__name__)

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")

# Load models
risk_model   = joblib.load(os.path.join(MODEL_DIR, "risk_model.pkl"))
fraud_model  = joblib.load(os.path.join(MODEL_DIR, "fraud_model.pkl"))
payout_model = joblib.load(os.path.join(MODEL_DIR, "payout_model.pkl"))

RISK_LABELS = {0: "LOW", 1: "MEDIUM", 2: "HIGH"}


# ─────────────────────────────────────────────
# HEALTH CHECK
# ─────────────────────────────────────────────
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "models": ["risk", "fraud", "payout"]})


# ─────────────────────────────────────────────
# 1. RISK PREDICTION
# POST /predict/risk
# Body: { "rainfall": 12.5, "temperature": 38.0, "aqi": 160 }
# ─────────────────────────────────────────────
@app.route("/predict/risk", methods=["POST"])
def predict_risk():
    data = request.get_json()
    rainfall    = float(data.get("rainfall", 0))
    temperature = float(data.get("temperature", 25))
    aqi         = float(data.get("aqi", 50))

    X = np.array([[rainfall, temperature, aqi]])
    pred  = risk_model.predict(X)[0]
    proba = risk_model.predict_proba(X)[0]

    return jsonify({
        "riskLevel":    RISK_LABELS[pred],
        "riskScore":    int(pred),
        "confidence":   round(float(max(proba)) * 100, 2),
        "probabilities": {
            "LOW":    round(float(proba[0]) * 100, 2),
            "MEDIUM": round(float(proba[1]) * 100, 2),
            "HIGH":   round(float(proba[2]) * 100, 2),
        },
        "inputs": {"rainfall": rainfall, "temperature": temperature, "aqi": aqi}
    })


# ─────────────────────────────────────────────
# 2. FRAUD DETECTION
# POST /predict/fraud
# Body: { "claimsLast7Days": 2, "claimAmount": 500,
#         "daysSincePolicyStart": 5, "aqi": 80, "rainfall": 3 }
# ─────────────────────────────────────────────
@app.route("/predict/fraud", methods=["POST"])
def predict_fraud():
    data = request.get_json()
    claims_7d      = float(data.get("claimsLast7Days", 0))
    claim_amount   = float(data.get("claimAmount", 0))
    days_since     = float(data.get("daysSincePolicyStart", 1))
    aqi            = float(data.get("aqi", 50))
    rainfall       = float(data.get("rainfall", 0))

    X = np.array([[claims_7d, claim_amount, days_since, aqi, rainfall]])
    pred  = fraud_model.predict(X)[0]
    score = fraud_model.decision_function(X)[0]

    is_fraud = bool(pred == -1)
    fraud_pct = round(max(0, min(100, (1 - (score + 0.5)) * 100)), 2)

    return jsonify({
        "isFraud":      is_fraud,
        "fraudScore":   fraud_pct,
        "anomalyScore": round(float(score), 4),
        "verdict":      "SUSPICIOUS" if is_fraud else "CLEAN",
        "reason":       _fraud_reason(claims_7d, claim_amount, days_since) if is_fraud else "Normal pattern"
    })


# ─────────────────────────────────────────────
# 3. PAYOUT CALCULATION
# POST /predict/payout
# Body: { "riskLevel": "HIGH", "rainfall": 15, "temperature": 42,
#         "aqi": 180, "coverageAmount": 3000, "planType": "PREMIUM" }
# ─────────────────────────────────────────────
@app.route("/predict/payout", methods=["POST"])
def predict_payout():
    data = request.get_json()
    risk_map  = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
    plan_map  = {"BASIC": 0, "STANDARD": 1, "PREMIUM": 2}

    risk_level   = risk_map.get(data.get("riskLevel", "LOW"), 0)
    rainfall     = float(data.get("rainfall", 0))
    temperature  = float(data.get("temperature", 25))
    aqi          = float(data.get("aqi", 50))
    coverage     = float(data.get("coverageAmount", 500))
    plan_type    = plan_map.get(data.get("planType", "BASIC"), 0)

    X = np.array([[risk_level, rainfall, temperature, aqi, coverage, plan_type]])
    payout = float(payout_model.predict(X)[0])
    payout = round(max(50, min(payout, coverage * 0.5)), 2)

    return jsonify({
        "recommendedPayout": payout,
        "coverageAmount":    coverage,
        "payoutPercentage":  round((payout / coverage) * 100, 2),
        "riskLevel":         data.get("riskLevel", "LOW"),
        "planType":          data.get("planType", "BASIC")
    })


# ─────────────────────────────────────────────
# 4. FULL ANALYSIS (all 3 models in one call)
# POST /predict/analyze
# ─────────────────────────────────────────────
@app.route("/predict/analyze", methods=["POST"])
def analyze():
    data = request.get_json()

    # Risk
    rainfall    = float(data.get("rainfall", 0))
    temperature = float(data.get("temperature", 25))
    aqi         = float(data.get("aqi", 50))
    X_risk = np.array([[rainfall, temperature, aqi]])
    risk_pred  = risk_model.predict(X_risk)[0]
    risk_proba = risk_model.predict_proba(X_risk)[0]
    risk_label = RISK_LABELS[risk_pred]

    # Fraud
    claims_7d    = float(data.get("claimsLast7Days", 0))
    claim_amount = float(data.get("claimAmount", 0))
    days_since   = float(data.get("daysSincePolicyStart", 1))
    X_fraud = np.array([[claims_7d, claim_amount, days_since, aqi, rainfall]])
    fraud_pred  = fraud_model.predict(X_fraud)[0]
    fraud_score = fraud_model.decision_function(X_fraud)[0]
    is_fraud    = bool(fraud_pred == -1)

    # Payout
    risk_map = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
    plan_map = {"BASIC": 0, "STANDARD": 1, "PREMIUM": 2}
    coverage  = float(data.get("coverageAmount", 500))
    plan_type = plan_map.get(data.get("planType", "BASIC"), 0)
    X_payout = np.array([[risk_pred, rainfall, temperature, aqi, coverage, plan_type]])
    payout = float(payout_model.predict(X_payout)[0])
    payout = round(max(50, min(payout, coverage * 0.5)), 2)

    return jsonify({
        "risk": {
            "level":      risk_label,
            "score":      int(risk_pred),
            "confidence": round(float(max(risk_proba)) * 100, 2),
            "probabilities": {
                "LOW":    round(float(risk_proba[0]) * 100, 2),
                "MEDIUM": round(float(risk_proba[1]) * 100, 2),
                "HIGH":   round(float(risk_proba[2]) * 100, 2),
            }
        },
        "fraud": {
            "isFraud":    is_fraud,
            "verdict":    "SUSPICIOUS" if is_fraud else "CLEAN",
            "fraudScore": round(max(0, min(100, (1 - (fraud_score + 0.5)) * 100)), 2)
        },
        "payout": {
            "recommendedPayout": payout,
            "payoutPercentage":  round((payout / coverage) * 100, 2)
        },
        "suggestion": _get_suggestion(risk_label, rainfall, temperature, aqi)
    })


def _fraud_reason(claims_7d, amount, days_since):
    if claims_7d >= 3:   return "Too many claims in 7 days"
    if days_since <= 2:  return "Claim too soon after policy start"
    if amount > 2000:    return "Unusually high claim amount"
    return "Abnormal claim pattern detected"


def _get_suggestion(level, rain, temp, aqi):
    if level == "HIGH":
        if rain > 10:  return "Heavy rain expected. Work early morning to avoid disruption."
        if temp > 40:  return "Extreme heat alert. Stay hydrated and limit midday deliveries."
        if aqi > 150:  return "Poor air quality. Wear a mask and reduce outdoor exposure."
    if level == "MEDIUM":
        return "Moderate risk today. Monitor conditions and plan your shifts carefully."
    return "Conditions look good. Safe to work normally today."


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
