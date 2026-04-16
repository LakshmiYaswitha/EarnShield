# GigShield
Smart Income Protection for Delivery Partners

## Overview

EarnShield is a parametric insurance system that **helps protect delivery partners against income loss resulting from external disruptions such as heavy rain, extreme heat, and pollution.**

This system **provides weekly micro-insurance to delivery partners and triggers claims and pays out instantly.**

---

## Problem Statement

Delivery partners of food delivery companies such as Swiggy, Zomato, and Zepto earn their daily income through these platforms. However, **disruptions such as heavy rainfall, extreme heat, and pollution cause 20-30% income loss.**

But currently, **there is no system to protect their income.**

---

## Target Persona

**Food Delivery Partners**

**Profile:**

*   Work for 8 to 10 hours per day
*   Earn ₹3,500 to ₹5,000 per week
*   Get paid once per week

**Scenario:**

*   Heavy rain disrupts delivery for 4 to 5 hours
*   Worker loses ₹600 to ₹800
*   EarnShield pays this loss to the worker.**

---

## Solution

EarnShield is a system that **provides weekly income protection plans to delivery partners and ensures automatic and instant payments.**

This system **uses AI-based systems to assess risks and monitor situations to ensure fast and fair compensation.**

---

## Application Workflow

User Registration

Name, phone, platform, city, working zone

AI Risk Profiling

Analyze weather history and disruption patterns

Risk score generated (Low, Medium, High)

Weekly Policy Activation

Premium is dynamically calculated

User activates plan

Real-Time Monitoring

APIs for tracking weather and environmental changes

Automatic Claim Trigger

Triggers claim when disruptions cross a threshold

Instant Payout

Compensation is sent via UPI or payment gateway

---

## Parametric Triggers
Event	Condition
Heavy Rain	Rainfall > 50mm
Extreme Heat	Temperature > 42°C
Pollution	AQI > 350
Flood	Government alert
- Weekly Premium Model

Premium is calculated weekly and is a function of Base + Risk Adjustment

Risk Level	Premium
Low Risk	₹15/week
Medium Risk	₹20/week
High Risk	₹30/week

- Designed to match weekly earning cycle

---

## AI-Powered Intelligence

EarnShield leverages AI and machine learning for better pricing, automation, and fraud detection.The model has the ability to take environmental disruption data, as well as historical disruption data, as inputs, then provide a risk score as output.

AI Models :

Random Forest → Risk prediction

Isolation Forest → Fraud detection (anomaly detection)

1. **Risk Prediction**

Analyzes:

Rainfall history

Temperature patterns

Pollution levels

Zone-based disruptions

Generates a risk score for better pricing.

2. **Dynamic Premium Calculation**

Premium amount is automatically adjusted according to risk levels:

Low risk: Lower premium

High risk: Higher premium

3. **Fraud Detection**

Detects:

GPS spoofing

No movement activity

Sudden abnormal claims

Group fraud patterns

Identifies suspicious claims for verification.

4. **Smart Trigger Optimization**

Optimizes triggering of claims using historical data patterns.

---

## Adversarial Defense & Anti-Spoofing Strategy

EarnShield avoids GPS spoofing and other types of fraud by using behavior-based validation instead of depending on location.

- **Detection Logic**

EarnShield checks:

Is the user actively working?

Validate movement patterns

Identify abnormal location jumps

- **Additional Data Used**

EarnShield also uses:

Delivery activity

Consistency in movement

Location from network vs GPS

Actual weather conditions in area

- **Handling Suspicious Claims**

For genuine users:

Immediate payout

For suspicious users:

Verification

- **Group Fraud Detection**

EarnShield identifies:

Multiple users claiming from same area

Applies stricter verification for them

---

## System Architecture
User → Frontend → Backend → Weather API → AI Model → Trigger Engine → Claims → Payments

---

## Tech Stack

**Frontend**: React JS

**Backend**: Python
-The backend combines the data from the API with the AI models, which automatically trigger the claim and payout.

**Database**: MongoDB

**AI/ML**: Scikit-learn, Pandas, NumPy 

**APIs**: OpenWeather API, Location Validation, Delivery Platform API 

**Payments**: Razorpay (Test Mode)

---

**Weekly Risk Insight**
The user can view upcoming risk levels (Rain, Heat, Pollution, etc.) and can choose better coverage plans.
is this okay now
