import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getProfile } from '../../api/profile';
import type { Profile } from '../../profile/types';
import PersonalInfoForm from './PersonalInfoForm';
import CvSection from './CvSection';
import ExperienceSection from './ExperienceSection';
import FormationSection from './FormationSection';
import SkillSection from './SkillSection';
import DegreeSection from './DegreeSection';
import LanguageSection from './LanguageSection';

/** Page « Mon profil » de l'espace intérimaire (incrément 3a). */
export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setError(null);
    try {
      setProfile(await getProfile());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Impossible de charger le profil.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  if (loading) {
    return <p className="text-slate-500">Chargement du profil…</p>;
  }

  if (error && !profile) {
    return <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>;
  }

  if (!profile) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/interimaire" className="text-sm text-indigo-600 hover:underline">
          ← Retour au tableau de bord
        </Link>
        <h1 className="mt-2 text-2xl font-semibold text-slate-900">Mon profil</h1>
        <p className="mt-1 text-slate-600">{profile.email}</p>
      </div>

      <PersonalInfoForm profile={profile} onSaved={setProfile} />
      <CvSection cvFilePath={profile.cvFilePath} onChanged={reload} />
      <ExperienceSection experiences={profile.experiences} onChanged={reload} />
      <FormationSection formations={profile.formations} onChanged={reload} />
      <SkillSection />
      <DegreeSection />
      <LanguageSection />
    </div>
  );
}
