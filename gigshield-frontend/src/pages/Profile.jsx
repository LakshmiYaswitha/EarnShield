import { useEffect, useState } from 'react';
import { userAPI, transactionAPI } from '../api';
import { useAuth } from '../context/AuthContext';
import { Spinner } from '../components/UI';
import { User, MapPin, Phone, Mail, Wallet, ArrowUpCircle, ArrowDownCircle, MinusCircle } from 'lucide-react';
import toast from 'react-hot-toast';

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Hyderabad', 'Pune', 'Kolkata', 'Ahmedabad'];

export default function Profile() {
  const { refreshUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ name: '', phone: '', city: '' });
  const [transactions, setTransactions] = useState([]);
  const [topupAmount, setTopupAmount] = useState('');
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [saving, setSaving] = useState(false);
  const [topping, setTopping] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [loading, setLoading] = useState(true);

  const fetchProfile = () =>
    userAPI.getProfile().then((r) => {
      setProfile(r.data);
      setForm({ name: r.data.name, phone: r.data.phone, city: r.data.city });
    });

  useEffect(() => {
    Promise.all([fetchProfile(), transactionAPI.getAll().then((r) => setTransactions(r.data))])
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await userAPI.updateProfile(form);
      await fetchProfile();
      await refreshUser();
      toast.success('Profile updated');
    } catch {
      toast.error('Update failed');
    } finally {
      setSaving(false);
    }
  };

  const handleTopup = async () => {
    const amt = parseFloat(topupAmount);
    if (!amt || amt <= 0) { toast.error('Enter a valid amount'); return; }
    setTopping(true);
    try {
      await userAPI.topUpWallet(amt);
      await fetchProfile();
      await refreshUser();
      setTopupAmount('');
      toast.success(`₹${amt} added to wallet`);
      transactionAPI.getAll().then((r) => setTransactions(r.data));
    } catch {
      toast.error('Top-up failed');
    } finally {
      setTopping(false);
    }
  };

  const handleWithdraw = async () => {
    const amt = parseFloat(withdrawAmount);
    if (!amt || amt <= 0) { toast.error('Enter a valid amount'); return; }
    setWithdrawing(true);
    try {
      const res = await userAPI.withdraw(amt);
      await fetchProfile();
      await refreshUser();
      setWithdrawAmount('');
      toast.success(`₹${amt} withdrawn successfully`);
      transactionAPI.getAll().then((r) => setTransactions(r.data));
    } catch (err) {
      toast.error(err.response?.data?.error || 'Withdrawal failed');
    } finally {
      setWithdrawing(false);
    }
  };

  if (loading) return <Spinner />;

  return (
    <div className="relative min-h-screen">
      {/* Background */}
      <div className="fixed inset-0 -z-10 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 right-0 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-1/4 w-80 h-80 bg-blue-600/8 rounded-full blur-3xl" />
      </div>

      <div className="p-6 space-y-6">
        <h2 className="text-xl font-bold text-white">Profile</h2>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Edit Profile */}
          <div className="card">
            <h3 className="font-semibold text-white mb-4 flex items-center gap-2">
              <User size={16} className="text-blue-400" /> Personal Info
            </h3>
            <form onSubmit={handleSave} className="space-y-4">
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Full Name</label>
                <input className="input" value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Email</label>
                <input className="input opacity-50 cursor-not-allowed" value={profile?.email ?? ''} disabled />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Phone</label>
                <input className="input" value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })} />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">City</label>
                <select className="input" value={form.city}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}>
                  {CITIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <button type="submit" className="btn-primary w-full" disabled={saving}>
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </form>
          </div>

          {/* Wallet */}
          <div className="space-y-4">
            <div className="card">
              <h3 className="font-semibold text-white mb-4 flex items-center gap-2">
                <Wallet size={16} className="text-green-400" /> Wallet
              </h3>
              <div className="bg-gradient-to-br from-blue-900/40 to-purple-900/40 border border-blue-800/30 rounded-xl p-5 mb-4 relative overflow-hidden">
                <div className="absolute top-0 right-0 w-24 h-24 bg-blue-500/10 rounded-full blur-2xl" />
                <p className="text-sm text-gray-400 mb-1">Available Balance</p>
                <p className="text-3xl font-bold text-white">₹{Number(profile?.walletBalance ?? 0).toFixed(2)}</p>
                <p className="text-xs text-gray-500 mt-1">Updated in real-time</p>
              </div>

              {/* Top Up */}
              <p className="text-xs text-gray-500 mb-1.5 uppercase tracking-wider">Add Money</p>
              <div className="flex gap-2 mb-4">
                <input className="input flex-1" type="number" placeholder="Enter amount"
                  value={topupAmount} onChange={(e) => setTopupAmount(e.target.value)} min="1" />
                <button onClick={handleTopup} disabled={topping}
                  className="bg-green-600 hover:bg-green-700 text-white font-semibold px-4 rounded-xl transition-all disabled:opacity-50 flex items-center gap-1.5">
                  <ArrowDownCircle size={14} />
                  {topping ? '...' : 'Top Up'}
                </button>
              </div>

              {/* Withdraw */}
              <p className="text-xs text-gray-500 mb-1.5 uppercase tracking-wider">Withdraw Money</p>
              <div className="flex gap-2">
                <input className="input flex-1" type="number" placeholder="Enter amount"
                  value={withdrawAmount} onChange={(e) => setWithdrawAmount(e.target.value)} min="1"
                  max={profile?.walletBalance ?? 0} />
                <button onClick={handleWithdraw} disabled={withdrawing}
                  className="bg-red-600 hover:bg-red-700 text-white font-semibold px-4 rounded-xl transition-all disabled:opacity-50 flex items-center gap-1.5">
                  <MinusCircle size={14} />
                  {withdrawing ? '...' : 'Withdraw'}
                </button>
              </div>
              <p className="text-xs text-gray-600 mt-2">Max: ₹{Number(profile?.walletBalance ?? 0).toFixed(2)}</p>
            </div>

            <div className="card">
              <h3 className="font-semibold text-white mb-3">Account Info</h3>
              <div className="space-y-2">
                <InfoRow icon={Mail} label="Email" value={profile?.email} />
                <InfoRow icon={MapPin} label="City" value={profile?.city} />
                <InfoRow icon={Phone} label="Phone" value={profile?.phone} />
                <InfoRow icon={User} label="Role" value={profile?.role} />
                <InfoRow icon={User} label="Persona" value={profile?.persona?.replace(/_/g, ' ')} />
                <InfoRow icon={Wallet} label="Weekly Earnings" value={profile?.weeklyEarnings ? `₹${profile.weeklyEarnings}` : '—'} />
              </div>
            </div>
          </div>
        </div>

        {/* Transaction History */}
        <div className="card">
          <h3 className="font-semibold text-white mb-4">Transaction History</h3>
          {transactions.length === 0 ? (
            <p className="text-gray-500 text-sm text-center py-6">No transactions yet</p>
          ) : (
            <div className="space-y-2">
              {transactions.slice(0, 10).map((tx) => (
                <div key={tx.id} className="flex items-center gap-3 py-2.5 border-b border-gray-800 last:border-0">
                  <div className={`p-1.5 rounded-lg ${
                    tx.type === 'PAYOUT_CREDIT' ? 'bg-green-900/30' :
                    tx.type === 'PAYMENT_CREDIT' ? 'bg-blue-900/30' :
                    tx.type === 'REFUND' ? 'bg-orange-900/30' : 'bg-red-900/30'
                  }`}>
                    {tx.type === 'PAYOUT_CREDIT'
                      ? <ArrowDownCircle size={16} className="text-green-400" />
                      : tx.type === 'PAYMENT_CREDIT'
                      ? <ArrowDownCircle size={16} className="text-blue-400" />
                      : tx.type === 'REFUND'
                      ? <MinusCircle size={16} className="text-orange-400" />
                      : <ArrowUpCircle size={16} className="text-red-400" />}
                  </div>
                  <div className="flex-1">
                    <p className="text-sm text-gray-300">{tx.description}</p>
                    <p className="text-xs text-gray-600">{new Date(tx.createdAt).toLocaleString()}</p>
                  </div>
                  <span className={`font-semibold text-sm ${
                    tx.type === 'PAYOUT_CREDIT' ? 'text-green-400' :
                    tx.type === 'PAYMENT_CREDIT' ? 'text-blue-400' :
                    tx.type === 'REFUND' ? 'text-orange-400' : 'text-red-400'
                  }`}>
                    {tx.type === 'PAYOUT_CREDIT' || tx.type === 'PAYMENT_CREDIT' ? '+' : '-'}₹{tx.amount}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function InfoRow({ icon: Icon, label, value }) {
  return (
    <div className="flex items-center gap-3 py-1.5">
      <Icon size={14} className="text-gray-500 shrink-0" />
      <span className="text-sm text-gray-500 w-16">{label}</span>
      <span className="text-sm text-gray-300">{value}</span>
    </div>
  );
}
