import { useEffect, useState } from 'react';
import { claimAPI } from '../api';
import { Spinner, EmptyState } from '../components/UI';
import { CloudRain, Thermometer, Wind, User, CheckCircle, Clock, XCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const TRIGGER_ICONS = {
  RAIN: CloudRain,
  HEAT: Thermometer,
  AQI: Wind,
  MANUAL: User,
};

const STATUS_STYLES = {
  PAID: 'text-green-400 bg-green-900/30',
  APPROVED: 'text-blue-400 bg-blue-900/30',
  PENDING: 'text-yellow-400 bg-yellow-900/30',
  REJECTED: 'text-red-400 bg-red-900/30',
};

const STATUS_ICONS = {
  PAID: CheckCircle,
  APPROVED: CheckCircle,
  PENDING: Clock,
  REJECTED: XCircle,
};

export default function Claims() {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);

  const fetchClaims = () =>
    claimAPI.getAll().then((r) => setClaims(r.data)).finally(() => setLoading(false));

  useEffect(() => { fetchClaims(); }, []);

  const handleManualClaim = async () => {
    setTriggering(true);
    try {
      const res = await claimAPI.trigger('Manual claim request');
      toast.success(`Claim approved! ₹${res.data.amount} credited`);
      fetchClaims();
    } catch (err) {
      toast.error(err.response?.data?.error || 'Claim failed');
    } finally {
      setTriggering(false);
    }
  };

  const totalPaid = claims.filter((c) => c.status === 'PAID').reduce((s, c) => s + c.amount, 0);

  if (loading) return <Spinner />;

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-white">Claims</h2>
          <p className="text-gray-500 text-sm mt-0.5">
            {claims.length} total · ₹{totalPaid.toFixed(2)} paid out
          </p>
        </div>
        <button onClick={handleManualClaim} disabled={triggering} className="btn-primary text-sm">
          {triggering ? 'Processing...' : '+ Manual Claim'}
        </button>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Total', value: claims.length, color: 'text-white' },
          { label: 'Paid', value: claims.filter((c) => c.status === 'PAID').length, color: 'text-green-400' },
          { label: 'Pending', value: claims.filter((c) => c.status === 'PENDING').length, color: 'text-yellow-400' },
        ].map(({ label, value, color }) => (
          <div key={label} className="card text-center">
            <p className={`text-2xl font-bold ${color}`}>{value}</p>
            <p className="text-xs text-gray-500 mt-1">{label}</p>
          </div>
        ))}
      </div>

      {/* Claims List */}
      {claims.length === 0 ? (
        <EmptyState message="No claims yet. Claims are auto-triggered when risk thresholds are exceeded." />
      ) : (
        <div className="space-y-3">
          {claims.map((claim) => {
            const Icon = TRIGGER_ICONS[claim.trigger] ?? User;
            const StatusIcon = STATUS_ICONS[claim.status] ?? Clock;
            return (
              <div key={claim.id} className="card flex items-center gap-4">
                <div className="w-10 h-10 bg-gray-800 rounded-xl flex items-center justify-center shrink-0">
                  <Icon size={18} className="text-gray-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-white text-sm">{claim.triggerReason}</p>
                  <p className="text-xs text-gray-500 mt-0.5">
                    {claim.trigger} · {new Date(claim.claimDate).toLocaleString()}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className="font-bold text-white">₹{claim.amount?.toFixed(2)}</p>
                  <span className={`inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full mt-1 ${STATUS_STYLES[claim.status]}`}>
                    <StatusIcon size={10} />
                    {claim.status}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
