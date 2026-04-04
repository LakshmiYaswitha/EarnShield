import { useEffect, useState } from 'react';
import { claimAPI } from '../api';
import { Spinner, EmptyState } from '../components/UI';
import { CloudRain, Thermometer, Wind, User, CheckCircle, Clock, XCircle, Waves, AlertOctagon } from 'lucide-react';

const TRIGGER_ICONS = {
  RAIN:   CloudRain,
  HEAT:   Thermometer,
  AQI:    Wind,
  FLOOD:  Waves,
  CURFEW: AlertOctagon,
  MANUAL: User,
};

const TRIGGER_LABELS = {
  RAIN:   'Heavy Rainfall',
  HEAT:   'Extreme Heat',
  AQI:    'Poor Air Quality',
  FLOOD:  'Flood Alert',
  CURFEW: 'Curfew / Strike',
  MANUAL: 'Manual',
};

const STATUS_STYLES = {
  PAID:     'text-green-400 bg-green-900/30',
  APPROVED: 'text-blue-400 bg-blue-900/30',
  PENDING:  'text-yellow-400 bg-yellow-900/30',
  REJECTED: 'text-red-400 bg-red-900/30',
};

const STATUS_ICONS = {
  PAID:     CheckCircle,
  APPROVED: CheckCircle,
  PENDING:  Clock,
  REJECTED: XCircle,
};

export default function Claims() {
  const [claims, setClaims] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    claimAPI.getAll().then((r) => setClaims(r.data)).finally(() => setLoading(false));
  }, []);

  const totalPaid = claims.filter((c) => c.status === 'PAID').reduce((s, c) => s + c.amount, 0);

  if (loading) return <Spinner />;

  return (
    <div className="p-6 space-y-6">
      <div>
        <h2 className="text-xl font-bold text-white">Claims</h2>
        <p className="text-gray-500 text-sm mt-0.5">
          {claims.length} total · ₹{totalPaid.toFixed(2)} paid out
        </p>
      </div>

      {/* Info banner */}
      <div className="flex items-start gap-3 bg-blue-900/20 border border-blue-800/50 rounded-2xl p-4">
        <CheckCircle size={16} className="text-blue-400 mt-0.5 shrink-0" />
        <p className="text-sm text-blue-300">
          Claims are <span className="font-semibold">automatically triggered</span> by our AI system when weather or environmental thresholds are exceeded. No manual filing needed.
        </p>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-3 gap-4">
        {[
          { label: 'Total',   value: claims.length,                                          color: 'text-white' },
          { label: 'Paid',    value: claims.filter((c) => c.status === 'PAID').length,        color: 'text-green-400' },
          { label: 'Pending', value: claims.filter((c) => c.status === 'PENDING').length,     color: 'text-yellow-400' },
        ].map(({ label, value, color }) => (
          <div key={label} className="card text-center">
            <p className={`text-2xl font-bold ${color}`}>{value}</p>
            <p className="text-xs text-gray-500 mt-1">{label}</p>
          </div>
        ))}
      </div>

      {/* Trigger legend */}
      <div className="grid grid-cols-3 sm:grid-cols-5 gap-2">
        {Object.entries(TRIGGER_LABELS).filter(([k]) => k !== 'MANUAL').map(([key, label]) => {
          const Icon = TRIGGER_ICONS[key];
          return (
            <div key={key} className="flex items-center gap-1.5 bg-gray-800/50 rounded-xl px-3 py-2">
              <Icon size={13} className="text-gray-400 shrink-0" />
              <span className="text-xs text-gray-400">{label}</span>
            </div>
          );
        })}
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
                    {TRIGGER_LABELS[claim.trigger] ?? claim.trigger} · {new Date(claim.claimDate).toLocaleString()}
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
