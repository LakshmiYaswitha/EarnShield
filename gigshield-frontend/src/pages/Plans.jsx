import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { policyAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import { Spinner } from '../components/UI';
import { Shield, Check, Lock, CreditCard, X, Smartphone, Building2 } from 'lucide-react';
import toast from 'react-hot-toast';

const PLAN_COLORS = {
  BASIC: 'border-gray-700 hover:border-gray-500',
  STANDARD: 'border-blue-700 hover:border-blue-500',
  PREMIUM: 'border-purple-700 hover:border-purple-500',
};
const PLAN_BADGE = {
  BASIC: 'text-gray-400',
  STANDARD: 'text-blue-400',
  PREMIUM: 'text-purple-400',
};

const PAYMENT_METHODS = [
  { id: 'Card', label: 'Credit / Debit Card', icon: CreditCard },
  { id: 'UPI', label: 'UPI', icon: Smartphone },
  { id: 'NetBanking', label: 'Net Banking', icon: Building2 },
];

function PaymentModal({ plan, onClose, onSuccess }) {
  const [method, setMethod] = useState('Card');
  const [fields, setFields] = useState({ cardNumber: '', expiry: '', cvv: '', upiId: '', bank: '' });
  const [paying, setPaying] = useState(false);

  const handlePay = async () => {
    if (method === 'Card') {
      if (fields.cardNumber.replace(/\s/g, '').length < 16) return toast.error('Enter a valid 16-digit card number');
      if (!fields.expiry) return toast.error('Enter expiry date');
      if (fields.cvv.length < 3) return toast.error('Enter valid CVV');
    } else if (method === 'UPI') {
      if (!fields.upiId.includes('@')) return toast.error('Enter a valid UPI ID (e.g. name@upi)');
    } else {
      if (!fields.bank) return toast.error('Select a bank');
    }

    setPaying(true);
    try {
      await policyAPI.payAndActivate(plan.name, method);
      toast.success(`Payment of ₹${plan.weeklyPremium} successful! ${plan.name} plan activated.`);
      onSuccess();
    } catch (err) {
      toast.error(err.response?.data?.error || err.response?.data?.message || 'Payment failed');
    } finally {
      setPaying(false);
    }
  };

  const formatCard = (val) =>
    val.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
      <div className="bg-gray-900 border border-gray-700 rounded-2xl w-full max-w-md p-6 space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-white font-bold text-lg">Complete Payment</h3>
            <p className="text-gray-400 text-sm">{plan.name} Plan — ₹{plan.weeklyPremium}/week</p>
          </div>
          <button onClick={onClose} className="text-gray-500 hover:text-white transition-colors">
            <X size={20} />
          </button>
        </div>

        <div className="bg-gray-800 rounded-xl p-4 flex justify-between items-center">
          <span className="text-gray-400 text-sm">Amount to pay</span>
          <span className="text-white font-bold text-xl">₹{plan.weeklyPremium}</span>
        </div>

        <div className="flex gap-2">
          {PAYMENT_METHODS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setMethod(id)}
              className={`flex-1 flex flex-col items-center gap-1 py-2.5 rounded-xl text-xs font-medium border transition-all ${
                method === id
                  ? 'border-blue-500 bg-blue-500/10 text-blue-400'
                  : 'border-gray-700 text-gray-500 hover:border-gray-500'
              }`}>
              <Icon size={16} />
              {label}
            </button>
          ))}
        </div>

        {method === 'Card' && (
          <div className="space-y-3">
            <input
              className="input w-full"
              placeholder="Card Number"
              value={fields.cardNumber}
              maxLength={19}
              onChange={(e) => setFields({ ...fields, cardNumber: formatCard(e.target.value) })}
            />
            <div className="flex gap-3">
              <input
                className="input w-full"
                placeholder="MM/YY"
                maxLength={5}
                value={fields.expiry}
                onChange={(e) => {
                  let v = e.target.value.replace(/\D/g, '').slice(0, 4);
                  if (v.length > 2) v = v.slice(0, 2) + '/' + v.slice(2);
                  setFields({ ...fields, expiry: v });
                }}
              />
              <input
                className="input w-full"
                placeholder="CVV"
                maxLength={4}
                type="password"
                value={fields.cvv}
                onChange={(e) => setFields({ ...fields, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) })}
              />
            </div>
          </div>
        )}

        {method === 'UPI' && (
          <input
            className="input w-full"
            placeholder="Enter UPI ID (e.g. name@okaxis)"
            value={fields.upiId}
            onChange={(e) => setFields({ ...fields, upiId: e.target.value })}
          />
        )}

        {method === 'NetBanking' && (
          <select
            className="input w-full"
            value={fields.bank}
            onChange={(e) => setFields({ ...fields, bank: e.target.value })}>
            <option value="">Select Bank</option>
            {['SBI', 'HDFC', 'ICICI', 'Axis', 'Kotak', 'PNB', 'BOB', 'Canara'].map((b) => (
              <option key={b} value={b}>{b} Bank</option>
            ))}
          </select>
        )}

        <button
          onClick={handlePay}
          disabled={paying}
          className="w-full py-3 rounded-xl font-bold text-white bg-green-600 hover:bg-green-700 transition-all disabled:opacity-60">
          {paying ? 'Processing...' : `Pay ₹${plan.weeklyPremium}`}
        </button>

        <p className="text-center text-xs text-gray-600">🔒 Simulated secure payment — no real charges</p>
      </div>
    </div>
  );
}

export default function Plans() {
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    policyAPI.getPlans()
      .then((r) => setPlans(r.data))
      .finally(() => setLoading(false));
  }, []);

  const handleSuccess = async () => {
    await refreshUser();
    navigate('/policy');
  };

  if (loading) return <Spinner />;

  return (
    <div className="relative min-h-screen">
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-1/3 left-0 w-80 h-80 bg-purple-600/10 rounded-full blur-3xl" />
      </div>

      {selectedPlan && (
        <PaymentModal
          plan={selectedPlan}
          onClose={() => setSelectedPlan(null)}
          onSuccess={handleSuccess}
        />
      )}

      <div className="p-6 space-y-6">
        <div>
          <h2 className="text-xl font-bold text-white">Insurance Plans</h2>
          <p className="text-gray-500 text-sm mt-1">
            {user?.riskLevel === 'HIGH'
              ? '⚠️ High risk detected — only Premium plan available'
              : 'Choose the plan that fits your needs'}
          </p>
          {/* Dynamic pricing notice */}
          {user?.riskLevel && user.riskLevel !== 'LOW' && (
            <div className="mt-3 flex items-center gap-2 bg-yellow-900/20 border border-yellow-800/40 rounded-xl px-4 py-2.5">
              <span className="text-yellow-400 text-xs font-medium">
                ⚡ Dynamic pricing applied — your premium is adjusted based on your {user.riskLevel} risk level
                {user.persona ? ` and ${user.persona.replace('_', ' ').toLowerCase()} profile` : ''}
              </span>
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {plans.map((plan) => (
            <div key={plan.name}
              className={`card border-2 transition-all relative ${PLAN_COLORS[plan.name]} ${
                !plan.available ? 'opacity-50' : ''
              } ${plan.name === 'PREMIUM' ? 'ring-1 ring-purple-700/50' : ''}`}>

              {plan.name === 'PREMIUM' && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-purple-600 text-white text-xs font-bold px-3 py-1 rounded-full">
                  RECOMMENDED
                </div>
              )}

              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 rounded-xl bg-gray-800">
                  <Shield size={20} className={PLAN_BADGE[plan.name]} />
                </div>
                <div>
                  <h3 className={`font-bold text-lg ${PLAN_BADGE[plan.name]}`}>{plan.name}</h3>
                  <p className="text-xs text-gray-500">Weekly plan</p>
                </div>
              </div>

              <div className="mb-4">
                <span className="text-3xl font-bold text-white">₹{plan.weeklyPremium}</span>
                <span className="text-gray-500 text-sm">/week</span>
              </div>

              <div className="mb-2">
                <p className="text-xs text-gray-500 mb-1">Coverage up to</p>
                <p className="text-xl font-semibold text-white">₹{plan.coverage}</p>
              </div>

              <ul className="space-y-2 my-5">
                {plan.features.map((f) => (
                  <li key={f} className="flex items-center gap-2 text-sm text-gray-300">
                    <Check size={14} className="text-green-400 shrink-0" />
                    {f}
                  </li>
                ))}
              </ul>

              <button
                onClick={() => plan.available && setSelectedPlan(plan)}
                disabled={!plan.available}
                className={`w-full py-2.5 rounded-xl font-semibold text-sm transition-all ${
                  !plan.available
                    ? 'bg-gray-800 text-gray-600 cursor-not-allowed'
                    : plan.name === 'PREMIUM'
                    ? 'bg-purple-600 hover:bg-purple-700 text-white'
                    : 'btn-primary'
                }`}>
                {!plan.available
                  ? <span className="flex items-center justify-center gap-1"><Lock size={12} /> Locked</span>
                  : `Pay ₹${plan.weeklyPremium} & Activate`}
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
