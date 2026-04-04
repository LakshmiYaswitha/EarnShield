import { useEffect, useState } from 'react';
import { adminAPI, fraudAPI } from '../api';
import { Spinner, RiskBadge } from '../components/UI';
import { Users, FileText, AlertTriangle, TrendingUp, Shield, CheckCircle, XCircle } from 'lucide-react';

const SEVERITY_STYLES = {
  LOW:      'text-blue-400 bg-blue-900/30 border-blue-800/50',
  MEDIUM:   'text-yellow-400 bg-yellow-900/30 border-yellow-800/50',
  HIGH:     'text-orange-400 bg-orange-900/30 border-orange-800/50',
  CRITICAL: 'text-red-400 bg-red-900/30 border-red-800/50',
};

const ALERT_LABELS = {
  GPS_SPOOFING:          '📍 GPS Spoofing',
  ABNORMAL_SPEED:        '🚀 Abnormal Speed',
  LOCATION_JUMP:         '⚡ Location Jump',
  NETWORK_GPS_MISMATCH:  '🔀 Network/GPS Mismatch',
  GROUP_FRAUD:           '👥 Group Fraud',
  DUPLICATE_CLAIM:       '🔁 Duplicate Claim',
  SUSPICIOUS_TIMING:     '⏱ Suspicious Timing',
  BEHAVIOR_ANOMALY:      '🤖 Behavior Anomaly',
};

export default function AdminDashboard() {
  const [users, setUsers]         = useState([]);
  const [claims, setClaims]       = useState([]);
  const [stats, setStats]         = useState(null);
  const [fraudAlerts, setFraudAlerts] = useState([]);
  const [fraudStats, setFraudStats]   = useState(null);
  const [tab, setTab]             = useState('users');
  const [loading, setLoading]     = useState(true);

  useEffect(() => {
    Promise.all([
      adminAPI.getUsers(),
      adminAPI.getClaims(),
      adminAPI.getStats(),
      fraudAPI.allAlerts().catch(() => ({ data: [] })),
      fraudAPI.stats().catch(() => ({ data: {} })),
    ]).then(([u, c, s, fa, fs]) => {
      setUsers(u.data);
      setClaims(c.data);
      setStats(s.data);
      setFraudAlerts(fa.data);
      setFraudStats(fs.data);
    }).finally(() => setLoading(false));
  }, []);

  const handleResolve = async (id) => {
    await fraudAPI.resolveAlert(id);
    setFraudAlerts((prev) => prev.map((a) => a.id === id ? { ...a, resolved: true } : a));
  };

  if (loading) return <Spinner />;

  const unresolvedAlerts = fraudAlerts.filter((a) => !a.resolved);

  return (
    <div className="relative min-h-screen">
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute top-0 right-0 w-96 h-96 bg-red-600/5 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-purple-600/8 rounded-full blur-3xl" />
      </div>

      <div className="p-6 space-y-6">
        <h2 className="text-xl font-bold text-white">Admin Dashboard</h2>

        {/* Stats */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard icon={Users}         label="Total Users"    value={stats?.totalUsers ?? 0}    color="blue" />
          <StatCard icon={AlertTriangle} label="High Risk"      value={stats?.highRiskUsers ?? 0} color="red" />
          <StatCard icon={FileText}      label="Total Claims"   value={stats?.totalClaims ?? 0}   color="purple" />
          <StatCard icon={Shield}        label="Fraud Alerts"   value={unresolvedAlerts.length}   color="yellow" />
        </div>

        {/* Fraud Stats Row */}
        {fraudStats && (
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
            {[
              { label: 'Total Alerts',  value: fraudStats.totalAlerts,  color: 'text-white' },
              { label: 'Unresolved',    value: fraudStats.unresolved,   color: 'text-yellow-400' },
              { label: 'Critical',      value: fraudStats.critical,     color: 'text-red-400' },
              { label: 'GPS Spoofing',  value: fraudStats.gpsSpoofing,  color: 'text-orange-400' },
              { label: 'Group Fraud',   value: fraudStats.groupFraud,   color: 'text-purple-400' },
            ].map(({ label, value, color }) => (
              <div key={label} className="card text-center py-3">
                <p className={`text-xl font-bold ${color}`}>{value ?? 0}</p>
                <p className="text-xs text-gray-500 mt-0.5">{label}</p>
              </div>
            ))}
          </div>
        )}

        {/* Tabs */}
        <div className="flex gap-2 border-b border-gray-800">
          {['users', 'claims', 'fraud alerts'].map((t) => (
            <button key={t} onClick={() => setTab(t)}
              className={`px-4 py-2 text-sm font-medium capitalize transition-all border-b-2 -mb-px ${
                tab === t
                  ? 'border-blue-500 text-blue-400'
                  : 'border-transparent text-gray-500 hover:text-gray-300'
              }`}>
              {t}
              {t === 'fraud alerts' && unresolvedAlerts.length > 0 && (
                <span className="ml-1.5 bg-red-600 text-white text-xs px-1.5 py-0.5 rounded-full">
                  {unresolvedAlerts.length}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Users Table */}
        {tab === 'users' && (
          <div className="card overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800">
                  {['ID', 'Name', 'Email', 'City', 'Risk', 'Wallet'].map((h) => (
                    <th key={h} className="text-left text-xs text-gray-500 font-medium px-4 py-3">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id} className="border-b border-gray-800/50 hover:bg-gray-800/30 transition-colors">
                    <td className="px-4 py-3 text-gray-500">#{u.id}</td>
                    <td className="px-4 py-3 font-medium text-white">{u.name}</td>
                    <td className="px-4 py-3 text-gray-400">{u.email}</td>
                    <td className="px-4 py-3 text-gray-400">{u.city}</td>
                    <td className="px-4 py-3"><RiskBadge level={u.riskLevel} /></td>
                    <td className="px-4 py-3 text-green-400">₹{u.walletBalance?.toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {users.length === 0 && <p className="text-center text-gray-500 py-8">No users found</p>}
          </div>
        )}

        {/* Claims Table */}
        {tab === 'claims' && (
          <div className="card overflow-x-auto p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-800">
                  {['ID', 'User', 'Trigger', 'Amount', 'Status', 'Date', 'Fraud'].map((h) => (
                    <th key={h} className="text-left text-xs text-gray-500 font-medium px-4 py-3">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {claims.map((c) => (
                  <tr key={c.id} className={`border-b border-gray-800/50 hover:bg-gray-800/30 transition-colors ${c.fraudFlag ? 'bg-red-900/10' : ''}`}>
                    <td className="px-4 py-3 text-gray-500">#{c.id}</td>
                    <td className="px-4 py-3 text-white">User #{c.userId ?? c.user?.id}</td>
                    <td className="px-4 py-3 text-gray-400">{c.trigger}</td>
                    <td className="px-4 py-3 text-green-400">₹{c.amount?.toFixed(2)}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded-full border ${
                        c.status === 'PAID'    ? 'bg-green-900/30 text-green-400 border-green-800/50' :
                        c.status === 'PENDING' ? 'bg-yellow-900/30 text-yellow-400 border-yellow-800/50' :
                        'bg-red-900/30 text-red-400 border-red-800/50'
                      }`}>{c.status}</span>
                    </td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{new Date(c.claimDate).toLocaleDateString()}</td>
                    <td className="px-4 py-3">
                      {c.fraudFlag
                        ? <span className="text-xs text-red-400 bg-red-900/30 border border-red-800/50 px-2 py-0.5 rounded-full">Flagged</span>
                        : <span className="text-xs text-green-400">Clean</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {claims.length === 0 && <p className="text-center text-gray-500 py-8">No claims found</p>}
          </div>
        )}

        {/* Fraud Alerts Tab */}
        {tab === 'fraud alerts' && (
          <div className="space-y-3">
            {fraudAlerts.length === 0 ? (
              <div className="card text-center py-12">
                <Shield size={40} className="text-green-600 mx-auto mb-3" />
                <p className="text-green-400 font-medium">No fraud alerts</p>
                <p className="text-gray-500 text-sm mt-1">All systems clean</p>
              </div>
            ) : (
              fraudAlerts.map((alert) => (
                <div key={alert.id} className={`card border ${alert.resolved ? 'opacity-50' : ''}`}>
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-start gap-3 flex-1">
                      <span className={`text-xs px-2 py-1 rounded-full border font-semibold shrink-0 ${SEVERITY_STYLES[alert.severity]}`}>
                        {alert.severity}
                      </span>
                      <div>
                        <p className="text-sm font-medium text-white">
                          {ALERT_LABELS[alert.alertType] ?? alert.alertType}
                        </p>
                        <p className="text-xs text-gray-400 mt-0.5">{alert.description}</p>
                        <p className="text-xs text-gray-600 mt-1">
                          User: {alert.userName} (#{alert.userId}) · {new Date(alert.createdAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                    <div className="shrink-0">
                      {alert.resolved ? (
                        <span className="flex items-center gap-1 text-xs text-green-400">
                          <CheckCircle size={12} /> Resolved
                        </span>
                      ) : (
                        <button onClick={() => handleResolve(alert.id)}
                          className="text-xs bg-gray-800 hover:bg-gray-700 text-gray-300 px-3 py-1.5 rounded-lg transition-all flex items-center gap-1">
                          <XCircle size={12} /> Resolve
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color }) {
  const colors = {
    blue:   'text-blue-400 bg-blue-900/30',
    red:    'text-red-400 bg-red-900/30',
    purple: 'text-purple-400 bg-purple-900/30',
    yellow: 'text-yellow-400 bg-yellow-900/30',
  };
  return (
    <div className="card flex items-center gap-4">
      <div className={`p-3 rounded-xl ${colors[color]}`}>
        <Icon size={20} className={colors[color].split(' ')[0]} />
      </div>
      <div>
        <p className="text-2xl font-bold text-white">{value}</p>
        <p className="text-xs text-gray-500">{label}</p>
      </div>
    </div>
  );
}
