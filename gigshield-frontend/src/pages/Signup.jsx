import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authAPI } from '../api';
import { Shield, Bike, ShoppingCart, Package } from 'lucide-react';
import toast from 'react-hot-toast';

const CITIES = ['Mumbai', 'Delhi', 'Bangalore', 'Chennai', 'Hyderabad', 'Pune', 'Kolkata', 'Ahmedabad'];

const PERSONAS = [
  { id: 'FOOD_DELIVERY',     label: 'Food Delivery',     icon: Bike,         desc: 'Swiggy, Zomato, etc.' },
  { id: 'GROCERY_DELIVERY',  label: 'Grocery Delivery',  icon: ShoppingCart, desc: 'Blinkit, Zepto, etc.' },
  { id: 'ECOMMERCE_DELIVERY',label: 'E-commerce',        icon: Package,      desc: 'Amazon, Flipkart, etc.' },
];

export default function Signup() {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState({ name: '', email: '', phone: '', password: '', city: '', persona: '', weeklyEarnings: '' });
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const handleNext = (e) => {
    e.preventDefault();
    if (!/^[0-9]{10}$/.test(form.phone)) { toast.error('Phone must be 10 digits'); return; }
    setStep(2);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.persona) { toast.error('Please select your work type'); return; }
    if (!form.weeklyEarnings || form.weeklyEarnings <= 0) { toast.error('Enter your weekly earnings'); return; }
    setLoading(true);
    try {
      const res = await authAPI.signup({ ...form, weeklyEarnings: parseFloat(form.weeklyEarnings) });
      login(res.data.token, res.data);
      toast.success(`Welcome to GigShield, ${res.data.name}!`);
      navigate('/dashboard');
    } catch (err) {
      toast.error(err.response?.data?.error || 'Signup failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      <div className="absolute inset-0 -z-10">
        <div className="absolute top-0 left-0 w-full h-full bg-gray-950" />
        <div className="absolute top-0 right-1/4 w-96 h-96 bg-purple-600/15 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-1/4 w-80 h-80 bg-blue-600/15 rounded-full blur-3xl" />
        <div className="absolute inset-0 opacity-5"
          style={{ backgroundImage: 'linear-gradient(#3b82f6 1px, transparent 1px), linear-gradient(90deg, #3b82f6 1px, transparent 1px)', backgroundSize: '50px 50px' }} />
      </div>

      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-blue-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-blue-600/30">
            <Shield size={32} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white">Create Account</h1>
          <p className="text-gray-500 mt-1">Start protecting your income today</p>
          {/* Step indicator */}
          <div className="flex items-center justify-center gap-2 mt-4">
            <div className={`w-8 h-1.5 rounded-full transition-all ${step >= 1 ? 'bg-blue-500' : 'bg-gray-700'}`} />
            <div className={`w-8 h-1.5 rounded-full transition-all ${step >= 2 ? 'bg-blue-500' : 'bg-gray-700'}`} />
          </div>
        </div>

        <div className="card border border-gray-700/50 shadow-2xl shadow-black/50">
          {step === 1 ? (
            <form onSubmit={handleNext} className="space-y-4">
              <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Step 1 — Personal Info</p>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Full Name</label>
                <input className="input" placeholder="Ravi Kumar" value={form.name} onChange={set('name')} required />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Email</label>
                <input className="input" type="email" placeholder="ravi@example.com" value={form.email} onChange={set('email')} required />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Phone (10 digits)</label>
                <input className="input" placeholder="9876543210" value={form.phone} onChange={set('phone')} required />
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">City</label>
                <select className="input" value={form.city} onChange={set('city')} required>
                  <option value="">Select your city</option>
                  {CITIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Password</label>
                <input className="input" type="password" placeholder="Min 6 characters" value={form.password} onChange={set('password')} required minLength={6} />
              </div>
              <button type="submit" className="btn-primary w-full mt-2">Next →</button>
            </form>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Step 2 — Work Profile</p>
              <div>
                <label className="block text-sm text-gray-400 mb-2">What type of gig work do you do?</label>
                <div className="space-y-2">
                  {PERSONAS.map(({ id, label, icon: Icon, desc }) => (
                    <button type="button" key={id}
                      onClick={() => setForm({ ...form, persona: id })}
                      className={`w-full flex items-center gap-3 p-3 rounded-xl border transition-all ${
                        form.persona === id
                          ? 'border-blue-500 bg-blue-500/10 text-blue-400'
                          : 'border-gray-700 text-gray-400 hover:border-gray-500'
                      }`}>
                      <Icon size={20} />
                      <div className="text-left">
                        <p className="font-medium text-sm">{label}</p>
                        <p className="text-xs text-gray-500">{desc}</p>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm text-gray-400 mb-1.5">Average Weekly Earnings (₹)</label>
                <input className="input" type="number" placeholder="e.g. 5000" value={form.weeklyEarnings}
                  onChange={set('weeklyEarnings')} required min="1" />
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setStep(1)}
                  className="flex-1 py-2.5 rounded-xl border border-gray-700 text-gray-400 hover:border-gray-500 transition-all text-sm">
                  ← Back
                </button>
                <button type="submit" className="btn-primary flex-1" disabled={loading}>
                  {loading ? 'Creating...' : 'Create Account'}
                </button>
              </div>
            </form>
          )}
          <p className="text-center text-sm text-gray-500 mt-4">
            Already have an account?{' '}
            <Link to="/login" className="text-blue-400 hover:text-blue-300 font-medium">Sign in</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
