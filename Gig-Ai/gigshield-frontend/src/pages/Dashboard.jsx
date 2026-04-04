import { useEffect, useState, useCallback } from 'react';
import { dashboardAPI, mlAPI } from '../api';
import { RiskBadge, StatCard, Spinner } from '../components/UI';
import {
  Shield, Wallet, FileText, TrendingUp,
  CloudRain, Thermometer, Wind, Lightbulb, RefreshCw
} from 'lucide-react';

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [mlData, setMlData] = useState(null);

  const fetchDashboard = useCallback((silent = false) => {
    if (!silent) setLoading(true);
    else setRefreshing(true);
    Promise.all([
      dashboardAPI.get(),
      mlAPI.analyze().catch(() => null)
    ])
      .then(([dash, ml]) => {
        setData(dash.data);
        if (ml) setMlData(ml.data);
      })
      .catch(() => {})
      .finally(() => { setLoading(false); setRefreshing(false); });
  }, []);

  useEffect(() => { fetchDashboard(); }, [fetchDashboard]);

  if (loading) return <Spinner />;
  if (!data) return <p className="text-gray-500 p-6">Failed to load dashboard.</p>;

  const risk = data.currentRisk;

  return (
    <div className="relative min-h-screen">
      {/* Background */}
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl" />
        <div className="absolute top-1/2 left-0 w-64 h-64 bg-cyan-600/5 rounded-full blur-3xl" />
      </div>

      <div className="p-6 space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-bold text-white">Welcome back, {data.name} 👋</h2>
            <p className="text-gray-500 text-sm mt-0.5">{data.city} · Real-time protection active</p>
          </div>
          <div className="flex items-center gap-3">
            <RiskBadge level={data.riskLevel} />
            <button onClick={() => fetchDashboard(true)} disabled={refreshing}
              className="p-2 rounded-xl bg-gray-800 hover:bg-gray-700 transition-all">
              <RefreshCw size={14} className={`text-gray-400 ${refreshing ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </div>

        {/* AI Suggestion Banner */}
        {data.riskSuggestion && (
          <div className="flex items-start gap-3 bg-blue-900/20 border border-blue-800/50 rounded-2xl p-4">
            <Lightbulb size={18} className="text-blue-400 mt-0.5 shrink-0" />
            <p className="text-sm text-blue-300">{data.riskSuggestion}</p>
          </div>
        )}

        {/* Stats Grid */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard title="Wallet Balance" value={`₹${Number(data.walletBalance ?? 0).toFixed(2)}`}
            icon={Wallet} color="green" />
          <StatCard title="Active Plan" value={data.activePlan ?? 'None'}
            subtitle={data.policyStatus ?? 'No policy'} icon={Shield} color="blue" />
          <StatCard title="Total Claims" value={data.totalClaims ?? 0}
            subtitle={`${data.approvedClaims ?? 0} paid`} icon={FileText} color="purple" />
          <StatCard title="Total Payouts" value={`₹${Number(data.totalPayouts ?? 0).toFixed(2)}`}
            icon={TrendingUp} color="yellow" />
        </div>

        {/* Risk & Policy Row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {/* Live Risk Card */}
          <div className="card relative overflow-hidden">
            <div className="absolute top-0 right-0 w-32 h-32 bg-blue-600/5 rounded-full blur-2xl pointer-events-none" />
            <h3 className="font-semibold text-white mb-4 flex items-center gap-2">
              <span className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
              Live Risk Monitor — {data.city}
            </h3>
            <div className="space-y-3">
              <RiskRow icon={CloudRain} label="Rainfall" value={`${risk?.rainfall ?? 0} mm`}
                threshold={10} current={risk?.rainfall ?? 0} />
              <RiskRow icon={Thermometer} label="Temperature" value={`${risk?.temperature ?? 0}°C`}
                threshold={40} current={risk?.temperature ?? 0} />
              <RiskRow icon={Wind} label="AQI" value={risk?.aqi ?? 0}
                threshold={150} current={risk?.aqi ?? 0} />
            </div>
            <div className="mt-4 pt-4 border-t border-gray-800 flex items-center justify-between">
              <span className="text-sm text-gray-500">Overall Risk</span>
              <RiskBadge level={risk?.level ?? 'LOW'} />
            </div>
          </div>

          {/* Policy Summary */}
          <div className="card relative overflow-hidden">
            <div className="absolute bottom-0 right-0 w-32 h-32 bg-purple-600/5 rounded-full blur-2xl pointer-events-none" />
            <h3 className="font-semibold text-white mb-4">Policy Summary</h3>
            {data.activePlan ? (
              <div className="space-y-3">
                <InfoRow label="Plan" value={data.activePlan} />
                <InfoRow label="Status" value={data.policyStatus} />
                <InfoRow label="Weekly Premium" value={`₹${data.weeklyPremium}`} />
                <InfoRow label="Coverage Amount" value={`₹${data.coverageAmount}`} />
                <InfoRow label="Claims Paid" value={data.approvedClaims} />
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center h-32 text-center">
                <Shield size={32} className="text-gray-700 mb-2" />
                <p className="text-gray-500 text-sm">No active policy</p>
                <a href="/plans" className="text-blue-400 text-sm mt-1 hover:underline">Browse plans →</a>
              </div>
            )}
          </div>
        </div>

        {/* ML Insights Panel */}
        {mlData && (
          <div className="card relative overflow-hidden">
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-600 via-purple-600 to-cyan-600" />
            <div className="absolute top-0 right-0 w-48 h-48 bg-purple-600/5 rounded-full blur-3xl pointer-events-none" />
            <h3 className="font-semibold text-white mb-4 flex items-center gap-2">
              <span className="text-purple-400">AI</span> ML Model Insights
              <span className="text-xs bg-purple-900/40 text-purple-400 border border-purple-800/50 px-2 py-0.5 rounded-full ml-1">Random Forest</span>
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {/* Risk Probabilities */}
              <div className="bg-gray-800/50 rounded-xl p-4">
                <p className="text-xs text-gray-500 mb-3 uppercase tracking-wider">Risk Probabilities</p>
                {mlData.risk?.probabilities && Object.entries(mlData.risk.probabilities).map(([level, pct]) => (
                  <div key={level} className="mb-2">
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-gray-400">{level}</span>
                      <span className="text-white font-medium">{pct}%</span>
                    </div>
                    <div className="h-1.5 bg-gray-700 rounded-full overflow-hidden">
                      <div className={`h-full rounded-full ${
                        level === 'HIGH' ? 'bg-red-500' : level === 'MEDIUM' ? 'bg-yellow-500' : 'bg-green-500'
                      }`} style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                ))}
                <p className="text-xs text-gray-600 mt-2">Confidence: {mlData.risk?.confidence}%</p>
              </div>

              {/* Fraud Status */}
              <div className="bg-gray-800/50 rounded-xl p-4">
                <p className="text-xs text-gray-500 mb-3 uppercase tracking-wider">Fraud Shield</p>
                <div className={`text-center py-3 rounded-xl ${
                  mlData.fraud?.isFraud ? 'bg-red-900/30 border border-red-800/50' : 'bg-green-900/30 border border-green-800/50'
                }`}>
                  <p className={`text-2xl font-bold ${ mlData.fraud?.isFraud ? 'text-red-400' : 'text-green-400'}`}>
                    {mlData.fraud?.verdict}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">Fraud Score: {mlData.fraud?.fraudScore}%</p>
                </div>
                <p className="text-xs text-gray-600 mt-2">Isolation Forest model</p>
              </div>

              {/* Payout Estimate */}
              <div className="bg-gray-800/50 rounded-xl p-4">
                <p className="text-xs text-gray-500 mb-3 uppercase tracking-wider">ML Payout Estimate</p>
                <p className="text-2xl font-bold text-white">₹{mlData.payout?.recommendedPayout?.toFixed(2)}</p>
                <p className="text-xs text-gray-500 mt-1">{mlData.payout?.payoutPercentage}% of coverage</p>
                <div className="mt-3 h-1.5 bg-gray-700 rounded-full overflow-hidden">
                  <div className="h-full bg-blue-500 rounded-full"
                    style={{ width: `${Math.min(mlData.payout?.payoutPercentage ?? 0, 100)}%` }} />
                </div>
                <p className="text-xs text-gray-600 mt-2">Random Forest Regressor</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function RiskRow({ icon: Icon, label, value, threshold, current }) {
  const pct = Math.min((current / threshold) * 100, 100);
  const barColor = pct >= 100 ? 'bg-red-500' : pct >= 60 ? 'bg-yellow-500' : 'bg-green-500';
  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <div className="flex items-center gap-2 text-sm text-gray-400">
          <Icon size={14} /> {label}
        </div>
        <span className="text-sm font-medium text-white">{value}</span>
      </div>
      <div className="h-1.5 bg-gray-800 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-500 ${barColor}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

function InfoRow({ label, value }) {
  return (
    <div className="flex justify-between items-center py-1.5 border-b border-gray-800 last:border-0">
      <span className="text-sm text-gray-500">{label}</span>
      <span className="text-sm font-medium text-white">{value}</span>
    </div>
  );
}
