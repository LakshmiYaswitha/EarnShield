import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { policyAPI } from '../api';
import { Spinner } from '../components/UI';
import { Shield, Calendar, CreditCard, CheckCircle, XCircle } from 'lucide-react';

export default function Policy() {
  const [policy, setPolicy] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    policyAPI.getActive()
      .then((r) => setPolicy(r.data?.plan ? r.data : null))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Spinner />;

  if (!policy) {
    return (
      <div className="p-6">
        <div className="card flex flex-col items-center justify-center py-16 text-center">
          <Shield size={48} className="text-gray-700 mb-4" />
          <h3 className="text-lg font-semibold text-white mb-2">No Active Policy</h3>
          <p className="text-gray-500 text-sm mb-6">You are not currently protected. Select a plan to get started.</p>
          <button onClick={() => navigate('/plans')} className="btn-primary">Browse Plans</button>
        </div>
      </div>
    );
  }

  const statusColor = policy.status === 'ACTIVE' ? 'text-green-400' : 'text-red-400';
  const StatusIcon = policy.status === 'ACTIVE' ? CheckCircle : XCircle;

  return (
    <div className="p-6 space-y-6">
      <h2 className="text-xl font-bold text-white">My Policy</h2>

      <div className="card border-2 border-blue-800/50">
        <div className="flex items-start justify-between mb-6">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-blue-900/50 rounded-2xl flex items-center justify-center">
              <Shield size={24} className="text-blue-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold text-white">{policy.plan} Plan</h3>
              <div className={`flex items-center gap-1.5 text-sm ${statusColor}`}>
                <StatusIcon size={14} />
                {policy.status}
              </div>
            </div>
          </div>
          <div className="text-right">
            <p className="text-2xl font-bold text-white">₹{policy.coverageAmount}</p>
            <p className="text-xs text-gray-500">Max coverage</p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <InfoCard icon={CreditCard} label="Weekly Premium" value={`₹${policy.weeklyPremium}`} />
          <InfoCard icon={Calendar} label="Start Date" value={policy.startDate} />
          <InfoCard icon={Calendar} label="Expiry Date" value={policy.expiryDate} />
          <InfoCard icon={Shield} label="Plan Type" value={policy.plan} />
        </div>

        <div className="mt-6 pt-4 border-t border-gray-800">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-gray-500">Policy validity</span>
            <span className="text-sm text-gray-400">
              {policy.startDate} → {policy.expiryDate}
            </span>
          </div>
          <div className="h-2 bg-gray-800 rounded-full overflow-hidden">
            <div className="h-full bg-blue-600 rounded-full" style={{ width: '40%' }} />
          </div>
        </div>
      </div>

      <div className="card">
        <h3 className="font-semibold text-white mb-3">What's Covered</h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          {['Heavy Rainfall', 'Extreme Heat', 'Poor Air Quality'].map((item) => (
            <div key={item} className="flex items-center gap-2 bg-green-900/20 border border-green-800/30 rounded-xl p-3">
              <CheckCircle size={16} className="text-green-400" />
              <span className="text-sm text-green-300">{item}</span>
            </div>
          ))}
        </div>
      </div>

      <button onClick={() => navigate('/plans')} className="btn-secondary">
        Change Plan
      </button>
    </div>
  );
}

function InfoCard({ icon: Icon, label, value }) {
  return (
    <div className="bg-gray-800/50 rounded-xl p-4">
      <div className="flex items-center gap-2 text-gray-500 text-xs mb-1">
        <Icon size={12} /> {label}
      </div>
      <p className="font-semibold text-white">{value}</p>
    </div>
  );
}
