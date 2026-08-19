import { Link } from 'react-router-dom';
import { getProfile } from '../../api/profile';
import { linkBack } from '../../components/ui';
import { useResource } from '../../hooks/useResource';
import PersonalInfoForm from './PersonalInfoForm';
import CvSection from './CvSection';
import ExperienceSection from './ExperienceSection';
import FormationSection from './FormationSection';
import SkillSection from './SkillSection';
import DegreeSection from './DegreeSection';
import LanguageSection from './LanguageSection';

/** Page « Mon profil » de l'espace intérimaire (incrément 3a). */
export default function ProfilePage() {
  const {
    data: profile,
    setData: setProfile,
    loading,
    error,
    reload,
  } = useResource(getProfile, 'Impossible de charger le profil.');

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
        <Link to="/interimaire" className={linkBack}>
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

      <section className="rounded-xl border border-slate-200 bg-white p-6">
        <h2 className="text-lg font-semibold text-slate-900">Disponibilités</h2>
        <p className="mt-1 text-sm text-slate-600">
          Tes indisponibilités se déclarent depuis ton planning, à partir de J+8.
        </p>
        <Link
          to="/interimaire/planning"
          className="mt-3 inline-block text-sm font-medium text-indigo-600 hover:underline"
        >
          Ouvrir mon planning →
        </Link>
      </section>
    </div>
  );
}
