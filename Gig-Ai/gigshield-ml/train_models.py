import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, IsolationForest
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline
import joblib
import os

MODEL_DIR = os.path.join(os.path.dirname(__file__), "models")
os.makedirs(MODEL_DIR, exist_ok=True)

# ─────────────────────────────────────────────
# 1. RISK PREDICTION MODEL (Random Forest)
#    Input:  rainfall, temperature, aqi
#    Output: 0=LOW, 1=MEDIUM, 2=HIGH
# ─────────────────────────────────────────────
def train_risk_model():
    np.random.seed(42)
    n = 2000

    rainfall    = np.random.exponential(5, n)
    temperature = np.random.normal(30, 8, n)
    aqi         = np.random.exponential(80, n)

    # Label logic matching business rules + noise
    labels = []
    for r, t, a in zip(rainfall, temperature, aqi):
        score = 0
        if r > 10:  score += 2
        if t > 40:  score += 2
        if a > 150: score += 2
        if r > 5:   score += 1
        if t > 35:  score += 1
        if a > 100: score += 1
        # add slight noise
        score += np.random.randint(-1, 2)
        if score >= 4:   labels.append(2)   # HIGH
        elif score >= 2: labels.append(1)   # MEDIUM
        else:            labels.append(0)   # LOW

    X = np.column_stack([rainfall, temperature, aqi])
    y = np.array(labels)

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("clf", RandomForestClassifier(n_estimators=100, max_depth=8, random_state=42))
    ])
    model.fit(X, y)
    joblib.dump(model, os.path.join(MODEL_DIR, "risk_model.pkl"))
    print("Risk model trained and saved")
    return model


# ─────────────────────────────────────────────
# 2. FRAUD DETECTION MODEL (Isolation Forest)
#    Input:  claims_last_7_days, claim_amount,
#            days_since_policy_start, aqi, rainfall
#    Output: -1=fraud, 1=normal
# ─────────────────────────────────────────────
def train_fraud_model():
    np.random.seed(42)
    n = 1500

    # Normal claims
    normal = np.column_stack([
        np.random.randint(0, 3, n),          # claims_last_7_days
        np.random.uniform(100, 900, n),      # claim_amount
        np.random.randint(1, 30, n),         # days_since_policy_start
        np.random.exponential(80, n),        # aqi
        np.random.exponential(5, n),         # rainfall
    ])

    model = IsolationForest(contamination=0.08, random_state=42)
    model.fit(normal)
    joblib.dump(model, os.path.join(MODEL_DIR, "fraud_model.pkl"))
    print("Fraud model trained and saved")
    return model


# ─────────────────────────────────────────────
# 3. PAYOUT AMOUNT MODEL (Random Forest Regressor)
#    Input:  risk_level(0-2), rainfall, temp,
#            aqi, coverage_amount, plan_type(0-2)
#    Output: recommended payout amount
# ─────────────────────────────────────────────
def train_payout_model():
    from sklearn.ensemble import RandomForestRegressor
    np.random.seed(42)
    n = 2000

    risk_level     = np.random.randint(0, 3, n)
    rainfall       = np.random.exponential(5, n)
    temperature    = np.random.normal(30, 8, n)
    aqi            = np.random.exponential(80, n)
    coverage       = np.random.choice([500, 1500, 3000], n)
    plan_type      = np.where(coverage == 500, 0, np.where(coverage == 1500, 1, 2))

    # Payout = base % of coverage based on severity
    base_pct = 0.20 + (risk_level * 0.08)
    rain_bonus = np.minimum(rainfall / 100, 0.10)
    aqi_bonus  = np.minimum(aqi / 2000, 0.05)
    payout = coverage * (base_pct + rain_bonus + aqi_bonus)
    payout = np.clip(payout, 50, coverage * 0.5)

    X = np.column_stack([risk_level, rainfall, temperature, aqi, coverage, plan_type])

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("reg", RandomForestRegressor(n_estimators=100, max_depth=8, random_state=42))
    ])
    model.fit(X, payout)
    joblib.dump(model, os.path.join(MODEL_DIR, "payout_model.pkl"))
    print("Payout model trained and saved")
    return model


if __name__ == "__main__":
    print("Training all GigShield AI/ML models...")
    train_risk_model()
    train_fraud_model()
    train_payout_model()
    print("All models trained successfully! Saved to ./models/")
